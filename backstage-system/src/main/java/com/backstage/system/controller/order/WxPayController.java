package com.backstage.system.controller.order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.order.OshOrderSave;
import com.backstage.system.domain.vo.order.PayResponse;
import com.backstage.system.service.order.IOshOrderSaveService;
import com.backstage.system.service.order.PayService;
import com.backstage.system.utils.IpUtils;
import org.apache.commons.lang3.StringUtils;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 历史扫码支付Controller。
 *
 * <p>接口地址保留 /pc/order/wxpay，兼容旧前端；实际统一发起支付宝扫码订单。</p>
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

    @Autowired
    private PayService payService;

    /**
     * 查询历史扫码支付列表
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
     * 导出历史扫码支付列表
     */
//    @PreAuthorize("@ss.hasPermi('system:wxpay:export')")
    @Anonymous
    @Log(title = "历史扫码支付", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WxPay wxPay)
    {
        List<WxPay> list = wxPayService.selectWxPayList(wxPay);
        ExcelUtil<WxPay> util = new ExcelUtil<WxPay>(WxPay.class);
        util.exportExcel(response, list, "历史扫码支付数据");
    }

    /**
     * 获取历史扫码支付详细信息
     */
//    @PreAuthorize("@ss.hasPermi('system:wxpay:query')")
    @Anonymous
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(1);
    }

    /**
     * 新增历史扫码支付
     */
//    @PreAuthorize("@ss.hasPermi('system:wxpay:add')")
//    @Log(title = "微信支付", businessType = BusinessType.INSERT)
    @Anonymous
    @PostMapping
    public Map<String, Object> add(@RequestBody WxPay wxPay, HttpServletRequest request)
    {
        if (wxPay == null || StringUtils.isBlank(wxPay.getNo())) {
            throw new ServiceException("订单号不能为空");
        }
        OshOrderSave orderSave = orderSaveService.selectOshOrderSaveByNo(wxPay.getNo());
        if (orderSave == null) {
            throw new ServiceException("订单不存在");
        }
        if ("success".equals(orderSave.getStatus())) {
            throw new ServiceException("订单已支付");
        }

        WxPay cached = wxPayService.selectWxPayByNo(wxPay.getNo());
        if (cached != null && StringUtils.isNotBlank(cached.getCodeUrl())) {
            return payResult(cached.getPrice(), cached.getCodeUrl(), legacyPaymentNo(wxPay.getNo()), "复用支付宝二维码");
        }

        String paymentNo = legacyPaymentNo(orderSave.getNo());
        PayResponse response = payService.createPay(
                paymentNo,
                "OSH商品",
                orderSave.getPrice().toPlainString(),
                IpUtils.getClientIp(request),
                "alipay"
        );

        if (response == null || response.getCode() != 1
                || StringUtils.isBlank(StringUtils.defaultIfBlank(response.getQrcode(), response.getPayUrl()))) {
            throw new ServiceException(response == null || StringUtils.isBlank(response.getMsg())
                    ? "支付宝订单创建失败"
                    : response.getMsg());
        }

        wxPay.setNo(orderSave.getNo());
        wxPay.setPrice(orderSave.getPrice().toPlainString());
        wxPay.setCodeUrl(StringUtils.defaultIfBlank(response.getQrcode(), response.getPayUrl()));
        wxPayService.insertWxPay(wxPay);

        return payResult(wxPay.getPrice(), wxPay.getCodeUrl(), paymentNo, "支付宝订单创建成功");
    }

    private Map<String, Object> payResult(String price, String qrcode, String paymentNo, String msg) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        Map<String, Object> dataInner = new LinkedHashMap<String, Object>();
        dataInner.put("price", price);
        dataInner.put("qrcode", qrcode);
        dataInner.put("code_url", qrcode);
        dataInner.put("pay_type", "alipay");
        dataInner.put("paymentNo", paymentNo);
        data.put("msg", msg);
        data.put("data", dataInner);
        data.put("code", 200);
        return data;
    }

    public static String legacyPaymentNo(String orderNo) {
        String digits = orderNo == null ? "" : orderNo.replaceAll("\\D", "");
        String date = digits.length() >= 8
                ? digits.substring(0, 8)
                : LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = orderNo == null ? "" : orderNo.replaceAll("[^A-Za-z0-9]", "");
        if (suffix.length() > 40) {
            suffix = suffix.substring(0, 40);
        }
        return "P" + date + suffix;
    }

    /**
     * 修改历史扫码支付
     */
    @PreAuthorize("@ss.hasPermi('system:wxpay:edit')")
    @Log(title = "历史扫码支付", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WxPay wxPay)
    {
        return toAjax(wxPayService.updateWxPay(wxPay));
    }

}
