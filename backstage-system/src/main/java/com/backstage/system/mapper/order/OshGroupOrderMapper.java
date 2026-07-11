package com.backstage.system.mapper.order;

import java.util.List;
import com.backstage.system.domain.order.OshGroupOrder;

/**
 * 订单Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-11
 */
public interface OshGroupOrderMapper 
{
    /**
     * 查询订单
     * 
     * @param id 订单主键
     * @return 订单
     */
    public OshGroupOrder selectOshGroupOrderById(Long id);

    /**
     * 查询订单列表
     * 
     * @param oshGroupOrder 订单
     * @return 订单集合
     */
    public List<OshGroupOrder> selectOshGroupOrderList(OshGroupOrder oshGroupOrder);

    /**
     * 新增订单
     * 
     * @param oshGroupOrder 订单
     * @return 结果
     */
    public int insertOshGroupOrder(OshGroupOrder oshGroupOrder);

    /**
     * 修改订单
     * 
     * @param oshGroupOrder 订单
     * @return 结果
     */
    public int updateOshGroupOrder(OshGroupOrder oshGroupOrder);

}
