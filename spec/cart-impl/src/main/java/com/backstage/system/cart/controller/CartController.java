package com.backstage.system.cart.controller;

import com.backstage.common.core.domain.AjaxResult;
import com.backstage.system.cart.dto.CartAddDTO;
import com.backstage.system.cart.service.CartService;
import com.backstage.system.cart.vo.CartItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物车加购接口（C 端，统一 /pc 前缀）。
 * 注意：本文件属"框架适配层"，依赖 Spring/项目基类，不在纯 JDK 单测的编译范围内；
 * 它是 spec 第 4 节允许改动的清单之一，作为生产接入的落地形态给出。
 */
@RestController
@RequestMapping("/pc/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 加购。@Validated 触发 DTO 字段级校验（E2 缺省=零校验，A-11 教训）。
     * userId 从 C 端登录态取（OshUser），取不到即未登录，Service 抛 E7。
     */
    @PostMapping("/add")
    public AjaxResult add(@Validated @RequestBody CartAddDTO dto) {
        Long userId = currentUserId();
        CartItemVO vo = cartService.addItem(userId, dto);
        return AjaxResult.success(vo);
    }

    /** 从 C 端令牌上下文解析当前用户 id（生产由 OshUser 认证过滤器注入）。 */
    private Long currentUserId() {
        // TODO 生产接入：return OshUserContextHolder.getUserId();
        throw new UnsupportedOperationException("沙盒未接入 C 端认证上下文");
    }
}
