package com.backstage.system.service.order;

import java.util.List;
import com.backstage.system.domain.order.OshOrderSave;
import com.backstage.system.domain.order.OrderCreateRequest;

/**
 * 创建订单Service接口
 * 
 * @author ruoyi
 * @date 2026-03-07
 */
public interface IOshOrderSaveService 
{
    /**
     * 查询创建订单
     * 
     * @param id 创建订单主键
     * @return 创建订单
     */
    public OshOrderSave selectOshOrderSaveById(Long id);

    /**
     * 查询订单编号
     *
     * @param no 订单编号
     * @return 订单数据
     */

    public OshOrderSave selectOshOrderSaveByNo(String no);


    /**
     * 查询创建订单列表
     * 
     * @param oshOrderSave 创建订单
     * @return 创建订单集合
     */
    public List<OshOrderSave> selectOshOrderSaveList(OshOrderSave oshOrderSave);

    /**
     * 新增创建订单
     * 
     * @param oshOrderSave 创建订单
     * @return 结果
     */
    public int insertOshOrderSave(OshOrderSave oshOrderSave);

    /**
     * 修改创建订单
     * 
     * @param oshOrderSave 创建订单
     * @return 结果
     */
    public int updateOshOrderSave(OshOrderSave oshOrderSave);

    /**
     * 创建订单
     * 
     * @param request 订单创建请求参数
     * @return 订单信息
     */
    public OshOrderSave createOrder(OrderCreateRequest request);
}
