package com.backstage.system.domain;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 电子书章节对象，对应表 osh_book_chapter。
 */
@TableName("osh_book_chapter")
public class BookChapter extends OSHBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 章节ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 电子书ID */
    private Long bookId;

    /** 章节标题 */
    private String title;

    /** 章节内容，统一按 Markdown 保存和渲染 */
    private String content;

    /** 第几章 */
    private Integer chapterNo;

    /** 展示排序 */
    private Integer sortOrder;

    /** 是否免费：0收费，1免费 */
    private Integer isFree;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(Integer chapterNo) {
        this.chapterNo = chapterNo;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getIsFree() {
        return isFree;
    }

    public void setIsFree(Integer isFree) {
        this.isFree = isFree;
    }
}
