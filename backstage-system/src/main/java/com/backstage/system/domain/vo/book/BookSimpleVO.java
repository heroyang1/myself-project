package com.backstage.system.domain.vo.book;

import java.io.Serializable;

/**
 * 电子书简单信息视图对象
 *
 * @author backstage
 */
public class BookSimpleVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 电子书ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 封面 */
    private String cover;

    /** Book media type: 1 ebook, 2 comic. */
    private Integer mediaType;

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

    public Integer getMediaType() {
        return mediaType;
    }

    public void setMediaType(Integer mediaType) {
        this.mediaType = mediaType;
    }
}
