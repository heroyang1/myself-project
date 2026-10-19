package com.backstage.system.service.order.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.backstage.system.domain.order.Coupon;
import com.backstage.system.mapper.order.ColumnPriceMapper;
import com.backstage.system.mapper.order.OshBookMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.backstage.system.mapper.order.OshOrderSaveMapper;
import com.backstage.system.domain.order.OshOrderSave;
import com.backstage.system.domain.order.OrderCreateRequest;
import com.backstage.system.service.order.IOshOrderSaveService;
import com.backstage.system.service.order.ICouponListService;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.DateUtils;
import com.backstage.common.utils.uuid.UUID;
import com.backstage.system.mapper.order.CourseMapper;

/**
 * 创建订单Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-07
 */
@Service
public class OshOrderSaveServiceImpl implements IOshOrderSaveService 
{
    @Autowired
    private OshOrderSaveMapper oshOrderSaveMapper;

    @Autowired
    private ICouponListService cardService;

    @Autowired(required = false)
    private CourseMapper courseMapper;

    @Autowired(required = false)
    private ColumnPriceMapper columnPriceMapper;

    @Autowired(required = false)
    private OshBookMapper oshBookMapper;

    /**
     * 查询创建订单
     * 
     * @param id 创建订单主键
     * @return 创建订单
     */
    @Override
    public OshOrderSave selectOshOrderSaveById(Long id)
    {
        return oshOrderSaveMapper.selectOshOrderSaveById(id);
    }

    @Override
    public OshOrderSave selectOshOrderSaveByNo(String no)
    {
        return oshOrderSaveMapper.selectOshOrderSaveByNo(no);
    }

    /**
     * 查询创建订单列表
     * 
     * @param oshOrderSave 创建订单
     * @return 创建订单
     */
    @Override
    public List<OshOrderSave> selectOshOrderSaveList(OshOrderSave oshOrderSave)
    {
        return oshOrderSaveMapper.selectOshOrderSaveList(oshOrderSave);
    }

    /**
     * 新增创建订单
     * 
     * @param oshOrderSave 创建订单
     * @return 结果
     */
    @Override
    public int insertOshOrderSave(OshOrderSave oshOrderSave)
    {
        return oshOrderSaveMapper.insertOshOrderSave(oshOrderSave);
    }

    /**
     * 修改创建订单
     * 
     * @param oshOrderSave 创建订单
     * @return 结果
     */
    @Override
    public int updateOshOrderSave(OshOrderSave oshOrderSave)
    {
        return oshOrderSaveMapper.updateOshOrderSave(oshOrderSave);
    }

    /**
     * 创建订单
     * 
     * @param request 订单创建请求参数
     * @return 订单信息
     */
    @Override
    public OshOrderSave createOrder(OrderCreateRequest request) {
        // 1. 验证参数
        if (request.getGoodsId() == null || request.getGoodsId() <= 0) {
            throw new ServiceException("商品 ID 不能为空");
        }
        if (request.getType() == null || request.getType().isEmpty()) {
            throw new ServiceException("订单类型不能为空");
        }
        if (request.getUserCouponId() == null || request.getUserCouponId() <= 0) {
            throw new ServiceException("优惠券 ID 不能为空");
        }



        // 2. 查询优惠券信息
        Coupon coupon = cardService.selectCardById(request.getUserCouponId());
        if (coupon == null) {
            throw new ServiceException("优惠券不存在");
        }
        if (coupon.getUsed() != 0) {
            throw new ServiceException("优惠券已使用");
        }
        if (!coupon.getType().equals(request.getType())) {
            throw new ServiceException("优惠券类型不匹配");
        }
        if (coupon.getGoodsId() != null && !coupon.getGoodsId().equals(request.getGoodsId())) {
            throw new ServiceException("优惠券不适用于该商品");
        }

        // 3. 验证优惠券有效期
        Date now = new Date();
        if (coupon.getStartTime() != null && now.before(coupon.getStartTime())) {
            throw new ServiceException("优惠券未到使用日期");
        }
        if (coupon.getEndTime() != null && now.after(coupon.getEndTime())) {
            throw new ServiceException("优惠券已过期");
        }

        // 4. 查询商品价格（根据 type 查询不同的表）
        BigDecimal originalPrice = queryGoodsPrice(request.getGoodsId(), request.getType());
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("商品价格无效");
        }

        // 5. 计算优惠后的价格
        BigDecimal couponPrice = coupon.getPrice();
        if (couponPrice == null) {
            couponPrice = BigDecimal.ZERO;
        }
        BigDecimal totalPrice = originalPrice.subtract(couponPrice);
        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            totalPrice = BigDecimal.ZERO; // 优惠后价格不能为负数
        }

        // 6. 生成订单号
        String orderNo = DateUtils.datePath() + "_" + UUID.randomUUID().toString().substring(0, 9);

        // 7. 创建订单
        OshOrderSave order = new OshOrderSave();
        order.setSchoolId(11L); // 默认学校 ID，实际应该从用户信息获取
        order.setUserId(252L); // 默认用户 ID，实际应该从登录信息获取
        order.setNo(orderNo);
        order.setStatus("pendding");
        order.setPrice(totalPrice);
        order.setTotalPrice(originalPrice.setScale(2, BigDecimal.ROUND_HALF_UP).toString()); // 优惠后总价，保留 2 位小数
        order.setType(request.getType());
        order.setCreatedTime(now);
        order.setUpdatedTime(now);

        // 8. 保存订单
        oshOrderSaveMapper.insertOshOrderSave(order);

        // 9. 更新优惠券状态为已使用
        Coupon updateCoupon = new Coupon();
        updateCoupon.setId(request.getUserCouponId());
        updateCoupon.setUsed(1);
        cardService.updateCard(updateCoupon);

        return order;
    }

    /**
     * 根据类型查询商品价格
     * @param goodsId 商品 ID
     * @param type 商品类型（course/column/book）
     * @return 商品价格
     */
    private BigDecimal queryGoodsPrice(Long goodsId, String type) {
        try {
            if ("course".equals(type)) {
                // 查询课程价格
                if (courseMapper != null) {
                    return courseMapper.selectPriceById(goodsId);
                }
            } else if ("column".equals(type)) {
                // 查询专栏价格
                if (columnPriceMapper != null) {
                    return columnPriceMapper.selectPriceById(goodsId);
                }
            } else if ("book".equals(type)) {
                // 查询电子书价格
                if (oshBookMapper != null) {
                    return oshBookMapper.selectPriceById(goodsId);
                }
            }
        } catch (Exception e) {
            throw new ServiceException("查询商品价格失败：" + e.getMessage());
        }
        throw new ServiceException("不支持的商品类型：" + type);
    }
}
