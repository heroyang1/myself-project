package com.backstage.system.controller.order;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.backstage.common.annotation.Log;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.AjaxResult;
import com.backstage.common.enums.BusinessType;
import com.backstage.system.domain.order.OshOrderList;
import com.backstage.system.service.order.IOshOrderListService;
import com.backstage.common.utils.poi.ExcelUtil;
import com.backstage.common.core.page.TableDataInfo;

/**
 * 我的订单列Controller
 * 
 * @author ruoyi
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/pc/order")
public class OshOrderListController extends BaseController
{
    @Autowired
    private IOshOrderListService oshOrderListService;

    /**
     * 查询我的订单列列表
     */
//    @PreAuthorize("@ss.hasPermi('system:list:list')")
//
    @Anonymous
    @GetMapping("/list")
    public R list(@RequestParam Integer page, @RequestParam(defaultValue = "20") Integer limit) {
        PageHelper.startPage(page, limit);
        List<OshOrderList> list = oshOrderListService.selectOshOrderListList(new OshOrderList());

        // 统计count
        PageInfo<OshOrderList> pageInfo = new PageInfo<>(list);

        // 保持顺序
        LinkedHashMap<String, Object> data1 = new LinkedHashMap<>();
        data1.put("count", pageInfo.getTotal());
        data1.put("rows", pageInfo.getList());


        R r = new R();
        r.setMsg("ok");
        r.setData(data1);
        r.setCode(20000);
        return r;

    }

    /**
     * 导出我的订单列列表
     */
    @PreAuthorize("@ss.hasPermi('system:list:export')")
    @Log(title = "我的订单列", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OshOrderList oshOrderList)
    {
        List<OshOrderList> list = oshOrderListService.selectOshOrderListList(oshOrderList);
        ExcelUtil<OshOrderList> util = new ExcelUtil<OshOrderList>(OshOrderList.class);
        util.exportExcel(response, list, "我的订单列数据");
    }

    /**
     * 获取我的订单列详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:list:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(oshOrderListService.selectOshOrderListById(id));
    }

    /**
     * 新增我的订单列
     */
    @PreAuthorize("@ss.hasPermi('system:list:add')")
    @Log(title = "我的订单列", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OshOrderList oshOrderList)
    {
        return toAjax(oshOrderListService.insertOshOrderList(oshOrderList));
    }

    /**
     * 修改我的订单列
     */
    @PreAuthorize("@ss.hasPermi('system:list:edit')")
    @Log(title = "我的订单列", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OshOrderList oshOrderList)
    {
        return toAjax(oshOrderListService.updateOshOrderList(oshOrderList));
    }

}
