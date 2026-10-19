package com.backstage.system.domain.homepage.es;

import com.backstage.system.jackson.FlexibleLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 首页热门工具 ES 文档
 *
 * @author jayTatum
 */
public class OshHomePageHotToolEsDocument {

    private Long id;
    private String toolName;
    private String description;
    private String resourceType;
    private BigDecimal price;
    private Integer pointCost;
    private Integer payType;
    private Long totalUsage;
    private String detailUrl;

    private Integer sortNo;
    private Integer sourceLimit;

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime syncTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getPointCost() { return pointCost; }
    public void setPointCost(Integer pointCost) { this.pointCost = pointCost; }

    public Integer getPayType() { return payType; }
    public void setPayType(Integer payType) { this.payType = payType; }

    public Long getTotalUsage() { return totalUsage; }
    public void setTotalUsage(Long totalUsage) { this.totalUsage = totalUsage; }

    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }

    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }

    public Integer getSourceLimit() { return sourceLimit; }
    public void setSourceLimit(Integer sourceLimit) { this.sourceLimit = sourceLimit; }

    public LocalDateTime getSyncTime() { return syncTime; }
    public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }
}
