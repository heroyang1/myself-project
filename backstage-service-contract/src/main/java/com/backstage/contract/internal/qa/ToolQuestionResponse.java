package com.backstage.contract.internal.qa;

import java.io.Serializable;

/**
 * 内部工具提问响应
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class ToolQuestionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新建提问 ID（字符串，与单体 addToolQuestion 返回值保持一致）
     */
    private String questionId;

    public ToolQuestionResponse() {
    }

    public ToolQuestionResponse(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }
}
