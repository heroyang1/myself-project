package com.backstage.system.service.questionanswer;

import com.backstage.common.core.domain.R;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/4/2
 * Time: 19:59
 */
public interface IOshQAAnswerService {
    R<String> answer(Long userId, Long questionId, String content);

    R<String> vote(Long userId, Long answerId);

    R<String> cancelVote(Long userId, Long answerId);
}
