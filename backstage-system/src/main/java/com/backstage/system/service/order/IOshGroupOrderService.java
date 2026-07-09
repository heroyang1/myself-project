package com.backstage.system.service.order;

import java.util.List;
import com.backstage.system.domain.order.OshGroupOrder;

/**
 * 订单Service接口
 * 
 * @author ruoyi
 * @date 2026-03-11
 */
public interface IOshGroupOrderService 
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

    public int insertOshGroupOrder(OshGroupOrder oshGroupOrder);

    /**
     * 新增订单
     * 
     * @param group_id 拼团id
     * @param group_work_id 组团id 可选
     * @return 结果
     */
    public OshGroupOrder findGroupId(Integer group_id, Integer group_work_id);


    /**
     * 修改订单
     * 
     * @param oshGroupOrder 订单
     * @return 结果
     */
    public int updateOshGroupOrder(OshGroupOrder oshGroupOrder);

}
