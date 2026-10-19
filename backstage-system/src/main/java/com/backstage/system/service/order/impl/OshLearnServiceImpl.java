package com.backstage.system.service.order.impl;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import com.backstage.common.utils.DateUtils;
import com.backstage.common.utils.uuid.UUID;
import com.backstage.system.mapper.order.OshBookMapper;
import com.backstage.system.mapper.order.ColumnPriceMapper;
import com.backstage.system.mapper.order.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.backstage.system.mapper.order.OshLearnMapper;
import com.backstage.system.domain.order.OshLearn;
import com.backstage.system.service.order.IOshLearnService;

/**
 * 立即学习Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-10
 */
@Service
public class OshLearnServiceImpl implements IOshLearnService 
{
    /** UTC ISO 时间格式（线程安全，可全局共享） */
    private static final DateTimeFormatter ISO_UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    @Autowired
    private OshLearnMapper oshLearnMapper;

    @Autowired(required = false)
    private CourseMapper courseMapper;

    @Autowired(required = false)
    private ColumnPriceMapper columnPriceMapper;

    @Autowired(required = false)
    private OshBookMapper oshBookMapper;

    /**
     * 查询立即学习
     * 
     * @param id 立即学习主键
     * @return 立即学习
     */
    @Override
    public OshLearn selectOshLearnById(Long id)
    {
        return oshLearnMapper.selectOshLearnById(id);
    }

    /**
     * 查询立即学习列表
     * 
     * @param oshLearn 立即学习
     * @return 立即学习
     */
    @Override
    public List<OshLearn> selectOshLearnList(OshLearn oshLearn)
    {
        return oshLearnMapper.selectOshLearnList(oshLearn);
    }



    public OshLearn CreateList(String type){

        OshLearn a = new OshLearn();
        a.setSchoolId(1L);
        a.setUserId(1L);
        String orderNo = DateUtils.datePath() + "_" + UUID.randomUUID().toString().substring(0, 9);
        a.setNo(orderNo);
        a.setStatus("success");
        a.setPrice("0.00");
        a.setTotalPrice("0.00");
        a.setType(type);
        a.setPayMethod("free");
        Date date = new Date();

        // 格式化为 UTC ISO 时间（如 2021-06-20T09:41:21.245Z）
        String isoString = ISO_UTC_FORMATTER.format(date.toInstant());
        a.setPayTime(isoString);
        a.setUpdatedTime(isoString);
        a.setCreatedTime(isoString);

        oshLearnMapper.insertOshLearn(a);
        return a;
    }
    /**
     * 新增立即学习
     * 
     * @param goods_id 立即学习
     * @return 结果
     */
    @Override
    public OshLearn insertOshLearn(int goods_id, String type)
    {


        BigDecimal c = null;
        if (type.equals("course")){
            c = courseMapper.selectPriceById((long)goods_id);
            if (c != null)
                return CreateList(type);
        }else if (type.equals("column")){
            c = columnPriceMapper.selectPriceById((long)goods_id);
            if (c != null)
                return CreateList(type);
        }else if (type.equals("book")){
            c = oshBookMapper.selectPriceById((long)goods_id);
            if (c != null)
                return CreateList(type);
        }
        return null;
    }

    /**
     * 修改立即学习
     * 
     * @param oshLearn 立即学习
     * @return 结果
     */
    @Override
    public int updateOshLearn(OshLearn oshLearn)
    {
        return oshLearnMapper.updateOshLearn(oshLearn);
    }

}
