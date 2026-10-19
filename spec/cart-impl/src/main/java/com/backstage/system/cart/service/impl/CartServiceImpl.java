package com.backstage.system.cart.service.impl;

import com.backstage.system.cart.dto.CartAddDTO;
import com.backstage.system.cart.exception.CartException;
import com.backstage.system.cart.port.CartRepository;
import com.backstage.system.cart.port.DownstreamUnavailableException;
import com.backstage.system.cart.port.IdempotencyStore;
import com.backstage.system.cart.port.ProductQueryService;
import com.backstage.system.cart.port.StockQueryService;
import com.backstage.system.cart.service.CartService;
import com.backstage.system.cart.vo.CartItemVO;

import java.util.logging.Logger;

/**
 * 加购实现。严格对应 spec/cart-add.md 的 R1~R5 与 E1~E8，顺序即契约：
 * 鉴权 → 幂等 token → 数量校验 → 幂等短路 → 商品存在 → 在售 → 库存校验（先于写）→ 原子累加。
 * 本类只依赖端口接口，不 import 任何框架，故可用纯 JDK 单测直接驱动。
 */
public class CartServiceImpl implements CartService {

    /** R2：单个 SKU 上限。 */
    static final int SKU_QUANTITY_CAP = 99;

    private static final Logger LOG = Logger.getLogger(CartServiceImpl.class.getName());

    private final CartRepository cartRepository;
    private final StockQueryService stockQueryService;
    private final ProductQueryService productQueryService;
    private final IdempotencyStore idempotencyStore;

    public CartServiceImpl(CartRepository cartRepository,
                           StockQueryService stockQueryService,
                           ProductQueryService productQueryService,
                           IdempotencyStore idempotencyStore) {
        this.cartRepository = cartRepository;
        this.stockQueryService = stockQueryService;
        this.productQueryService = productQueryService;
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public CartItemVO addItem(Long userId, CartAddDTO dto) {
        // E7：未登录不产生任何写入
        if (userId == null) {
            throw new CartException(CartException.Code.UNAUTHORIZED);
        }
        if (dto == null) {
            throw new CartException(CartException.Code.INVALID_QUANTITY);
        }
        // E8：幂等 token 缺失即拒绝（幂等是接口契约的一部分，不默认宽容）
        String token = dto.getIdempotencyToken();
        if (token == null || token.trim().isEmpty()) {
            throw new CartException(CartException.Code.IDEMPOTENCY_TOKEN_REQUIRED);
        }
        // E2：数量非法（<=0 或非数字在绑定层已被 @Validated 拦下，这里防御性再校验）
        Integer quantity = dto.getQuantity();
        if (quantity == null || quantity <= 0) {
            throw new CartException(CartException.Code.INVALID_QUANTITY);
        }
        final Long skuId = dto.getSkuId();
        final int qty = quantity;
        // R5：同 token 并发/重试只执行一次，第二次直接返回首次结果
        return idempotencyStore.executeOnce(token, () -> doAdd(userId, skuId, qty));
    }

    private CartItemVO doAdd(Long userId, Long skuId, int qty) {
        // E1：SKU 不存在
        if (skuId == null || !productQueryService.exists(skuId)) {
            throw new CartException(CartException.Code.SKU_NOT_FOUND);
        }
        // R4：下架商品不可加
        if (!productQueryService.isOnSale(skuId)) {
            throw new CartException(CartException.Code.SKU_OFF_SALE);
        }
        // R3：库存校验必须先于 cart 写入
        //  E3=库存不足（确定拒绝，严线）；E4=库存查询失败（未知，放行 + WARN，下单环节强制复查，宽线）
        boolean stockKnown = true;
        int available = 0;
        try {
            available = stockQueryService.getAvailable(skuId);
        } catch (DownstreamUnavailableException e) {
            stockKnown = false;
            LOG.warning("库存查询失败，放行加购待下单复查 fail-open skuId=" + skuId
                    + " reason=" + e.getMessage());
        }
        if (stockKnown && available < qty) {
            throw new CartException(CartException.Code.OUT_OF_STOCK);
        }
        // R1/R2/E6：单条原子 upsert 累加并截断，禁「读-改-写」；唯一索引防双行
        CartRepository.Result result =
                cartRepository.addQuantityAtomically(userId, skuId, qty, SKU_QUANTITY_CAP);
        return new CartItemVO(skuId, result.getQuantity(), result.isCapped());
    }
}
