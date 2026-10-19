package com.backstage.system.service.order;

import java.util.List;
import com.backstage.system.domain.order.OshLearn;

/**
 * 立即学习Service接口
 * 
 * @author ruoyi
 * @date 2026-03-10
 */
public interface IOshLearnService 
{
    /**
     * 查询立即学习
     * 
     * @param id 立即学习主键
     * @return 立即学习
     */
    public OshLearn selectOshLearnById(Long id);

    /**
     * 查询立即学习列表
     * 
     * @param oshLearn 立即学习
     * @return 立即学习集合
     */
    public List<OshLearn> selectOshLearnList(OshLearn oshLearn);

    /**
     * 新增立即学习
     * 
     * @param goods_id 立即学习
     * @return 结果
     */
    public OshLearn insertOshLearn(int goods_id,String type);

    /**
     * 修改立即学习
     * 
     * @param oshLearn 立即学习
     * @return 结果
     */
    public int updateOshLearn(OshLearn oshLearn);

}
