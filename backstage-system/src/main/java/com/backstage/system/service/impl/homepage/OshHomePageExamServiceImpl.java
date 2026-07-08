package com.backstage.system.service.impl.homepage;

import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.homepage.es.OshHomePageHotExamEsDocument;
import com.backstage.system.domain.homepage.vo.HotExamVO;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageExamMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotExamEsMapper;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.homepage.IOshHomePageExamService;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import com.backstage.system.service.homepage.es.AbstractOshHomePageEsCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OshHomePageExamServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotExamEsDocument, HotExamVO>
        implements IOshHomePageExamService {

    private static final String CARD_TYPE = "exam";
    private static final int COVER_SIGN_MINUTES = 1440;

    @Autowired private OshHomePageExamMapper homePageExamMapper;
    @Autowired private OshHomePageHotExamEsMapper hotEsMapper;
    @Autowired private OssService ossService;
    @Autowired private IOshHomePageModulePathService modulePathService;

    @Override public String cardType() { return CARD_TYPE; }
    @Override public List<HotExamVO> getHotExams(int limit) { return getCards(limit); }

    @Override
    public List<HotExamVO> getHotExamsFromMysql(int effectiveLimit) {
        return signCoverUrls(getHotExamsRawFromMysql(effectiveLimit));
    }

    @Override public int syncHotExamsToEs() { return syncToEs(); }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotExamEsDocument> esMapper() {
        return hotEsMapper;
    }

    @Override
    protected List<HotExamVO> queryFromMysql(int limit) {
        return getHotExamsFromMysql(limit);
    }

    @Override
    protected List<HotExamVO> queryFromMysqlForSync(int limit) {
        return getHotExamsRawFromMysql(limit);
    }

    @Override
    protected List<HotExamVO> toVos(List<OshHomePageHotExamEsDocument> docs) {
        List<HotExamVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotExamEsDocument doc : docs) {
            HotExamVO vo = new HotExamVO();
            vo.setId(doc.getId());
            vo.setTitle(doc.getTitle());
            vo.setCover(doc.getCover());
            vo.setDescription(doc.getDescription());
            vo.setTotalScore(doc.getTotalScore());
            vo.setPassScore(doc.getPassScore());
            vo.setExpire(doc.getExpire());
            vo.setQuestionCount(doc.getQuestionCount());
            vo.setCollectCount(doc.getCollectCount());
            vo.setHotScore(doc.getHotScore());
            vo.setTags(doc.getTags());
            vo.setDetailUrl(doc.getDetailUrl());
            result.add(vo);
        }
        return signCoverUrls(result);
    }

    @Override
    protected List<OshHomePageHotExamEsDocument> toDocs(List<HotExamVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotExamEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotExamVO vo = vos.get(i);
            OshHomePageHotExamEsDocument doc = new OshHomePageHotExamEsDocument();
            doc.setId(vo.getId());
            doc.setTitle(vo.getTitle());
            doc.setCover(vo.getCover());
            doc.setDescription(vo.getDescription());
            doc.setTotalScore(vo.getTotalScore());
            doc.setPassScore(vo.getPassScore());
            doc.setExpire(vo.getExpire());
            doc.setQuestionCount(vo.getQuestionCount());
            doc.setCollectCount(vo.getCollectCount());
            doc.setHotScore(vo.getHotScore());
            doc.setTags(vo.getTags());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }

    private List<HotExamVO> getHotExamsRawFromMysql(int effectiveLimit) {
        List<HotExamVO> exams = homePageExamMapper.selectHotExams(effectiveLimit);
        if (exams.isEmpty()) {
            return exams;
        }
        List<Long> ids = exams.stream().map(HotExamVO::getId).collect(Collectors.toList());
        List<HotExamVO> tagRels = homePageExamMapper.selectTagsByExamIds(ids);
        Map<Long, List<String>> tagMap = tagRels.stream().collect(Collectors.groupingBy(
                HotExamVO::getExamId, Collectors.mapping(HotExamVO::getTagName, Collectors.toList())));
        for (HotExamVO vo : exams) {
            List<String> tags = tagMap.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setTags(tags.subList(0, Math.min(2, tags.size())));
            vo.setDetailUrl(modulePathService.getListPath("exam"));
        }
        return exams;
    }

    private List<HotExamVO> signCoverUrls(List<HotExamVO> exams) {
        if (exams == null || exams.isEmpty()) {
            return exams;
        }
        for (HotExamVO vo : exams) {
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossService.getLimitedUrl(vo.getCover(), COVER_SIGN_MINUTES));
            }
        }
        return exams;
    }
}
