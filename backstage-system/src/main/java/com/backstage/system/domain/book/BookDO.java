package com.backstage.system.domain.book;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 电子书对象，对应表 osh_book。
 */
@TableName("osh_book")
public class BookDO extends OSHBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 电子书ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资源编号 */
    private String no;

    /** 标题 */
    private String title;

    /** 封面 */
    private String cover;

    /** 描述 */
    @JsonProperty("desc")
    private String description;

    /** 试读内容 */
    @JsonProperty("try")
    private String tryContent;

    /** 价格 */
    private BigDecimal price;

    /** 原价 */
    @JsonProperty("t_price")
    private BigDecimal originalPrice;

    /** 订阅数 */
    @JsonProperty("sub_count")
    private Integer subCount;

    /** 访问等级：1免费，2VIP，3小班，4内部成员，6创始人 */
    private Integer level;

    /** Book media type: 1 ebook, 2 comic. */
    private Integer mediaType;

    /** 状态：0正常，1下架 */
    private String status;

    /** 备注 */
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTryContent() {
        return tryContent;
    }

    public void setTryContent(String tryContent) {
        this.tryContent = tryContent;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Integer getSubCount() {
        return subCount;
    }

    public void setSubCount(Integer subCount) {
        this.subCount = subCount;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getMediaType() {
        return mediaType;
    }

    public void setMediaType(Integer mediaType) {
        this.mediaType = mediaType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
