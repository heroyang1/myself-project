package com.backstage.contract.internal.qa;

import java.io.Serializable;
import java.util.List;

/**
 * 内部工具提问请求
 *
 * <p>工具提问归属 course 域，映射到单体 {@code IOshQAQuestionService.addToolQuestion}。
 * tool-service 仅负责入口与工具校验，提问的存储与后续回答流程由 course 域承担。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class ToolQuestionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提问用户 ID（非空）
     */
    private Long userId;

    /**
     * 工具 ID（非空）
     */
    private Long toolId;

    /**
     * 提问内容（非空）
     */
    private String content;

    /**
     * 提问标签（可空）
     */
    private List<String> tags;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
