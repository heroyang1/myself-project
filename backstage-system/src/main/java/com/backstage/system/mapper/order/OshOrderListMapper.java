package com.backstage.system.mapper.order;

import java.util.List;
import com.backstage.system.domain.order.OshOrderList;

/**
 * 我的订单列Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-09
 */
public interface OshOrderListMapper 
{
    /**
     * 查询我的订单列
     * 
     * @param id 我的订单列主键
     * @return 我的订单列
     */
    public OshOrderList selectOshOrderListById(Long id);

    /**
     * 查询我的订单列列表
     * 
     * @param oshOrderList 我的订单列
     * @return 我的订单列集合
     */
    public List<OshOrderList> selectOshOrderListList(OshOrderList oshOrderList);

    /**
     * 新增我的订单列
     * 
     * @param oshOrderList 我的订单列
     * @return 结果
     */
    public int insertOshOrderList(OshOrderList oshOrderList);

    /**
     * 修改我的订单列
     * 
     * @param oshOrderList 我的订单列
     * @return 结果
     */
    public int updateOshOrderList(OshOrderList oshOrderList);

}
