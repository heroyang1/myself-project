package com.backstage.system.service.impl.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotOpenProjectEsDocument;
import com.backstage.system.domain.homepage.vo.HotOpenProjectVO;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotOpenProjectEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageOpenProjectMapper;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import com.backstage.system.service.homepage.IOshHomePageOpenProjectService;
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
public class OshHomePageOpenProjectServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotOpenProjectEsDocument, HotOpenProjectVO>
        implements IOshHomePageOpenProjectService {

    private static final String CARD_TYPE = "openProject";

    @Autowired private OshHomePageOpenProjectMapper homePageOpenProjectMapper;
    @Autowired private OshHomePageHotOpenProjectEsMapper hotEsMapper;
    @Autowired private IOshHomePageModulePathService modulePathService;

    @Override public String cardType() { return CARD_TYPE; }
    @Override public List<HotOpenProjectVO> getHotOpenProjects(int limit) { return getCards(limit); }

    @Override
    public List<HotOpenProjectVO> getHotOpenProjectsFromMysql(int effectiveLimit) {
        List<HotOpenProjectVO> projects = homePageOpenProjectMapper.selectHotOpenProjects(effectiveLimit);
        if (projects.isEmpty()) {
            return projects;
        }

        List<Long> ids = projects.stream().map(HotOpenProjectVO::getId).collect(Collectors.toList());
        List<HotOpenProjectVO> tagRels = homePageOpenProjectMapper.selectTagsByProjectIds(ids);
        Map<Long, List<String>> tagMap = tagRels.stream().collect(Collectors.groupingBy(
                HotOpenProjectVO::getProjectId,
                Collectors.mapping(HotOpenProjectVO::getTagName, Collectors.toList())));

        for (HotOpenProjectVO vo : projects) {
            List<String> tags = tagMap.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setTagList(tags.subList(0, Math.min(3, tags.size())));
            vo.setDetailUrl(modulePathService.getListPath("openproject"));
        }
        return projects;
    }

    @Override public int syncHotOpenProjectsToEs() { return syncToEs(); }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotOpenProjectEsDocument> esMapper() {
        return hotEsMapper;
    }

    @Override
    protected List<HotOpenProjectVO> queryFromMysql(int limit) {
        return getHotOpenProjectsFromMysql(limit);
    }

    @Override
    protected List<HotOpenProjectVO> toVos(List<OshHomePageHotOpenProjectEsDocument> docs) {
        List<HotOpenProjectVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotOpenProjectEsDocument doc : docs) {
            HotOpenProjectVO vo = new HotOpenProjectVO();
            vo.setId(doc.getId());
            vo.setProjectName(doc.getProjectName());
            vo.setProjectDesc(doc.getProjectDesc());
            vo.setProjectUrl(doc.getProjectUrl());
            vo.setAuthorName(doc.getAuthorName());
            vo.setProjectCover(doc.getProjectCover());
            vo.setStarCount(doc.getStarCount());
            vo.setForkCount(doc.getForkCount());
            vo.setLastCommit(doc.getLastCommit());
            vo.setTagList(doc.getTagList());
            vo.setDetailUrl(doc.getDetailUrl());
            result.add(vo);
        }
        return result;
    }

    @Override
    protected List<OshHomePageHotOpenProjectEsDocument> toDocs(List<HotOpenProjectVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotOpenProjectEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotOpenProjectVO vo = vos.get(i);
            OshHomePageHotOpenProjectEsDocument doc = new OshHomePageHotOpenProjectEsDocument();
            doc.setId(vo.getId());
            doc.setProjectName(vo.getProjectName());
            doc.setProjectDesc(vo.getProjectDesc());
            doc.setProjectUrl(vo.getProjectUrl());
            doc.setAuthorName(vo.getAuthorName());
            doc.setProjectCover(vo.getProjectCover());
            doc.setStarCount(vo.getStarCount());
            doc.setForkCount(vo.getForkCount());
            doc.setLastCommit(vo.getLastCommit());
            doc.setTagList(vo.getTagList());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }
}
