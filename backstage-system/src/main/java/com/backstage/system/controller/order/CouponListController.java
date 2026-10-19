package com.backstage.system.controller.order;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.order.Coupon;
import com.github.pagehelper.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.backstage.system.service.order.ICouponListService;

/**
 * 卡券 Controller - 适配 /pc/card/list?page=1&limit=20
 * 
 * @author ruoyi
 * @date 2026-03-05
 */

@Api("查询用户优惠券")
@RestController
@RequestMapping("/pc/user_coupon")
public class CouponListController {

    /** 时间格式化（线程安全，可全局共享） */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Autowired
    private ICouponListService cardService;

    @ApiOperation("查询用户优惠券")
    @ApiParam("goods_id 课程/专栏 ID")
    @Anonymous
    @GetMapping("")
    public R<Coupon> getUserCouponList(
            @ApiParam(name = "goods_id", value = "课程/专栏等ID", required = true)
            @RequestParam Long goods_id,    // 必填：goods_id 课程/专栏 ID
            @ApiParam(name = "type", value = "类型（column 专栏/course 课程）", required = true)
            @RequestParam String type,      // 必填：type（column 专栏/course 课程）
            @ApiParam(name = "page", value = "页面码", required = true)
            @RequestParam Integer page    // 必填：page 页面码
    ) {
        // 构建查询条件
        Coupon coupon = new Coupon();
        coupon.setGoodsId(goods_id);
        coupon.setType(type);
        
        // 查询列表（未使用的优惠券）
        // card.setUsed(0);
        
        // 使用 PageHelper 分页（每页 20 条）
        PageHelper.startPage(page, 20);
        List<Coupon> list = cardService.selectCardList(coupon);
        
        // 转换为前端需要的格式，price 转为字符串，时间格式化为 yyyy-MM-dd HH:mm:ss
        List<Map<String, Object>> formattedList = new ArrayList<>();
        for (Coupon item : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("title", item.getTitle());
            map.put("price", item.getPrice() != null ? item.getPrice().toString() : "0.00");
            map.put("start_time", item.getStartTime() != null ? DATE_TIME_FORMATTER.format(item.getStartTime().toInstant()) : "");
            map.put("end_time", item.getEndTime() != null ? DATE_TIME_FORMATTER.format(item.getEndTime().toInstant()) : "");
            map.put("type", item.getType());
            map.put("used", item.getUsed());
            map.put("goods_id", item.getGoodsId());
            formattedList.add(map);
        }
        
        R r = new R();

        r.setCode(20000);
        r.setMsg("ok");
        r.setData(new LinkedHashMap<String, Object>() {{
            put("count", (long) list.size());
            put("rows", formattedList);
        }});

        return r;
    }
}
