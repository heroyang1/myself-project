package com.backstage.system.domain.homepage.es;

import com.backstage.system.jackson.FlexibleLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OshHomePageHotBookEsDocument {
    private Long id;
    private String title;
    private String cover;
    private String description;
    private BigDecimal price;
    private Integer subCount;
    private Integer level;
    private Integer chapterCount;
    private Double hotScore;
    private String detailUrl;
    private List<String> tags;
    private Integer sortNo;
    private Integer sourceLimit;
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime syncTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getSubCount() { return subCount; }
    public void setSubCount(Integer subCount) { this.subCount = subCount; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getChapterCount() { return chapterCount; }
    public void setChapterCount(Integer chapterCount) { this.chapterCount = chapterCount; }
    public Double getHotScore() { return hotScore; }
    public void setHotScore(Double hotScore) { this.hotScore = hotScore; }
    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Integer getSourceLimit() { return sourceLimit; }
    public void setSourceLimit(Integer sourceLimit) { this.sourceLimit = sourceLimit; }
    public LocalDateTime getSyncTime() { return syncTime; }
    public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }
}
