package com.backstage.system.service.impl.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotGroupEsDocument;
import com.backstage.system.domain.homepage.vo.HotGroupVO;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageGroupMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotGroupEsMapper;
import com.backstage.system.service.homepage.IOshHomePageGroupService;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import com.backstage.system.service.homepage.es.AbstractOshHomePageEsCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OshHomePageGroupServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotGroupEsDocument, HotGroupVO>
        implements IOshHomePageGroupService {

    private static final String CARD_TYPE = "group";

    @Autowired private OshHomePageGroupMapper homePageGroupMapper;
    @Autowired private OshHomePageHotGroupEsMapper hotEsMapper;
    @Autowired private IOshHomePageModulePathService modulePathService;

    @Override public String cardType() { return CARD_TYPE; }
    @Override public List<HotGroupVO> getHotGroup(int limit) { return getCards(limit); }

    @Override
    public List<HotGroupVO> getHotGroupFromMysql(int effectiveLimit) {
        List<HotGroupVO> list = homePageGroupMapper.selectHotGroup(effectiveLimit);
        for (HotGroupVO vo : list) {
            if (vo.getGroupId() != null) {
                vo.setDetailUrl("/group/work/" + vo.getGroupId());
            } else {
                vo.setDetailUrl(modulePathService.getListPath("group"));
            }
        }
        return list;
    }

    @Override public int syncHotGroupToEs() { return syncToEs(); }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotGroupEsDocument> esMapper() {
        return hotEsMapper;
    }

    @Override
    protected List<HotGroupVO> queryFromMysql(int limit) {
        return getHotGroupFromMysql(limit);
    }

    @Override
    protected List<HotGroupVO> toVos(List<OshHomePageHotGroupEsDocument> docs) {
        List<HotGroupVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotGroupEsDocument doc : docs) {
            HotGroupVO vo = new HotGroupVO();
            vo.setGroupId(doc.getGroupId());
            vo.setGoodsId(doc.getGoodsId());
            vo.setType(doc.getType());
            vo.setTitle(doc.getTitle());
            vo.setCover(doc.getCover());
            vo.setGroupPrice(doc.getGroupPrice());
            vo.setOriginPrice(doc.getOriginPrice());
            vo.setpNum(doc.getPNum());
            vo.setMaxNum(doc.getMaxNum());
            vo.setGroupMinNum(doc.getGroupMinNum());
            vo.setGroupMaxNum(doc.getGroupMaxNum());
            vo.setStartTime(doc.getStartTime());
            vo.setEndTime(doc.getEndTime());
            vo.setDescription(doc.getDescription());
            vo.setCurrentNum(doc.getCurrentNum());
            vo.setHotScore(doc.getHotScore());
            vo.setDetailUrl(doc.getDetailUrl());
            result.add(vo);
        }
        return result;
    }

    @Override
    protected List<OshHomePageHotGroupEsDocument> toDocs(List<HotGroupVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotGroupEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotGroupVO vo = vos.get(i);
            OshHomePageHotGroupEsDocument doc = new OshHomePageHotGroupEsDocument();
            doc.setGroupId(vo.getGroupId());
            doc.setGoodsId(vo.getGoodsId());
            doc.setType(vo.getType());
            doc.setTitle(vo.getTitle());
            doc.setCover(vo.getCover());
            doc.setGroupPrice(vo.getGroupPrice());
            doc.setOriginPrice(vo.getOriginPrice());
            doc.setPNum(vo.getpNum());
            doc.setMaxNum(vo.getMaxNum());
            doc.setGroupMinNum(vo.getGroupMinNum());
            doc.setGroupMaxNum(vo.getGroupMaxNum());
            doc.setStartTime(vo.getStartTime());
            doc.setEndTime(vo.getEndTime());
            doc.setDescription(vo.getDescription());
            doc.setCurrentNum(vo.getCurrentNum());
            doc.setHotScore(vo.getHotScore());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }
}
