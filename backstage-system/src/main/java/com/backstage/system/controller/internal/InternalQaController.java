package com.backstage.system.controller.internal;

import com.backstage.common.core.domain.R;
import com.backstage.contract.common.InternalApiConstants;
import com.backstage.contract.common.InternalResult;
import com.backstage.contract.internal.qa.ToolQuestionRequest;
import com.backstage.contract.internal.qa.ToolQuestionResponse;
import com.backstage.system.service.questionanswer.IOshQAQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部课程问答接口
 *
 * <p>工具提问归属 course 域。tool-service 仅负责入口与工具校验，通过本端点将提问委托给 course 域存储，
 * 保证"一表一主"（提问表由 course 域独享写权限）。委托单体现有 IOshQAQuestionService。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
@RestController
@RequestMapping(InternalApiConstants.QA_BASE)
public class InternalQaController {

    @Autowired
    private IOshQAQuestionService oshQAQuestionService;

    /**
     * 新建工具提问
     *
     * @param request 提问请求（用户、工具、内容、标签）
     * @return 新建提问 ID
     */
    @PostMapping("/tool-question")
    public InternalResult<ToolQuestionResponse> toolQuestion(@RequestBody ToolQuestionRequest request) {
        R<String> result = oshQAQuestionService.addToolQuestion(
                request.getUserId(), request.getToolId(), request.getContent(), request.getTags());
        // 【结果透传】course 域以 R 返回业务结果，成功则映射提问 ID，失败则原样回传状态码与提示
        if (R.isSuccess(result)) {
            return InternalResult.ok(new ToolQuestionResponse(result.getData()));
        }
        return InternalResult.fail(result.getCode(), result.getMsg());
    }
}
