package com.backstage.system.service.questionanswer.impl;

import cn.hutool.core.bean.BeanUtil;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.questionanswer.Tag;
import com.backstage.system.domain.questionanswer.vo.QATagVO;
import com.backstage.system.mapper.questionanswer.OshQATagMapper;
import com.backstage.system.service.questionanswer.IOshQATagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/4/1
 * Time: 15:37
 */
@Service
public class OshQATagServiceImpl implements IOshQATagService {

    @Autowired
    private OshQATagMapper oshQaMapper;

    @Override
    public R<List<QATagVO>> searchTags(String type) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>().select(Tag::getId, Tag::getName)
                .eq(StringUtils.hasText(type), Tag::getType, type)
                .orderByDesc(Tag::getUseCount);
        List<Tag> tags = oshQaMapper.selectList(wrapper);
        List<QATagVO> QATagVOS = new ArrayList<>();
        for (Tag tag : tags) {
            QATagVO QATagVO = new QATagVO();
            BeanUtil.copyProperties(tag, QATagVO);
            QATagVOS.add(QATagVO);
        }
        return R.ok(QATagVOS);
    }

    @Override
    public Long resolveTagId(String tagName, Long userId) {
        if (!StringUtils.hasText(tagName)) {
            return null;
        }
        String name = tagName.trim();
        Tag existing = oshQaMapper.selectTagByName(name);
        if (existing != null) {
            return existing.getId();
        }
        Tag tag = new Tag();
        tag.setName(name);
        tag.setType("custom");
        tag.setUseCount(0);
        try {
            oshQaMapper.insert(tag);
            return tag.getId();
        } catch (DuplicateKeyException ex) {
            Tag retry = oshQaMapper.selectTagByName(name);
            return retry != null ? retry.getId() : null;
        }
    }

    @Override
    public void bindQuestionTags(Long questionId, List<String> tags, Long userId) {
        if (questionId == null || tags == null || tags.isEmpty()) {
            return;
        }
        for (String tagName : tags) {
            Long tagId = resolveTagId(tagName, userId);
            if (tagId != null) {
                oshQaMapper.addQuestionTags(questionId, tagId, userId);
            }
        }
    }

    @Override
    public void replaceQuestionTags(Long questionId, List<String> tags, Long userId) {
        if (questionId == null) {
            return;
        }
        oshQaMapper.deleteQuestionTags(questionId);
        bindQuestionTags(questionId, tags, userId);
    }

    @Override
    public List<String> listQuestionTagNames(Long questionId) {
        List<Long> tagIds = oshQaMapper.selectTagIdsByQuestionId(questionId);
        if (CollectionUtils.isEmpty(tagIds)) {
            return null;
        }
        List<Tag> tags = oshQaMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getId, tagIds));
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<String> tagNames = new ArrayList<>();
        for (Tag tag : tags) {
            tagNames.add(tag.getName());
        }
        return tagNames;
    }
}
