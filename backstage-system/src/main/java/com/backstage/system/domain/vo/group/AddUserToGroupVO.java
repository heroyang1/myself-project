package com.backstage.system.domain.vo.group;

/**
 * 手动添加用户到拼团响应VO
 * 
 * @author system
 * @date 2026-07-09
 */
public class AddUserToGroupVO {

    /** 是否成功 */
    private Boolean success;

    /** 拼团活动ID */
    private Long activityId;

    /** 用户ID */
    private Long userId;

    /** 当前人数 */
    private Integer currentNum;

    /** 最大人数 */
    private Integer maxNum;

    /** 是否已成团 */
    private Boolean isSuccess;

    /** 提示消息 */
    private String message;

    /** 备注 */
    private String remark;

    public AddUserToGroupVO() {}

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getCurrentNum() { return currentNum; }
    public void setCurrentNum(Integer currentNum) { this.currentNum = currentNum; }

    public Integer getMaxNum() { return maxNum; }
    public void setMaxNum(Integer maxNum) { this.maxNum = maxNum; }

    public Boolean getIsSuccess() { return isSuccess; }
    public void setIsSuccess(Boolean isSuccess) { this.isSuccess = isSuccess; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
