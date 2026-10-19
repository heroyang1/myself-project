package com.backstage.system.service.order;

import java.util.List;
import com.backstage.system.domain.order.OshFlashsaleList;

/**
 * 创建秒杀订单Service接口
 * 
 * @author ruoyi
 * @date 2026-03-10
 */
public interface IOshFlashsaleListService 
{
    /**
     * 查询创建秒杀订单
     * 
     * @param id 创建秒杀订单主键
     * @return 创建秒杀订单
     */
    public OshFlashsaleList selectOshFlashsaleListById(Long id);

    /**
     * 查询创建秒杀订单列表
     * 
     * @param oshFlashsaleList 创建秒杀订单
     * @return 创建秒杀订单集合
     */
    public List<OshFlashsaleList> selectOshFlashsaleListList(OshFlashsaleList oshFlashsaleList);

    /**
     * 新增创建秒杀订单
     * 
     * @param flashsale_id 创建秒杀订单
     * @return 结果
     */
    public OshFlashsaleList insertOshFlashsaleList(Long flashsale_id);

    /**
     * 修改创建秒杀订单
     * 
     * @param oshFlashsaleList 创建秒杀订单
     * @return 结果
     */
    public int updateOshFlashsaleList(OshFlashsaleList oshFlashsaleList);

}
