package com.backstage.system.service.impl.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotInfoGapEsDocument;
import com.backstage.system.domain.homepage.vo.HotInfoGapVO;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotInfoGapEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageInfoGapMapper;
import com.backstage.system.service.homepage.IOshHomePageInfoGapService;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import com.backstage.system.service.homepage.es.AbstractOshHomePageEsCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OshHomePageInfoGapServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotInfoGapEsDocument, HotInfoGapVO>
        implements IOshHomePageInfoGapService {

    private static final String CARD_TYPE = "infoGap";

    @Autowired private OshHomePageInfoGapMapper homePageInfoGapMapper;
    @Autowired private OshHomePageHotInfoGapEsMapper hotEsMapper;
    @Autowired private IOshHomePageModulePathService modulePathService;

    @Override public String cardType() { return CARD_TYPE; }
    @Override public List<HotInfoGapVO> getHotInfoGap(int limit) { return getCards(limit); }

    @Override
    public List<HotInfoGapVO> getHotInfoGapFromMysql(int effectiveLimit) {
        List<HotInfoGapVO> list = homePageInfoGapMapper.selectHotInfoGap(effectiveLimit);
        for (HotInfoGapVO vo : list) {
            vo.setDetailUrl(modulePathService.getDetailPath("info_gap", vo.getId()));
        }
        return list;
    }

    @Override public int syncHotInfoGapToEs() { return syncToEs(); }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotInfoGapEsDocument> esMapper() {
        return hotEsMapper;
    }

    @Override
    protected List<HotInfoGapVO> queryFromMysql(int limit) {
        return getHotInfoGapFromMysql(limit);
    }

    @Override
    protected List<HotInfoGapVO> toVos(List<OshHomePageHotInfoGapEsDocument> docs) {
        List<HotInfoGapVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotInfoGapEsDocument doc : docs) {
            HotInfoGapVO vo = new HotInfoGapVO();
            vo.setId(doc.getId());
            vo.setTitle(doc.getTitle());
            vo.setTag(doc.getTag());
            vo.setContent(doc.getContent());
            vo.setGoodCount(doc.getGoodCount());
            vo.setMiddleCount(doc.getMiddleCount());
            vo.setBadCount(doc.getBadCount());
            vo.setUserName(doc.getUserName());
            vo.setDetailUrl(doc.getDetailUrl());
            result.add(vo);
        }
        return result;
    }

    @Override
    protected List<OshHomePageHotInfoGapEsDocument> toDocs(List<HotInfoGapVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotInfoGapEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotInfoGapVO vo = vos.get(i);
            OshHomePageHotInfoGapEsDocument doc = new OshHomePageHotInfoGapEsDocument();
            doc.setId(vo.getId());
            doc.setTitle(vo.getTitle());
            doc.setTag(vo.getTag());
            doc.setContent(vo.getContent());
            doc.setGoodCount(vo.getGoodCount());
            doc.setMiddleCount(vo.getMiddleCount());
            doc.setBadCount(vo.getBadCount());
            doc.setUserName(vo.getUserName());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }
}
