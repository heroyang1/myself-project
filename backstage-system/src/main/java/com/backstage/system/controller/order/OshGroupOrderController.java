package com.backstage.system.controller.order;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 拼团订单 Controller
 * 
 * @author ruoyi
 * @date 2026-03-11
 */
@Api(tags = "拼团订单接口")
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
    @ApiOperation("查询拼团订单列表")
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
    @Log(title = "拼团订单", businessType = BusinessType.EXPORT)
    @ApiOperation("导出拼团订单列表")
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
    @ApiOperation("获取拼团订单详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(oshGroupOrderService.selectOshGroupOrderById(id));
    }

    /**
     * 新增订单
     */
    @Anonymous
    @Log(title = "拼团订单", businessType = BusinessType.INSERT)
    @ApiOperation("新增拼团订单")
    @PostMapping
    public R<OshGroupOrder> add(@RequestBody Map<String, Integer> params)
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
    @Log(title = "拼团订单", businessType = BusinessType.UPDATE)
    @ApiOperation("修改拼团订单")
    @PutMapping
    public AjaxResult edit(@RequestBody OshGroupOrder oshGroupOrder)
    {
        return toAjax(oshGroupOrderService.updateOshGroupOrder(oshGroupOrder));
    }

    /**
     * 删除订单
     */
    @PreAuthorize("@ss.hasPermi('system:order:remove')")
    @Log(title = "拼团订单", businessType = BusinessType.DELETE)
    @ApiOperation("删除拼团订单")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(oshGroupOrderService.deleteOshGroupOrderByIds(ids));
    }
}
