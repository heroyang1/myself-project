package com.backstage.system.mapper.order;

import java.util.List;
import com.backstage.system.domain.order.OshOrderSave;

/**
 * 创建订单Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-07
 */
public interface OshOrderSaveMapper 
{
    /**
     * 查询创建订单
     * 
     * @param id 创建订单主键
     * @return 创建订单
     */
    public OshOrderSave selectOshOrderSaveById(Long id);

    /**
     * 查询创建订单
     *
     * @param no 创建订单主键
     * @return 创建订单
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

}
