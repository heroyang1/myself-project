package com.backstage.system.domain.internalmember.vo;

public class InternalMemberEmploymentDocumentVO {
    private Long userId;
    private String resumeFileKey;
    private String resumeFileName;
    private String offerFileKey;
    private String offerFileName;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getResumeFileKey() {
        return resumeFileKey;
    }

    public void setResumeFileKey(String resumeFileKey) {
        this.resumeFileKey = resumeFileKey;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public void setResumeFileName(String resumeFileName) {
        this.resumeFileName = resumeFileName;
    }

    public String getOfferFileKey() {
        return offerFileKey;
    }

    public void setOfferFileKey(String offerFileKey) {
        this.offerFileKey = offerFileKey;
    }

    public String getOfferFileName() {
        return offerFileName;
    }

    public void setOfferFileName(String offerFileName) {
        this.offerFileName = offerFileName;
    }
}
