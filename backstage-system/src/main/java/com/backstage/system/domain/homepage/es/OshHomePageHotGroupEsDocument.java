package com.backstage.system.domain.homepage.es;

import com.backstage.system.jackson.FlexibleLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OshHomePageHotGroupEsDocument {
    private Long groupId;
    private Long goodsId;
    private String type;
    private String title;
    private String cover;
    private BigDecimal groupPrice;
    private BigDecimal originPrice;
    private Integer pNum;
    private Integer maxNum;
    private Integer groupMinNum;
    private Integer groupMaxNum;
    private String startTime;
    private String endTime;
    private String description;
    private Integer currentNum;
    private BigDecimal hotScore;
    private String detailUrl;
    private Integer sortNo;
    private Integer sourceLimit;
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime syncTime;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }
    public BigDecimal getGroupPrice() { return groupPrice; }
    public void setGroupPrice(BigDecimal groupPrice) { this.groupPrice = groupPrice; }
    public BigDecimal getOriginPrice() { return originPrice; }
    public void setOriginPrice(BigDecimal originPrice) { this.originPrice = originPrice; }
    public Integer getPNum() { return pNum; }
    public void setPNum(Integer pNum) { this.pNum = pNum; }
    public Integer getMaxNum() { return maxNum; }
    public void setMaxNum(Integer maxNum) { this.maxNum = maxNum; }
    public Integer getGroupMinNum() { return groupMinNum; }
    public void setGroupMinNum(Integer groupMinNum) { this.groupMinNum = groupMinNum; }
    public Integer getGroupMaxNum() { return groupMaxNum; }
    public void setGroupMaxNum(Integer groupMaxNum) { this.groupMaxNum = groupMaxNum; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getCurrentNum() { return currentNum; }
    public void setCurrentNum(Integer currentNum) { this.currentNum = currentNum; }
    public BigDecimal getHotScore() { return hotScore; }
    public void setHotScore(BigDecimal hotScore) { this.hotScore = hotScore; }
    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Integer getSourceLimit() { return sourceLimit; }
    public void setSourceLimit(Integer sourceLimit) { this.sourceLimit = sourceLimit; }
    public LocalDateTime getSyncTime() { return syncTime; }
    public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }
}
