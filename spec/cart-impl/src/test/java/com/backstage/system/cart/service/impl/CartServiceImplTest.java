package com.backstage.system.cart.service.impl;

import com.backstage.system.cart.dto.CartAddDTO;
import com.backstage.system.cart.exception.CartException;
import com.backstage.system.cart.port.CartRepository;
import com.backstage.system.cart.port.DownstreamUnavailableException;
import com.backstage.system.cart.port.IdempotencyStore;
import com.backstage.system.cart.port.ProductQueryService;
import com.backstage.system.cart.port.StockQueryService;
import com.backstage.system.cart.vo.CartItemVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 加购单测：逐条覆盖 spec/cart-add.md 的 R1~R5 与异常路径 E1~E8。
 * 用纯 JDK 内存假件驱动，无 Spring/MyBatis/DB —— 可在本地直接编译运行。
 */
class CartServiceImplTest {

    // ---------- 假件 ----------

    /** 记录端口调用顺序，用于断言 R3「库存校验先于写入」。 */
    static final class Recorder {
        final List<String> events = new CopyOnWriteArrayList<>();
    }

    static final class FakeProduct implements ProductQueryService {
        final Set<Long> existing = Collections.synchronizedSet(new LinkedHashSet<>());
        final Set<Long> onSale = Collections.synchronizedSet(new LinkedHashSet<>());
        final Recorder rec;
        FakeProduct(Recorder rec) { this.rec = rec; }
        @Override public boolean exists(Long skuId) { rec.events.add("product.exists"); return existing.contains(skuId); }
        @Override public boolean isOnSale(Long skuId) { rec.events.add("product.isOnSale"); return onSale.contains(skuId); }
    }

    static final class FakeStock implements StockQueryService {
        final Map<Long, Integer> available = new ConcurrentHashMap<>();
        final Set<Long> down = Collections.synchronizedSet(new LinkedHashSet<>());
        final Recorder rec;
        FakeStock(Recorder rec) { this.rec = rec; }
        @Override public int getAvailable(Long skuId) {
            rec.events.add("stock.getAvailable");
            if (down.contains(skuId)) { throw new DownstreamUnavailableException("stock service timeout"); }
            return available.getOrDefault(skuId, 0);
        }
    }

    /** 内存假件：以 (user,sku) 为唯一键，synchronized 保证累加原子（模拟唯一索引 + 原子 upsert）。 */
    static final class FakeRepo implements CartRepository {
        final Map<String, int[]> rows = new ConcurrentHashMap<>();
        final Recorder rec;
        FakeRepo(Recorder rec) { this.rec = rec; }
        private String key(Long u, Long s) { return u + ":" + s; }
        @Override public synchronized Result addQuantityAtomically(Long userId, Long skuId, int delta, int cap) {
            rec.events.add("repo.addQuantityAtomically");
            int[] row = rows.computeIfAbsent(key(userId, skuId), k -> new int[]{0});
            int sum = row[0] + delta;
            int q = Math.min(sum, cap);
            boolean capped = sum > cap;
            row[0] = q;
            return new Result(q, capped);
        }
        int quantity(Long u, Long s) { int[] r = rows.get(key(u, s)); return r == null ? 0 : r[0]; }
        int rowCount(Long u, Long s) { return rows.containsKey(key(u, s)) ? 1 : 0; }
    }

    /** 幂等假件：同 token 只执行一次（ConcurrentHashMap.computeIfAbsent 原子性）。 */
    static final class FakeIdem implements IdempotencyStore {
        final Map<String, Object> cache = new ConcurrentHashMap<>();
        @SuppressWarnings("unchecked")
        @Override public <T> T executeOnce(String token, Supplier<T> action) {
            return (T) cache.computeIfAbsent(token, k -> action.get());
        }
    }

    // ---------- 装配 ----------

    private FakeProduct product;
    private FakeStock stock;
    private FakeRepo repo;
    private Recorder rec;

    private CartServiceImpl newService() {
        rec = new Recorder();
        product = new FakeProduct(rec);
        stock = new FakeStock(rec);
        repo = new FakeRepo(rec);
        return new CartServiceImpl(repo, stock, product, new FakeIdem());
    }

    private CartAddDTO dto(Long sku, int qty, String token) {
        CartAddDTO d = new CartAddDTO();
        d.setSkuId(sku);
        d.setQuantity(qty);
        d.setIdempotencyToken(token);
        return d;
    }

    private void onSale(Long sku, int availableQty) {
        product.existing.add(sku);
        product.onSale.add(sku);
        stock.available.put(sku, availableQty);
    }

    private CartException.Code codeOf(Runnable r) {
        CartException e = assertThrows(CartException.class, r::run);
        return e.getCode();
    }

    // ---------- R1~R5 ----------

    @Test
    @DisplayName("R1 同一 SKU 累加数量：加 A×2 再加 A×3 → 恰 1 行且 quantity=5")
    void r1_mergeSameSku() {
        CartServiceImpl svc = newService();
        onSale(100L, 1000);
        svc.addItem(1L, dto(100L, 2, "t1"));
        CartItemVO vo = svc.addItem(1L, dto(100L, 3, "t2"));
        assertEquals(5, vo.getQuantity());
        assertEquals(5, repo.quantity(1L, 100L));
        assertEquals(1, repo.rowCount(1L, 100L), "同用户同 SKU 必须合并为一行");
    }

    @Test
    @DisplayName("R2 单 SKU 上限 99：97 再加 5 → 截断为 99（非 102、非 2 行）且 capped=true")
    void r2_capAt99() {
        CartServiceImpl svc = newService();
        onSale(100L, 1000);
        svc.addItem(1L, dto(100L, 97, "t1"));
        CartItemVO vo = svc.addItem(1L, dto(100L, 5, "t2"));
        assertEquals(99, vo.getQuantity());
        assertTrue(vo.isCapped(), "触顶须返回 capped=true");
        assertEquals(1, repo.rowCount(1L, 100L));
    }

    @Test
    @DisplayName("R3 加购前校验库存：库存查询必须先于 cart 写入（按调用顺序断言）")
    void r3_stockCheckBeforeWrite() {
        CartServiceImpl svc = newService();
        onSale(100L, 50);
        svc.addItem(1L, dto(100L, 5, "t1"));
        int stockIdx = rec.events.indexOf("stock.getAvailable");
        int writeIdx = rec.events.indexOf("repo.addQuantityAtomically");
        assertTrue(stockIdx >= 0 && writeIdx >= 0, "两步都应发生");
        assertTrue(stockIdx < writeIdx, "库存校验必须先于写入");
    }

    @Test
    @DisplayName("R4 下架商品不可加：返回下架错误码，cart 行数不变")
    void r4_offSaleRejected() {
        CartServiceImpl svc = newService();
        product.existing.add(100L); // 存在但不在售
        stock.available.put(100L, 50);
        assertEquals(CartException.Code.SKU_OFF_SALE,
                codeOf(() -> svc.addItem(1L, dto(100L, 1, "t1"))));
        assertEquals(0, repo.rowCount(1L, 100L));
        assertFalse(rec.events.contains("repo.addQuantityAtomically"), "拒绝时不得写入");
    }

    @Test
    @DisplayName("R5 重复提交幂等：同 token 并发提交 6 次×3 → 终态 quantity=3 而非 18")
    void r5_idempotentByToken() throws Exception {
        CartServiceImpl svc = newService();
        onSale(100L, 1000);
        int n = 6;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                try { start.await(); svc.addItem(1L, dto(100L, 3, "SAME_TOKEN")); }
                catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(3, repo.quantity(1L, 100L), "同 token 只应生效一次");
        assertEquals(1, repo.rowCount(1L, 100L));
    }

    // ---------- E1~E8 异常路径 ----------

    @Test
    @DisplayName("E1 SKU 不存在：参数类错误码，cart 零变化")
    void e1_skuNotFound() {
        CartServiceImpl svc = newService();
        assertEquals(CartException.Code.SKU_NOT_FOUND,
                codeOf(() -> svc.addItem(1L, dto(999L, 1, "t1"))));
        assertEquals(0, repo.rowCount(1L, 999L));
    }

    @Test
    @DisplayName("E2 数量非法（<=0）：拒绝且不触碰任何下游")
    void e2_invalidQuantity() {
        CartServiceImpl svc = newService();
        onSale(100L, 50);
        assertEquals(CartException.Code.INVALID_QUANTITY,
                codeOf(() -> svc.addItem(1L, dto(100L, 0, "t1"))));
        assertEquals(CartException.Code.INVALID_QUANTITY,
                codeOf(() -> svc.addItem(1L, dto(100L, -3, "t2"))));
        assertTrue(rec.events.isEmpty(), "校验失败须在任何端口调用之前");
    }

    @Test
    @DisplayName("E3 库存不足：拒绝 + 缺货码，cart 零变化（严线）")
    void e3_outOfStock() {
        CartServiceImpl svc = newService();
        onSale(100L, 0);
        assertEquals(CartException.Code.OUT_OF_STOCK,
                codeOf(() -> svc.addItem(1L, dto(100L, 1, "t1"))));
        assertEquals(0, repo.rowCount(1L, 100L));
    }

    @Test
    @DisplayName("E4 库存查询失败：放行加购 + 记 WARN（宽线，下单强制复查）")
    void e4_stockDownFailOpen() {
        CartServiceImpl svc = newService();
        product.existing.add(100L);
        product.onSale.add(100L);
        stock.down.add(100L); // getAvailable 抛 DownstreamUnavailable
        CartItemVO vo = svc.addItem(1L, dto(100L, 2, "t1"));
        assertEquals(2, vo.getQuantity(), "库存未知时不得误拒，须放行");
        assertTrue(rec.events.contains("repo.addQuantityAtomically"), "放行应完成写入");
    }

    @Test
    @DisplayName("E5 加购后才下架（竞态）：既有行保留不静默删")
    void e5_offSaleAfterAddKeepsRow() {
        CartServiceImpl svc = newService();
        onSale(100L, 100);
        svc.addItem(1L, dto(100L, 4, "t1")); // 先成功加购
        product.onSale.remove(100L);         // 之后商品下架
        onSale(200L, 100);                    // 用户再加另一个 SKU
        svc.addItem(1L, dto(200L, 1, "t2"));
        assertEquals(4, repo.quantity(1L, 100L), "已下架 SKU 的购物车行不得被静默删除");
    }

    @Test
    @DisplayName("E6 多端并发加购同 SKU（不同 token）：原子累加不丢更新、只一行")
    void e6_concurrentDistinctTokensAtomic() throws Exception {
        CartServiceImpl svc = newService();
        onSale(100L, 10000);
        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < n; i++) {
            final int idx = i;
            pool.submit(() -> {
                try { start.await(); svc.addItem(1L, dto(100L, 1, "tok_" + idx)); }
                catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(50, repo.quantity(1L, 100L), "无丢更新：50 次 +1 应得 50");
        assertEquals(1, repo.rowCount(1L, 100L), "唯一索引防双行");
    }

    @Test
    @DisplayName("E7 未登录调用：401，不产生任何写入")
    void e7_unauthorized() {
        CartServiceImpl svc = newService();
        onSale(100L, 50);
        assertEquals(CartException.Code.UNAUTHORIZED,
                codeOf(() -> svc.addItem(null, dto(100L, 1, "t1"))));
        assertTrue(rec.events.isEmpty());
    }

    @Test
    @DisplayName("E8 幂等 token 缺失：拒绝并提示，不默认宽容")
    void e8_tokenMissing() {
        CartServiceImpl svc = newService();
        onSale(100L, 50);
        assertEquals(CartException.Code.IDEMPOTENCY_TOKEN_REQUIRED,
                codeOf(() -> svc.addItem(1L, dto(100L, 1, null))));
        assertEquals(CartException.Code.IDEMPOTENCY_TOKEN_REQUIRED,
                codeOf(() -> svc.addItem(1L, dto(100L, 1, "   "))));
        assertTrue(rec.events.isEmpty());
    }
}
