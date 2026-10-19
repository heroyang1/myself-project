package com.backstage.system.service.order.impl;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import com.backstage.common.utils.DateUtils;
import com.backstage.common.utils.uuid.UUID;
import com.backstage.system.domain.SysFlashSale;
import com.backstage.system.mapper.SysFlashsaleMapper;
import com.backstage.system.mapper.order.OshBookMapper;
import com.backstage.system.mapper.order.ColumnPriceMapper;
import com.backstage.system.mapper.order.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.backstage.system.mapper.order.OshFlashsaleListMapper;
import com.backstage.system.domain.order.OshFlashsaleList;
import com.backstage.system.service.order.IOshFlashsaleListService;

/**
 * 创建秒杀订单Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-10
 */
@Service
public class OshFlashsaleListServiceImpl implements IOshFlashsaleListService 
{
    /** UTC ISO 时间格式（线程安全，可全局共享） */
    private static final DateTimeFormatter ISO_UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    @Autowired
    private OshFlashsaleListMapper oshFlashsaleListMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private ColumnPriceMapper columnPriceMapper;

    @Autowired
    private OshBookMapper oshBookMapper;

    @Autowired
    private SysFlashsaleMapper flashsaleMapper;
    /**
     * 查询创建秒杀订单
     * 
     * @param id 创建秒杀订单主键
     * @return 创建秒杀订单
     */
    @Override
    public OshFlashsaleList selectOshFlashsaleListById(Long id)
    {
        return oshFlashsaleListMapper.selectOshFlashsaleListById(id);
    }

    /**
     * 查询创建秒杀订单列表
     * 
     * @param oshFlashsaleList 创建秒杀订单
     * @return 创建秒杀订单
     */
    @Override
    public List<OshFlashsaleList> selectOshFlashsaleListList(OshFlashsaleList oshFlashsaleList)
    {
        return oshFlashsaleListMapper.selectOshFlashsaleListList(oshFlashsaleList);
    }



    public OshFlashsaleList CreateList(String flashId,String price, String totalPrice){

        OshFlashsaleList a = new OshFlashsaleList();
        a.setSchoolId("1");
        a.setUserId("1");
        // ruoyi自带 拼接年月日 和 uuid
        String orderNo = DateUtils.datePath() + "_" + UUID.randomUUID().toString().substring(0, 9);
        a.setNo(orderNo);

        a.setStatus("pending");
        a.setPrice(price);
        a.setTotalPrice(totalPrice);
        a.setType("flashsale");
        a.setFlashsaleId(flashId);
        Date date = new Date();
        // 格式化为 UTC ISO 时间（如 2021-06-20T09:41:21.245Z）
        String isoString = ISO_UTC_FORMATTER.format(date.toInstant());
        a.setUpdatedTime(isoString);
        a.setCreatedTime(isoString);

        oshFlashsaleListMapper.insertOshFlashsaleList(a);
        return a;
    }

    /**
     * 新增创建秒杀订单
     * 
     * @param flashsale_id 创建秒杀订单
     * @return 结果
     */
    @Override
    public OshFlashsaleList insertOshFlashsaleList(Long flashsale_id)
    {


        boolean is_exist = false;
        String price="",totalPrice="",goodId="";
        if (flashsale_id>0){
            // 秒杀列表查询id是否存在 并且获取价格price 和 goods_id
            SysFlashSale flashsale = flashsaleMapper.selectOshFlashsaleById(flashsale_id);
            price = String.valueOf(flashsale.getFlashPrice());
            goodId = String.valueOf(flashsale.getGoodsId());

            String flashsale_type = flashsale.getFlashType();
            if(flashsale_type.equals("course")){
                BigDecimal coursePrice = courseMapper.selectPriceById(Long.valueOf(goodId));
                totalPrice = coursePrice != null ? coursePrice.toString() : "0.00";
            }else if(flashsale_type.equals("column")){
                BigDecimal columnPrice = columnPriceMapper.selectPriceById(Long.valueOf(goodId));
                totalPrice = columnPrice != null ? columnPrice.toString() : "0.00";
            }else if(flashsale_type.equals("book")) {
                BigDecimal bookPrice = oshBookMapper.selectPriceById(Long.valueOf(goodId));
                totalPrice = bookPrice != null ? bookPrice.toString() : "0.00";
            }

            if (flashsale!=null)
                is_exist=true;
        }


        if (!is_exist){
            return null;
        }
        //  goods_id 查找course column book 三张表的id是否匹配
        // 获取价格price
        // 查找课程表的内容获取价格 price(total_price)
        OshFlashsaleList fl = CreateList(String.valueOf(flashsale_id),price,totalPrice);

        return fl;
    }

    /**
     * 修改创建秒杀订单
     * 
     * @param oshFlashsaleList 创建秒杀订单
     * @return 结果
     */
    @Override
    public int updateOshFlashsaleList(OshFlashsaleList oshFlashsaleList)
    {
        return oshFlashsaleListMapper.updateOshFlashsaleList(oshFlashsaleList);
    }

}
