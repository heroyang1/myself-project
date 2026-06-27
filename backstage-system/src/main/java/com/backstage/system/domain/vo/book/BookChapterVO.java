package com.backstage.system.domain.vo.book;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 电子书章节视图对象
 *
 * @author backstage
 */
public class BookChapterVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 章节ID */
    private Long id;

    /** 章节标题 */
    private String title;

    /** 第几章 */
    @JsonProperty("chapter_no")
    private Integer chapterNo;

    /** 展示排序 */
    @JsonProperty("sort_order")
    private Integer sortOrder;

    /** 是否免费（0收费 1免费） */
    private Integer isFree;

    /** 章节内容 */
    private String content;

    /** Comic pages, only used when the parent book is comic. */
    private List<BookChapterPageVO> pages;

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

    public Integer getChapterNo()
    {
        return chapterNo;
    }

    public void setChapterNo(Integer chapterNo)
    {
        this.chapterNo = chapterNo;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getIsFree()
    {
        return isFree;
    }

    public void setIsFree(Integer isFree)
    {
        this.isFree = isFree;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public List<BookChapterPageVO> getPages() {
        return pages;
    }

    public void setPages(List<BookChapterPageVO> pages) {
        this.pages = pages;
    }
}
