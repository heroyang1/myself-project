package com.backstage.system.service.order.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.backstage.system.mapper.order.OshOrderListMapper;
import com.backstage.system.domain.order.OshOrderList;
import com.backstage.system.service.order.IOshOrderListService;

/**
 * 我的订单列Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-09
 */
@Service
public class OshOrderListServiceImpl implements IOshOrderListService 
{
    @Autowired
    private OshOrderListMapper oshOrderListMapper;

    /**
     * 查询我的订单列
     * 
     * @param id 我的订单列主键
     * @return 我的订单列
     */
    @Override
    public OshOrderList selectOshOrderListById(Long id)
    {
        return oshOrderListMapper.selectOshOrderListById(id);
    }

    /**
     * 查询我的订单列列表
     * 
     * @param oshOrderList 我的订单列
     * @return 我的订单列
     */
    @Override
    public List<OshOrderList> selectOshOrderListList(OshOrderList oshOrderList)
    {
        return oshOrderListMapper.selectOshOrderListList(oshOrderList);
    }

    /**
     * 新增我的订单列
     * 
     * @param oshOrderList 我的订单列
     * @return 结果
     */
    @Override
    public int insertOshOrderList(OshOrderList oshOrderList)
    {
        return oshOrderListMapper.insertOshOrderList(oshOrderList);
    }

    /**
     * 修改我的订单列
     * 
     * @param oshOrderList 我的订单列
     * @return 结果
     */
    @Override
    public int updateOshOrderList(OshOrderList oshOrderList)
    {
        return oshOrderListMapper.updateOshOrderList(oshOrderList);
    }

}
