package com.backstage.system.controller.order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.backstage.common.annotation.Anonymous;
import com.backstage.system.domain.order.OshOrderSave;
import com.backstage.system.service.order.IOshOrderSaveService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.backstage.common.annotation.Log;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.AjaxResult;
import com.backstage.common.enums.BusinessType;
import com.backstage.system.domain.order.WxPay;
import com.backstage.system.service.order.IWxPayService;
import com.backstage.common.utils.poi.ExcelUtil;
import com.backstage.common.core.page.TableDataInfo;

/**
 * 微信支付Controller
 * 
 * @author ruoyi
 * @date 2026-03-08
 */
@RestController
@RequestMapping("/pc/order/wxpay")
public class WxPayController extends BaseController
{
    @Autowired
    private IWxPayService wxPayService;

    @Autowired
    private IOshOrderSaveService orderSaveService;

    /**
     * 查询微信支付列表
     */
//    @PreAuthorize("@ss.hasPermi('system:wxpay:list')")
    @Anonymous
    @GetMapping("/list")
    public TableDataInfo list(WxPay wxPay)
    {
        startPage();
        List<WxPay> list = wxPayService.selectWxPayList(wxPay);
        return getDataTable(list);
    }

    /**
     * 导出微信支付列表
     */
//    @PreAuthorize("@ss.hasPermi('system:wxpay:export')")
    @Anonymous
    @Log(title = "微信支付", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WxPay wxPay)
    {
        List<WxPay> list = wxPayService.selectWxPayList(wxPay);
        ExcelUtil<WxPay> util = new ExcelUtil<WxPay>(WxPay.class);
        util.exportExcel(response, list, "微信支付数据");
    }

    /**
     * 获取微信支付详细信息
     */
//    @PreAuthorize("@ss.hasPermi('system:wxpay:query')")
    @Anonymous
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(1);
    }

    /**
     * 新增微信支付
     */
//    @PreAuthorize("@ss.hasPermi('system:wxpay:add')")
//    @Log(title = "微信支付", businessType = BusinessType.INSERT)
    @Anonymous
    @PostMapping
    public Map<String, Object> add(@RequestBody WxPay wxPay)
    {

        OshOrderSave orderSave = orderSaveService.selectOshOrderSaveByNo(wxPay.getNo());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        if (wxPayService.selectWxPayByNo(wxPay.getNo())!= null)
            return data;
        if (orderSave!= null){
            wxPay.setNo(orderSave.getNo());
            wxPay.setPrice(orderSave.getPrice().toString());
            wxPay.setCodeUrl("weixin:asddasd");
            int rows = wxPayService.insertWxPay(wxPay);

            // 修改返回格式
            if (rows > 0) {
                Map<String, Object> dataInner = new LinkedHashMap<String, Object>();
                data.put("msg", "ok");
                dataInner.put("price", wxPay.getPrice());
                dataInner.put("code_url", wxPay.getCodeUrl());
                data.put("data", dataInner);
                data.put("code", 20000);
            }

            return data;
        }
        return toAjax(0);
    }

    /**
     * 修改微信支付
     */
    @PreAuthorize("@ss.hasPermi('system:wxpay:edit')")
    @Log(title = "微信支付", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WxPay wxPay)
    {
        return toAjax(wxPayService.updateWxPay(wxPay));
    }

}
