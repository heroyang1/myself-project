package com.backstage.system.service.impl.homepage;

import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.homepage.es.OshHomePageHotBookEsDocument;
import com.backstage.system.domain.homepage.vo.HotBookVO;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageBookMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotBookEsMapper;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.homepage.IOshHomePageBookService;
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
public class OshHomePageBookServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotBookEsDocument, HotBookVO>
        implements IOshHomePageBookService {

    private static final String CARD_TYPE = "book";
    private static final int COVER_SIGN_MINUTES = 1440;

    @Autowired private OshHomePageBookMapper homePageBookMapper;
    @Autowired private OshHomePageHotBookEsMapper hotEsMapper;
    @Autowired private OssService ossService;
    @Autowired private IOshHomePageModulePathService modulePathService;

    @Override public String cardType() { return CARD_TYPE; }
    @Override public List<HotBookVO> getHotBooks(int limit) { return getCards(limit); }

    @Override
    public List<HotBookVO> getHotBooksFromMysql(int effectiveLimit) {
        return signCoverUrls(getHotBooksRawFromMysql(effectiveLimit));
    }

    @Override public int syncHotBooksToEs() { return syncToEs(); }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotBookEsDocument> esMapper() {
        return hotEsMapper;
    }

    @Override
    protected List<HotBookVO> queryFromMysql(int limit) {
        return getHotBooksFromMysql(limit);
    }

    @Override
    protected List<HotBookVO> queryFromMysqlForSync(int limit) {
        return getHotBooksRawFromMysql(limit);
    }

    @Override
    protected List<HotBookVO> toVos(List<OshHomePageHotBookEsDocument> docs) {
        List<HotBookVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotBookEsDocument doc : docs) {
            HotBookVO vo = new HotBookVO();
            vo.setId(doc.getId());
            vo.setTitle(doc.getTitle());
            vo.setCover(doc.getCover());
            vo.setDescription(doc.getDescription());
            vo.setPrice(doc.getPrice());
            vo.setSubCount(doc.getSubCount());
            vo.setLevel(doc.getLevel());
            vo.setChapterCount(doc.getChapterCount());
            vo.setHotScore(doc.getHotScore());
            vo.setDetailUrl(doc.getDetailUrl());
            vo.setTags(doc.getTags());
            result.add(vo);
        }
        return signCoverUrls(result);
    }

    @Override
    protected List<OshHomePageHotBookEsDocument> toDocs(List<HotBookVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotBookEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotBookVO vo = vos.get(i);
            OshHomePageHotBookEsDocument doc = new OshHomePageHotBookEsDocument();
            doc.setId(vo.getId());
            doc.setTitle(vo.getTitle());
            doc.setCover(vo.getCover());
            doc.setDescription(vo.getDescription());
            doc.setPrice(vo.getPrice());
            doc.setSubCount(vo.getSubCount());
            doc.setLevel(vo.getLevel());
            doc.setChapterCount(vo.getChapterCount());
            doc.setHotScore(vo.getHotScore());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setTags(vo.getTags());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }

    private List<HotBookVO> getHotBooksRawFromMysql(int effectiveLimit) {
        List<HotBookVO> books = homePageBookMapper.selectHotBooks(effectiveLimit);
        if (books.isEmpty()) {
            return books;
        }
        List<Long> ids = books.stream().map(HotBookVO::getId).collect(Collectors.toList());
        List<HotBookVO> tagRels = homePageBookMapper.selectTagsByBookIds(ids);
        Map<Long, List<String>> tagMap = tagRels.stream().collect(Collectors.groupingBy(
                HotBookVO::getBookId, Collectors.mapping(HotBookVO::getTagName, Collectors.toList())));
        for (HotBookVO vo : books) {
            List<String> tags = tagMap.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setTags(tags.subList(0, Math.min(2, tags.size())));
            vo.setDetailUrl(modulePathService.getDetailPath("book", vo.getId()));
        }
        return books;
    }

    private List<HotBookVO> signCoverUrls(List<HotBookVO> books) {
        if (books == null || books.isEmpty()) {
            return books;
        }
        for (HotBookVO vo : books) {
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossService.getLimitedUrl(vo.getCover(), COVER_SIGN_MINUTES));
            }
        }
        return books;
    }
}
