package com.backstage.system.mapper.order;

import java.util.List;
import com.backstage.system.domain.order.WxPay;

/**
 * 微信支付Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-08
 */
public interface WxPayMapper 
{
    /**
     * 查询微信支付
     * 
     * @param no 微信支付主键
     * @return 微信支付
     */
    public WxPay selectWxPayByNo(String no);

    /**
     * 查询微信支付列表
     * 
     * @param wxPay 微信支付
     * @return 微信支付集合
     */
    public List<WxPay> selectWxPayList(WxPay wxPay);

    /**
     * 新增微信支付
     * 
     * @param wxPay 微信支付
     * @return 结果
     */
    public int insertWxPay(WxPay wxPay);

    /**
     * 修改微信支付
     * 
     * @param wxPay 微信支付
     * @return 结果
     */
    public int updateWxPay(WxPay wxPay);

}
