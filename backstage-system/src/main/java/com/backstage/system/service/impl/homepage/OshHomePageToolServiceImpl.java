package com.backstage.system.service.impl.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotToolEsDocument;
import com.backstage.system.domain.homepage.vo.HotToolVO;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotToolEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageToolMapper;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import com.backstage.system.service.homepage.IOshHomePageToolService;
import com.backstage.system.service.homepage.es.AbstractOshHomePageEsCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OshHomePageToolServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotToolEsDocument, HotToolVO>
        implements IOshHomePageToolService {

    private static final String CARD_TYPE = "tool";

    @Autowired private OshHomePageToolMapper homePageToolMapper;
    @Autowired private OshHomePageHotToolEsMapper hotToolEsMapper;
    @Autowired private IOshHomePageModulePathService modulePathService;

    @Override public String cardType() { return CARD_TYPE; }
    @Override public List<HotToolVO> getHotTools(int limit) { return getCards(limit); }

    @Override
    public List<HotToolVO> getHotToolsFromMysql(int effectiveLimit) {
        List<HotToolVO> list = homePageToolMapper.selectHotTools(effectiveLimit);
        for (HotToolVO vo : list) {
            vo.setDetailUrl(modulePathService.getDetailPath("tool", vo.getId()));
        }
        return list;
    }

    @Override public int syncHotToolsToEs() { return syncToEs(); }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotToolEsDocument> esMapper() {
        return hotToolEsMapper;
    }

    @Override
    protected List<HotToolVO> queryFromMysql(int limit) {
        return getHotToolsFromMysql(limit);
    }

    @Override
    protected List<HotToolVO> toVos(List<OshHomePageHotToolEsDocument> docs) {
        List<HotToolVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotToolEsDocument doc : docs) {
            HotToolVO vo = new HotToolVO();
            vo.setId(doc.getId());
            vo.setToolName(doc.getToolName());
            vo.setDescription(doc.getDescription());
            vo.setResourceType(doc.getResourceType());
            vo.setPrice(doc.getPrice());
            vo.setPointCost(doc.getPointCost());
            vo.setPayType(doc.getPayType());
            vo.setTotalUsage(doc.getTotalUsage());
            vo.setDetailUrl(doc.getDetailUrl());
            result.add(vo);
        }
        return result;
    }

    @Override
    protected List<OshHomePageHotToolEsDocument> toDocs(List<HotToolVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotToolEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotToolVO vo = vos.get(i);
            OshHomePageHotToolEsDocument doc = new OshHomePageHotToolEsDocument();
            doc.setId(vo.getId());
            doc.setToolName(vo.getToolName());
            doc.setDescription(vo.getDescription());
            doc.setResourceType(vo.getResourceType());
            doc.setPrice(vo.getPrice());
            doc.setPointCost(vo.getPointCost());
            doc.setPayType(vo.getPayType());
            doc.setTotalUsage(vo.getTotalUsage());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }
}
