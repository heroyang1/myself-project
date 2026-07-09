package com.backstage.system.service.order.impl;

import java.util.List;

import com.backstage.system.service.order.IOshOrderSaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.backstage.system.mapper.order.WxPayMapper;
import com.backstage.system.domain.order.WxPay;
import com.backstage.system.service.order.IWxPayService;

/**
 * 微信支付Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-08
 */
@Service
public class WxPayServiceImpl implements IWxPayService 
{
    @Autowired
    private WxPayMapper wxPayMapper;

    @Autowired
    private IOshOrderSaveService orderService;


    /**
     * 查询微信支付
     * 
     * @param no 微信支付主键
     * @return 微信支付
     */
    @Override
    public WxPay selectWxPayByNo(String no)
    {
        return wxPayMapper.selectWxPayByNo(no);
    }

    /**
     * 查询微信支付列表
     * 
     * @param wxPay 微信支付
     * @return 微信支付
     */
    @Override
    public List<WxPay> selectWxPayList(WxPay wxPay)
    {
        return wxPayMapper.selectWxPayList(wxPay);
    }

    /**
     * 新增微信支付
     * 
     * @param wxPay 微信支付
     * @return 结果
     */
    @Override
    public int insertWxPay(WxPay wxPay)
    {
        if (orderService.selectOshOrderSaveByNo(wxPay.getNo()) != null){
            return wxPayMapper.insertWxPay(wxPay);
        }
        else
            return 0;
    }

    /**
     * 修改微信支付
     * 
     * @param wxPay 微信支付
     * @return 结果
     */
    @Override
    public int updateWxPay(WxPay wxPay)
    {
        return wxPayMapper.updateWxPay(wxPay);
    }

}
