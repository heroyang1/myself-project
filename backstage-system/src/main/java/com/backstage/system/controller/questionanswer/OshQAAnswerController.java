package com.backstage.system.controller.questionanswer;

import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.questionanswer.dto.AnswerDTO;
import com.backstage.system.domain.questionanswer.dto.AnswerVoteDTO;
import com.backstage.system.service.questionanswer.IOshQAAnswerService;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pc/qna/answer")
public class OshQAAnswerController {

    @Autowired
    private IOshQAAnswerService oshQAAnswerService;

    @ApiOperation("回答")
    @PostMapping("/createPost")
    @OshUserEvent(module = "答疑模块", actionType = "新增", description = "回答", resourceType = ResourceType.QA_ANSWER_TYPE)
    @OshUserLevel(value = 2)
    public R<String> answer(@RequestBody AnswerDTO answerDTO) {
        return oshQAAnswerService.answer(UserContextUtil.getCurrentUserId(),
                answerDTO == null ? null : answerDTO.getQuestionId(),
                answerDTO == null ? null : answerDTO.getContent());
    }

    @ApiOperation("给回答点赞")
    @PostMapping("/vote")
    @OshUserEvent(module = "答疑模块", actionType = "点赞", description = "给回答点赞", resourceType = ResourceType.QA_ANSWER_TYPE)
    @OshUserLevel(value = 2)
    public R<String> vote(@RequestBody AnswerVoteDTO answerVoteDTO) {
        return oshQAAnswerService.vote(UserContextUtil.getCurrentUserId(),
                answerVoteDTO == null ? null : answerVoteDTO.getAnswerId());
    }

    @ApiOperation("取消给回答点赞")
    @PostMapping("/vote/cancel")
    @OshUserEvent(module = "答疑模块", actionType = "取消点赞", description = "取消给回答点赞", resourceType = ResourceType.QA_ANSWER_TYPE)
    @OshUserLevel(value = 2)
    public R<String> cancelVote(@RequestBody AnswerVoteDTO answerVoteDTO) {
        return oshQAAnswerService.cancelVote(UserContextUtil.getCurrentUserId(),
                answerVoteDTO == null ? null : answerVoteDTO.getAnswerId());
    }
}
