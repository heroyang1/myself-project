package com.backstage.system.controller.order;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
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
import com.backstage.system.domain.order.OshGroupOrder;
import com.backstage.system.service.order.IOshGroupOrderService;
import com.backstage.common.utils.poi.ExcelUtil;
import com.backstage.common.core.page.TableDataInfo;

/**
 * 订单Controller
 * 
 * @author ruoyi
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/pc/order/group")
public class OshGroupOrderController extends BaseController
{
    @Autowired
    private IOshGroupOrderService oshGroupOrderService;

    /**
     * 查询订单列表
     */
    @PreAuthorize("@ss.hasPermi('system:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(OshGroupOrder oshGroupOrder)
    {
        startPage();
        List<OshGroupOrder> list = oshGroupOrderService.selectOshGroupOrderList(oshGroupOrder);
        return getDataTable(list);
    }

    /**
     * 导出订单列表
     */
    @PreAuthorize("@ss.hasPermi('system:order:export')")
    @Log(title = "订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OshGroupOrder oshGroupOrder)
    {
        List<OshGroupOrder> list = oshGroupOrderService.selectOshGroupOrderList(oshGroupOrder);
        ExcelUtil<OshGroupOrder> util = new ExcelUtil<OshGroupOrder>(OshGroupOrder.class);
        util.exportExcel(response, list, "订单数据");
    }

    /**
     * 获取订单详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:order:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(oshGroupOrderService.selectOshGroupOrderById(id));
    }

    /**
     * 新增订单
     */
//    @PreAuthorize("@ss.hasPermi('system:order:add')")
//    @Log(title = "订单", businessType = BusinessType.INSERT)
    @Anonymous
    @PostMapping
    public R add(@RequestBody Map<String, Integer> params)
    {
        OshGroupOrder go = oshGroupOrderService.findGroupId( (Integer) params.get("group_id"), (Integer) params.get("group_work_id"));
        if (go==null)
            return R.fail("group_id不存在");
        R r = R.ok(go, "ok");
        return r;
    }

    /**
     * 修改订单
     */
    @PreAuthorize("@ss.hasPermi('system:order:edit')")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OshGroupOrder oshGroupOrder)
    {
        return toAjax(oshGroupOrderService.updateOshGroupOrder(oshGroupOrder));
    }

}
