package com.backstage.system.service.order.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.backstage.common.utils.DateUtils;
import com.backstage.common.utils.uuid.UUID;
import com.backstage.system.domain.group.GroupActivity;
import com.backstage.system.mapper.group.GroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.backstage.system.mapper.order.OshGroupOrderMapper;
import com.backstage.system.domain.order.OshGroupOrder;
import com.backstage.system.service.order.IOshGroupOrderService;

/**
 * 订单Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-11
 */
@Service
public class OshGroupOrderServiceImpl implements IOshGroupOrderService 
{
    @Autowired
    private OshGroupOrderMapper oshGroupOrderMapper;

    @Autowired
    private GroupMapper groupMapper;

    /**
     * 查询订单
     * 
     * @param id 订单主键
     * @return 订单
     */
    @Override
    public OshGroupOrder selectOshGroupOrderById(Long id)
    {
        return oshGroupOrderMapper.selectOshGroupOrderById(id);
    }

    /**
     * 查询订单列表
     * 
     * @param oshGroupOrder 订单
     * @return 订单
     */
    @Override
    public List<OshGroupOrder> selectOshGroupOrderList(OshGroupOrder oshGroupOrder)
    {
        return oshGroupOrderMapper.selectOshGroupOrderList(oshGroupOrder);
    }

    /**
     * 新增订单
     * 
     * @param oshGroupOrder 订单
     * @return 结果
     */
    @Override
    public int insertOshGroupOrder(OshGroupOrder oshGroupOrder)
    {

        return oshGroupOrderMapper.insertOshGroupOrder(oshGroupOrder);
    }

    /**
     * 修改订单
     * 
     * @param oshGroupOrder 订单
     * @return 结果
     */
    @Override
    public int updateOshGroupOrder(OshGroupOrder oshGroupOrder)
    {
        return oshGroupOrderMapper.updateOshGroupOrder(oshGroupOrder);
    }

    public OshGroupOrder CreateGroupList(GroupActivity ga){

        OshGroupOrder a = new OshGroupOrder();
        a.setId(ga.getId().longValue());
        a.setSchoolId(1L);
        a.setUserId(1L);
        // 生成订单号：日期 (yyyyMMdd) + 随机字符串
        String orderNo = DateUtils.dateTime() + "_" + UUID.randomUUID().toString().substring(0, 9);
        a.setNo(orderNo);
        a.setStatus("pendding");
        a.setPrice(new BigDecimal(ga.getPrice()));
        a.setTotalPrice(new BigDecimal(ga.getpNum()));
        a.setType("group");
        Date date = new Date();

        a.setUpdatedTime(date);
        a.setCreatedTime(date);

        oshGroupOrderMapper.insertOshGroupOrder(a);
        return a;
    }

    @Override
    public OshGroupOrder findGroupId(Integer group_id, Integer group_work_id) {

        // 调用service获取拼团相关数值 group_id对应数据
        // 返回json格式 id school_id user_id no status price total_price type updated_time created_time
        GroupActivity group = groupMapper.getGroupActivityById(Long.valueOf(group_id));


        OshGroupOrder oshGroupOrder = CreateGroupList(group);


    	return oshGroupOrder;
    }
}
