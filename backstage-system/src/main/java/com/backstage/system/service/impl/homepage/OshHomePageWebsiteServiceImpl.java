package com.backstage.system.service.impl.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotWebsiteEsDocument;
import com.backstage.system.domain.homepage.vo.HotWebsiteVO;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotWebsiteEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageWebsiteMapper;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import com.backstage.system.service.homepage.IOshHomePageWebsiteService;
import com.backstage.system.service.homepage.es.AbstractOshHomePageEsCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OshHomePageWebsiteServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotWebsiteEsDocument, HotWebsiteVO>
        implements IOshHomePageWebsiteService {

    private static final String CARD_TYPE = "website";

    @Autowired private OshHomePageWebsiteMapper homePageWebsiteMapper;
    @Autowired private OshHomePageHotWebsiteEsMapper hotEsMapper;
    @Autowired private IOshHomePageModulePathService modulePathService;

    @Override public String cardType() { return CARD_TYPE; }
    @Override public List<HotWebsiteVO> getHotWebsites(int limit) { return getCards(limit); }

    @Override
    public List<HotWebsiteVO> getHotWebsitesFromMysql(int effectiveLimit) {
        List<HotWebsiteVO> list = homePageWebsiteMapper.selectHotWebsites(effectiveLimit);
        for (HotWebsiteVO vo : list) {
            vo.setDetailUrl(modulePathService.getListPath("usefull"));
        }
        return list;
    }

    @Override public int syncHotWebsitesToEs() { return syncToEs(); }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotWebsiteEsDocument> esMapper() {
        return hotEsMapper;
    }

    @Override
    protected List<HotWebsiteVO> queryFromMysql(int limit) {
        return getHotWebsitesFromMysql(limit);
    }

    @Override
    protected List<HotWebsiteVO> toVos(List<OshHomePageHotWebsiteEsDocument> docs) {
        List<HotWebsiteVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotWebsiteEsDocument doc : docs) {
            HotWebsiteVO vo = new HotWebsiteVO();
            vo.setId(doc.getId());
            vo.setName(doc.getName());
            vo.setUrl(doc.getUrl());
            vo.setDescription(doc.getDescription());
            vo.setLogoUrl(doc.getLogoUrl());
            vo.setClickCount(doc.getClickCount());
            vo.setGoodCount(doc.getGoodCount());
            vo.setCollectionCount(doc.getCollectionCount());
            vo.setDetailUrl(doc.getDetailUrl());
            result.add(vo);
        }
        return result;
    }

    @Override
    protected List<OshHomePageHotWebsiteEsDocument> toDocs(List<HotWebsiteVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotWebsiteEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotWebsiteVO vo = vos.get(i);
            OshHomePageHotWebsiteEsDocument doc = new OshHomePageHotWebsiteEsDocument();
            doc.setId(vo.getId());
            doc.setName(vo.getName());
            doc.setUrl(vo.getUrl());
            doc.setDescription(vo.getDescription());
            doc.setLogoUrl(vo.getLogoUrl());
            doc.setClickCount(vo.getClickCount());
            doc.setGoodCount(vo.getGoodCount());
            doc.setCollectionCount(vo.getCollectionCount());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }
}
