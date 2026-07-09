package com.backstage.system.controller.order;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.order.OrderCreateRequest;
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
import com.backstage.system.domain.order.OshOrderSave;
import com.backstage.system.service.order.IOshOrderSaveService;
import com.backstage.common.utils.poi.ExcelUtil;
import com.backstage.common.core.page.TableDataInfo;

/**
 * 创建订单Controller
 * 
 * @author ruoyi
 * @date 2026-03-07
 */
@RestController
@RequestMapping("/pc/order/save")
public class OshOrderSaveController extends BaseController
{
    @Autowired
    private IOshOrderSaveService oshOrderSaveService;

    /**
     * 查询创建订单列表
     */
//    @PreAuthorize("@ss.hasPermi('order:save:list')")
    @Anonymous
    @GetMapping("/list")
    public TableDataInfo list(OshOrderSave oshOrderSave)
    {
        startPage();
        List<OshOrderSave> list = oshOrderSaveService.selectOshOrderSaveList(oshOrderSave);
        return getDataTable(list);
    }

    /**
     * 导出创建订单列表
     */
//    @PreAuthorize("@ss.hasPermi('order:save:export')")
    @Log(title = "创建订单", businessType = BusinessType.EXPORT)
    @Anonymous
    @PostMapping("/export")
    public void export(HttpServletResponse response, OshOrderSave oshOrderSave)
    {
        System.out.println(response);
        System.out.println(oshOrderSave);
        List<OshOrderSave> list = oshOrderSaveService.selectOshOrderSaveList(oshOrderSave);
        ExcelUtil<OshOrderSave> util = new ExcelUtil<OshOrderSave>(OshOrderSave.class);
        util.exportExcel(response, list, "创建订单数据");
    }

    /**
     * 获取创建订单详细信息
     */
    @PreAuthorize("@ss.hasPermi('order:save:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(oshOrderSaveService.selectOshOrderSaveById(id));
    }

    /**
     * 新增创建订单
     */
//    @PreAuthorize("@ss.hasPermi('order:save:add')")
    @Anonymous
//    @Log(title = "创建订单", businessType = BusinessType.INSERT)
    @PostMapping
    public R createOrder(@RequestBody OrderCreateRequest request)
    {
        try {
            OshOrderSave order = oshOrderSaveService.createOrder(request);

            R r = new R();
            R result = new R();
            result.setMsg("ok");
            result.setData(order);
            result.setCode(20000);
            return result;
        }
        catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 修改创建订单
     */
    @PreAuthorize("@ss.hasPermi('order:save:edit')")
    @Log(title = "创建订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OshOrderSave oshOrderSave)
    {
        return toAjax(oshOrderSaveService.updateOshOrderSave(oshOrderSave));
    }

}
