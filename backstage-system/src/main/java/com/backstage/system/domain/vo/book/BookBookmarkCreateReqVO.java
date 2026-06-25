package com.backstage.system.domain.vo.book;

import com.backstage.common.annotation.OshResourceId;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class BookBookmarkCreateReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "电子书ID不能为空")
    @OshResourceId
    private Long bookId;

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    private String chapterTitle;

    @NotBlank(message = "书签内容不能为空")
    private String selectedText;

    private String prefixText;

    private String suffixText;

    private Integer startOffset;

    private Integer endOffset;

    private java.math.BigDecimal progressPercent;

    private String note;

    private String color;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public String getSelectedText() {
        return selectedText;
    }

    public void setSelectedText(String selectedText) {
        this.selectedText = selectedText;
    }

    public String getPrefixText() {
        return prefixText;
    }

    public void setPrefixText(String prefixText) {
        this.prefixText = prefixText;
    }

    public String getSuffixText() {
        return suffixText;
    }

    public void setSuffixText(String suffixText) {
        this.suffixText = suffixText;
    }

    public Integer getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(Integer startOffset) {
        this.startOffset = startOffset;
    }

    public Integer getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(Integer endOffset) {
        this.endOffset = endOffset;
    }

    public java.math.BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(java.math.BigDecimal progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
