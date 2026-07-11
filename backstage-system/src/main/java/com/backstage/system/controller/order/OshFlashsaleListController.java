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
import com.backstage.system.domain.order.OshFlashsaleList;
import com.backstage.system.service.order.IOshFlashsaleListService;
import com.backstage.common.utils.poi.ExcelUtil;
import com.backstage.common.core.page.TableDataInfo;

/**
 * 创建秒杀订单Controller
 * 
 * @author ruoyi
 * @date 2026-03-10
 */
@RestController
@RequestMapping("/pc/order/flashsale")
public class OshFlashsaleListController extends BaseController
{
    @Autowired
    private IOshFlashsaleListService oshFlashsaleListService;

    /**
     * 查询创建秒杀订单列表
     */
    @PreAuthorize("@ss.hasPermi('system:list:list')")
    @GetMapping("/list")
    public TableDataInfo list(OshFlashsaleList oshFlashsaleList)
    {
        startPage();
        List<OshFlashsaleList> list = oshFlashsaleListService.selectOshFlashsaleListList(oshFlashsaleList);
        return getDataTable(list);
    }

    /**
     * 导出创建秒杀订单列表
     */
    @PreAuthorize("@ss.hasPermi('system:list:export')")
    @Log(title = "创建秒杀订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OshFlashsaleList oshFlashsaleList)
    {
        List<OshFlashsaleList> list = oshFlashsaleListService.selectOshFlashsaleListList(oshFlashsaleList);
        ExcelUtil<OshFlashsaleList> util = new ExcelUtil<OshFlashsaleList>(OshFlashsaleList.class);
        util.exportExcel(response, list, "创建秒杀订单数据");
    }

    /**
     * 获取创建秒杀订单详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:list:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(oshFlashsaleListService.selectOshFlashsaleListById(id));
    }

    /**
     * 新增创建秒杀订单
     */
//    @PreAuthorize("@ss.hasPermi('system:list:add')")
//    @Log(title = "创建秒杀订单", businessType = BusinessType.INSERT)
    // 秒杀只有课程
    @Anonymous
    @PostMapping
    public R add(@RequestBody Map<String, Integer> params)
    {

        OshFlashsaleList fl = oshFlashsaleListService.insertOshFlashsaleList((long)params.get("flashsale_id"));
        if (fl== null)
            return R.fail("flashsale_id 不存在！");
        R r = new R();
        r.setMsg("ok");
        r.setData(fl);
        r.setCode(20000);
        return r;
    }

    /**
     * 修改创建秒杀订单
     */
    @PreAuthorize("@ss.hasPermi('system:list:edit')")
    @Log(title = "创建秒杀订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OshFlashsaleList oshFlashsaleList)
    {
        return toAjax(oshFlashsaleListService.updateOshFlashsaleList(oshFlashsaleList));
    }

}
