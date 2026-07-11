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
import com.backstage.system.domain.order.OshLearn;
import com.backstage.system.service.order.IOshLearnService;
import com.backstage.common.utils.poi.ExcelUtil;
import com.backstage.common.core.page.TableDataInfo;

/**
 * 立即学习Controller
 * 
 * @author ruoyi
 * @date 2026-03-10
 */
@RestController
@RequestMapping("/pc/order/learn")
public class OshLearnController extends BaseController
{
    @Autowired
    private IOshLearnService oshLearnService;

    /**
     * 查询立即学习列表
     */
    @PreAuthorize("@ss.hasPermi('system:learn:list')")
    @GetMapping("/list")
    public TableDataInfo list(OshLearn oshLearn)
    {
        startPage();
        List<OshLearn> list = oshLearnService.selectOshLearnList(oshLearn);
        return getDataTable(list);
    }

    /**
     * 导出立即学习列表
     */
    @PreAuthorize("@ss.hasPermi('system:learn:export')")
    @Log(title = "立即学习", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OshLearn oshLearn)
    {
        List<OshLearn> list = oshLearnService.selectOshLearnList(oshLearn);
        ExcelUtil<OshLearn> util = new ExcelUtil<OshLearn>(OshLearn.class);
        util.exportExcel(response, list, "立即学习数据");
    }

    /**
     * 获取立即学习详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:learn:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(oshLearnService.selectOshLearnById(id));
    }

    /**
     * 新增立即学习
     */
//    @PreAuthorize("@ss.hasPermi('system:learn:add')")
//    @Log(title = "立即学习", businessType = BusinessType.INSERT)
    @Anonymous
    @PostMapping
    public R add(@RequestBody Map<String, Object> params)
    {

        // 参数必须填写goods_id和type
        if (params.get("goods_id" ) == null || params.get("type") == null)
            return R.fail("参数错误");

        OshLearn oshLearn = oshLearnService.insertOshLearn((int)params.get("goods_id"),(String)params.get("type"));
        if (oshLearn == null)
            return R.fail("未查询到！goods_id");
        R r = new R();
        r.setMsg("ok");
        r.setData(oshLearn);
        r.setCode(20000);

        return r;
    }

    /**
     * 修改立即学习
     */
    @PreAuthorize("@ss.hasPermi('system:learn:edit')")
    @Log(title = "立即学习", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OshLearn oshLearn)
    {
        return toAjax(oshLearnService.updateOshLearn(oshLearn));
    }

}
