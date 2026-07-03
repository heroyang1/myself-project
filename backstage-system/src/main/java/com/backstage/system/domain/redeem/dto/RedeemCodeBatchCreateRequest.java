package com.backstage.system.domain.redeem.dto;

import java.util.List;

public class RedeemCodeBatchCreateRequest {
    private List<RedeemCodeCreateRequest> items;

    public List<RedeemCodeCreateRequest> getItems() { return items; }
    public void setItems(List<RedeemCodeCreateRequest> items) { this.items = items; }
}
