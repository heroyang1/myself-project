package com.backstage.system.service.questionanswer.impl;

import com.backstage.common.core.domain.R;
import com.backstage.common.enums.ResultCode;
import com.backstage.system.domain.questionanswer.Answer;
import com.backstage.system.domain.questionanswer.Question;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.questionanswer.OshQAAnswerMapper;
import com.backstage.system.mapper.questionanswer.OshQAQuestionMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.questionanswer.IOshQAAnswerService;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 问答回答 Service 实现
 */
@Service
public class OshQAAnswerServiceImpl implements IOshQAAnswerService {

    private static final Logger log = LoggerFactory.getLogger(OshQAAnswerServiceImpl.class);
    private static final byte QUESTION_STATUS_PUBLISHED = 1;
    private static final byte QUESTION_STATUS_ANSWERED = 2;
    private static final byte ANSWER_STATUS_NORMAL = 0;

    @Autowired
    private OshQAAnswerMapper oshQAAnswerMapper;

    @Autowired
    private OshQAQuestionMapper questionMapper;

    @Autowired
    private OshUserMapper userMapper;

    @Autowired
    private WebSocketNotifyService webSocketNotifyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> answer(Long userId, Long questionId, String content) {
        if (userId == null) {
            return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        }
        if (questionId == null || !StringUtils.hasText(content)) {
            return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        }
        Question question = questionMapper.selectOne(new LambdaQueryWrapper<Question>()
                .select(Question::getId, Question::getUserId, Question::getStatus)
                .eq(Question::getId, questionId)
                .eq(Question::getDeleteFlag, 0));
        if (question == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        if (question.getStatus() == null || question.getStatus() < QUESTION_STATUS_PUBLISHED) {
            return R.fail(ResultCode.FAILED_USER_PERMISSION_DENIED.getMsg());
        }

        // 1. 保存回答
        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setUserId(userId);
        answer.setContent(content.trim());
        answer.setStatus(ANSWER_STATUS_NORMAL);
        answer.setCreateBy(userId);
        answer.setUpdateBy(userId);
        oshQAAnswerMapper.insert(answer);
        if (Byte.valueOf(QUESTION_STATUS_PUBLISHED).equals(question.getStatus())) {
            Question update = new Question();
            update.setStatus(QUESTION_STATUS_ANSWERED);
            questionMapper.update(update, new LambdaQueryWrapper<Question>().eq(Question::getId, questionId));
        }

        // 2. 推送通知（异常不影响主流程）
        try {
            pushAnswerNotify(userId, question, content);
        } catch (Exception e) {
            log.error("问答回复通知推送失败，questionId={}, answererUserId={}, 原因={}",
                    questionId, userId, e.getMessage());
        }

        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> vote(Long userId, Long answerId) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (answerId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        Integer deleteFlag = oshQAAnswerMapper.getVoteInfoByUserIdAndAnswerId(userId, answerId);
        if (deleteFlag != null && deleteFlag == 0) {
            return R.fail(ResultCode.FAILED.getMsg());
        }
        Answer answer = oshQAAnswerMapper.selectOne(new LambdaQueryWrapper<Answer>()
                .select(Answer::getId)
                .eq(Answer::getId, answerId)
                .eq(Answer::getDeleteFlag, 0)
                .eq(Answer::getStatus, ANSWER_STATUS_NORMAL));
        if (answer == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        oshQAAnswerMapper.voteAnswer(userId, answerId, userId);
        oshQAAnswerMapper.incrementVoteCount(answerId);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> cancelVote(Long userId, Long answerId) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (answerId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        Integer deleteFlag = oshQAAnswerMapper.getVoteInfoByUserIdAndAnswerId(userId, answerId);
        if (deleteFlag != null && deleteFlag == 0) {
            Answer answer = oshQAAnswerMapper.selectOne(new LambdaQueryWrapper<Answer>()
                    .select(Answer::getId)
                    .eq(Answer::getId, answerId)
                    .eq(Answer::getDeleteFlag, 0)
                    .eq(Answer::getStatus, ANSWER_STATUS_NORMAL));
            if (answer == null) {
                return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
            }
            oshQAAnswerMapper.cancelVoteAnswer(userId, answerId, userId);
            oshQAAnswerMapper.decrementVoteCount(answerId);
            return R.ok(ResultCode.SUCCESS.getMsg());
        }
        return R.fail(ResultCode.FAILED.getMsg());
    }

    /**
     * 构建并推送新回复通知
     * 判断规则：
     *   1. 问题不存在 → 跳过
     *   2. 回答者 == 提问者（自问自答）→ 跳过
     */
    private void pushAnswerNotify(Long answererUserId, Question question, String content) {
        // 自问自答不推送
        if (answererUserId.equals(question.getUserId())) {
            return;
        }

        // 查询回答者昵称
        OshUser answerer = userMapper.selectById(answererUserId);
        String nickname = (answerer != null && answerer.getUsername() != null)
                ? answerer.getUsername() : "匿名用户";

        // 构建消息体，content 截取摘要
        WsNotifyMessage msg = new WsNotifyMessage();
        msg.setType("QA_NEW_ANSWER");
        msg.setTitle(nickname + " 回答了你的问题");
        msg.setContent(webSocketNotifyService.truncate(content));
        msg.setJumpUrl("/question_answer/detail/" + question.getId());
        msg.setBizId(question.getId().toString());

        webSocketNotifyService.send(question.getUserId(), msg);
    }
}
