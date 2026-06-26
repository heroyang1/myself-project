package com.backstage.system.domain.vo.book;

import java.io.Serializable;
import java.util.List;

/**
 * 电子书章节内容视图对象
 *
 * @author backstage
 */
public class BookChapterContentVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 章节标题 */
    private String title;

    /** 章节内容 */
    private String content;

    /** 是否免费（0收费 1免费） */
    private Integer isFree;

    /** Book media type: 1 ebook, 2 comic. */
    private Integer mediaType;

    /** Comic pages, only returned for comic books. */
    private List<BookChapterPageVO> pages;

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Integer getIsFree()
    {
        return isFree;
    }

    public void setIsFree(Integer isFree)
    {
        this.isFree = isFree;
    }

    public Integer getMediaType() {
        return mediaType;
    }

    public void setMediaType(Integer mediaType) {
        this.mediaType = mediaType;
    }

    public List<BookChapterPageVO> getPages() {
        return pages;
    }

    public void setPages(List<BookChapterPageVO> pages) {
        this.pages = pages;
    }
}
