package com.backstage.system.service.impl.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotFeedbackEsDocument;
import com.backstage.system.domain.homepage.vo.HotFeedbackVO;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageFeedbackMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotFeedbackEsMapper;
import com.backstage.system.service.homepage.IOshHomePageFeedbackService;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import com.backstage.system.service.homepage.es.AbstractOshHomePageEsCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OshHomePageFeedbackServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotFeedbackEsDocument, HotFeedbackVO>
        implements IOshHomePageFeedbackService {

    private static final String CARD_TYPE = "feedback";

    @Autowired private OshHomePageFeedbackMapper homePageFeedbackMapper;
    @Autowired private OshHomePageHotFeedbackEsMapper hotFeedbackEsMapper;
    @Autowired private IOshHomePageModulePathService modulePathService;

    @Override
    public String cardType() {
        return CARD_TYPE;
    }

    @Override
    public List<HotFeedbackVO> getHotFeedback(int limit) {
        return getCards(limit);
    }

    @Override
    public List<HotFeedbackVO> getHotFeedbackFromMysql(int effectiveLimit) {
        List<HotFeedbackVO> list = homePageFeedbackMapper.selectHotFeedback(effectiveLimit);
        for (HotFeedbackVO vo : list) {
            vo.setDetailUrl(modulePathService.getDetailPath("feedback", vo.getId()));
        }
        return list;
    }

    @Override
    public int syncHotFeedbackToEs() {
        return syncToEs();
    }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotFeedbackEsDocument> esMapper() {
        return hotFeedbackEsMapper;
    }

    @Override
    protected List<HotFeedbackVO> queryFromMysql(int limit) {
        return getHotFeedbackFromMysql(limit);
    }

    @Override
    protected List<HotFeedbackVO> toVos(List<OshHomePageHotFeedbackEsDocument> docs) {
        List<HotFeedbackVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotFeedbackEsDocument doc : docs) {
            HotFeedbackVO vo = new HotFeedbackVO();
            vo.setId(doc.getId());
            vo.setTitle(doc.getTitle());
            vo.setSummary(doc.getSummary());
            vo.setCategory(doc.getCategory());
            vo.setCategoryIcon(doc.getCategoryIcon());
            vo.setStatus(doc.getStatus());
            vo.setUsername(doc.getUsername());
            vo.setTagName(doc.getTagName());
            vo.setLikeCount(doc.getLikeCount());
            vo.setCommentCount(doc.getCommentCount());
            vo.setViewCount(doc.getViewCount());
            vo.setCollectCount(doc.getCollectCount());
            vo.setHotScore(doc.getHotScore());
            vo.setCreateTime(doc.getCreateTime());
            vo.setDetailUrl(doc.getDetailUrl());
            result.add(vo);
        }
        return result;
    }

    @Override
    protected List<OshHomePageHotFeedbackEsDocument> toDocs(List<HotFeedbackVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotFeedbackEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotFeedbackVO vo = vos.get(i);
            OshHomePageHotFeedbackEsDocument doc = new OshHomePageHotFeedbackEsDocument();
            doc.setId(vo.getId());
            doc.setTitle(vo.getTitle());
            doc.setSummary(vo.getSummary());
            doc.setCategory(vo.getCategory());
            doc.setCategoryIcon(vo.getCategoryIcon());
            doc.setStatus(vo.getStatus());
            doc.setUsername(vo.getUsername());
            doc.setTagName(vo.getTagName());
            doc.setLikeCount(vo.getLikeCount());
            doc.setCommentCount(vo.getCommentCount());
            doc.setViewCount(vo.getViewCount());
            doc.setCollectCount(vo.getCollectCount());
            doc.setHotScore(vo.getHotScore());
            doc.setCreateTime(vo.getCreateTime());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }
}
