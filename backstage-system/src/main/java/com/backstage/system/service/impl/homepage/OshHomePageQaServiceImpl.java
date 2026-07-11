package com.backstage.system.service.impl.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotQaEsDocument;
import com.backstage.system.domain.homepage.vo.HotQaVO;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotQaEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageQaMapper;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import com.backstage.system.service.homepage.IOshHomePageQaService;
import com.backstage.system.service.homepage.es.AbstractOshHomePageEsCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OshHomePageQaServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotQaEsDocument, HotQaVO>
        implements IOshHomePageQaService {

    private static final String CARD_TYPE = "qa";

    @Autowired private OshHomePageQaMapper homePageQaMapper;
    @Autowired private OshHomePageHotQaEsMapper hotEsMapper;
    @Autowired private IOshHomePageModulePathService modulePathService;

    @Override public String cardType() { return CARD_TYPE; }
    @Override public List<HotQaVO> getHotQa(int limit) { return getCards(limit); }

    @Override
    public List<HotQaVO> getHotQaFromMysql(int effectiveLimit) {
        List<HotQaVO> list = homePageQaMapper.selectHotQa(effectiveLimit);
        for (HotQaVO vo : list) {
            vo.setDetailUrl(modulePathService.getDetailPath("qa", vo.getId()));
        }
        return list;
    }

    @Override public int syncHotQaToEs() { return syncToEs(); }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotQaEsDocument> esMapper() {
        return hotEsMapper;
    }

    @Override
    protected List<HotQaVO> queryFromMysql(int limit) {
        return getHotQaFromMysql(limit);
    }

    @Override
    protected List<HotQaVO> toVos(List<OshHomePageHotQaEsDocument> docs) {
        List<HotQaVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotQaEsDocument doc : docs) {
            HotQaVO vo = new HotQaVO();
            vo.setId(doc.getId());
            vo.setContent(doc.getContent());
            vo.setResourceType(doc.getResourceType());
            vo.setStatus(doc.getStatus());
            vo.setViewCount(doc.getViewCount());
            vo.setFollowCount(doc.getFollowCount());
            vo.setAnswerCount(doc.getAnswerCount());
            vo.setUser(doc.getUser());
            vo.setTime(doc.getTime());
            vo.setDetailUrl(doc.getDetailUrl());
            result.add(vo);
        }
        return result;
    }

    @Override
    protected List<OshHomePageHotQaEsDocument> toDocs(List<HotQaVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotQaEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotQaVO vo = vos.get(i);
            OshHomePageHotQaEsDocument doc = new OshHomePageHotQaEsDocument();
            doc.setId(vo.getId());
            doc.setContent(vo.getContent());
            doc.setResourceType(vo.getResourceType());
            doc.setStatus(vo.getStatus());
            doc.setViewCount(vo.getViewCount());
            doc.setFollowCount(vo.getFollowCount());
            doc.setAnswerCount(vo.getAnswerCount());
            doc.setUser(vo.getUser());
            doc.setTime(vo.getTime());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }
}
