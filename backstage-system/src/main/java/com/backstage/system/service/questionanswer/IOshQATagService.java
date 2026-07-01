package com.backstage.system.service.questionanswer;

import com.backstage.common.core.domain.R;
import com.backstage.system.domain.questionanswer.vo.QATagVO;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/4/1
 * Time: 15:37
 */
public interface IOshQATagService {
    R<List<QATagVO>> searchTags(String type);

    Long resolveTagId(String tagName, Long userId);

    void bindQuestionTags(Long questionId, List<String> tags, Long userId);

    void replaceQuestionTags(Long questionId, List<String> tags, Long userId);

    List<String> listQuestionTagNames(Long questionId);
}
