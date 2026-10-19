package com.backstage.system.domain.vo.book;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 电子书保存请求视图对象（新增/修改）
 *
 * @author backstage
 */
public class BookSaveReqVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 电子书ID（更新时传） */
    private Long id;

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
    private BigDecimal tPrice;

    /** 权限等级 */
    private Integer level;

    /** Book media type: 1 ebook, 2 comic. */
    private Integer mediaType;

    /** 标签列表 */
    private List<String> tags;

    /** 章节列表 */
    private List<BookChapterSaveUpdateVO> chapters;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getCover()
    {
        return cover;
    }

    public void setCover(String cover)
    {
        this.cover = cover;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTryContent()
    {
        return tryContent;
    }

    public void setTryContent(String tryContent)
    {
        this.tryContent = tryContent;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public BigDecimal getTPrice()
    {
        return tPrice;
    }

    public void setTPrice(BigDecimal tPrice)
    {
        this.tPrice = tPrice;
    }

    public BigDecimal gettPrice() {
        return tPrice;
    }

    public void settPrice(BigDecimal tPrice) {
        this.tPrice = tPrice;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<BookChapterSaveUpdateVO> getChapters() {
        return chapters;
    }

    public void setChapters(List<BookChapterSaveUpdateVO> chapters) {
        this.chapters = chapters;
    }
}
