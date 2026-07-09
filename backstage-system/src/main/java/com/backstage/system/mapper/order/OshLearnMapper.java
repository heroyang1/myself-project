package com.backstage.system.mapper.order;

import java.util.List;
import com.backstage.system.domain.order.OshLearn;

/**
 * 立即学习Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-10
 */
public interface OshLearnMapper 
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
     * @param oshLearn 立即学习
     * @return 结果
     */
    public int insertOshLearn(OshLearn oshLearn);

    /**
     * 修改立即学习
     * 
     * @param oshLearn 立即学习
     * @return 结果
     */
    public int updateOshLearn(OshLearn oshLearn);

}
