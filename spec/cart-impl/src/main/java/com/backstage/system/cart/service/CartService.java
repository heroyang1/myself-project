package com.backstage.system.cart.service;

import com.backstage.system.cart.dto.CartAddDTO;
import com.backstage.system.cart.vo.CartItemVO;

/**
 * 购物车服务（spec/cart-add.md 第 2 步「执行」）。
 */
public interface CartService {

    /**
     * 加购。
     *
     * @param userId 当前登录用户（C 端 OshUser 的 userId；null 视为未登录 E7）
     * @param dto    加购请求
     */
    CartItemVO addItem(Long userId, CartAddDTO dto);
}
