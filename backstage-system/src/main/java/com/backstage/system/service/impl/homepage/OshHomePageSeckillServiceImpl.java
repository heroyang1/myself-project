package com.backstage.system.service.impl.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotSeckillEsDocument;
import com.backstage.system.domain.homepage.vo.HotSeckillVO;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotSeckillEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageSeckillMapper;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import com.backstage.system.service.homepage.IOshHomePageSeckillService;
import com.backstage.system.service.homepage.es.AbstractOshHomePageEsCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OshHomePageSeckillServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotSeckillEsDocument, HotSeckillVO>
        implements IOshHomePageSeckillService {

    private static final String CARD_TYPE = "seckill";

    @Autowired private OshHomePageSeckillMapper homePageSeckillMapper;
    @Autowired private OshHomePageHotSeckillEsMapper hotEsMapper;
    @Autowired private IOshHomePageModulePathService modulePathService;

    @Override public String cardType() { return CARD_TYPE; }
    @Override public List<HotSeckillVO> getHotSeckill(int limit) { return getCards(limit); }

    @Override
    public List<HotSeckillVO> getHotSeckillFromMysql(int effectiveLimit) {
        List<HotSeckillVO> list = homePageSeckillMapper.selectHotSeckill(effectiveLimit);
        for (HotSeckillVO vo : list) {
            if (vo.getActivityId() != null) {
                vo.setDetailUrl(modulePathService.getDetailPath("seckill", vo.getActivityId()));
            } else {
                vo.setDetailUrl(modulePathService.getListPath("seckill"));
            }
        }
        return list;
    }

    @Override public int syncHotSeckillToEs() { return syncToEs(); }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotSeckillEsDocument> esMapper() {
        return hotEsMapper;
    }

    @Override
    protected List<HotSeckillVO> queryFromMysql(int limit) {
        return getHotSeckillFromMysql(limit);
    }

    @Override
    protected List<HotSeckillVO> toVos(List<OshHomePageHotSeckillEsDocument> docs) {
        List<HotSeckillVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotSeckillEsDocument doc : docs) {
            HotSeckillVO vo = new HotSeckillVO();
            vo.setActivityId(doc.getActivityId());
            vo.setItemId(doc.getItemId());
            vo.setGoodsId(doc.getGoodsId());
            vo.setGoodsType(doc.getGoodsType());
            vo.setGoodsName(doc.getGoodsName());
            vo.setGoodsCover(doc.getGoodsCover());
            vo.setOriginPrice(doc.getOriginPrice());
            vo.setSeckillPrice(doc.getSeckillPrice());
            vo.setStock(doc.getStock());
            vo.setSoldCount(doc.getSoldCount());
            vo.setLimitPerUser(doc.getLimitPerUser());
            vo.setDetailUrl(doc.getDetailUrl());
            result.add(vo);
        }
        return result;
    }

    @Override
    protected List<OshHomePageHotSeckillEsDocument> toDocs(List<HotSeckillVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotSeckillEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotSeckillVO vo = vos.get(i);
            OshHomePageHotSeckillEsDocument doc = new OshHomePageHotSeckillEsDocument();
            doc.setActivityId(vo.getActivityId());
            doc.setItemId(vo.getItemId());
            doc.setGoodsId(vo.getGoodsId());
            doc.setGoodsType(vo.getGoodsType());
            doc.setGoodsName(vo.getGoodsName());
            doc.setGoodsCover(vo.getGoodsCover());
            doc.setOriginPrice(vo.getOriginPrice());
            doc.setSeckillPrice(vo.getSeckillPrice());
            doc.setStock(vo.getStock());
            doc.setSoldCount(vo.getSoldCount());
            doc.setLimitPerUser(vo.getLimitPerUser());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }
}
