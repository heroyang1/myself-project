package com.backstage.system.domain.vo.book;

import java.io.Serializable;
import java.util.List;

public class BookChapterSaveUpdateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long bookId;

    private String title;

    private String content;

    private Integer chapterNo;

    private Integer sortOrder;

    private Integer isFree;

    private List<BookChapterPageVO> pages;

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

    public List<BookChapterPageVO> getPages() {
        return pages;
    }

    public void setPages(List<BookChapterPageVO> pages) {
        this.pages = pages;
    }
}
