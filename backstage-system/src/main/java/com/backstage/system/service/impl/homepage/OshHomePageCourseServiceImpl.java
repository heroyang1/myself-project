package com.backstage.system.service.impl.homepage;

import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.homepage.es.OshHomePageHotCourseEsDocument;
import com.backstage.system.domain.homepage.vo.HotCourseLatestSectionVO;
import com.backstage.system.domain.homepage.vo.HotCourseVO;
import com.backstage.system.mapper.course.OshCourseSectionMapper;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import com.backstage.system.mapper.homepage.OshHomePageCourseMapper;
import com.backstage.system.mapper.homepage.OshHomePageHotCourseEsMapper;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.homepage.IOshHomePageCourseService;
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
public class OshHomePageCourseServiceImpl
        extends AbstractOshHomePageEsCardService<OshHomePageHotCourseEsDocument, HotCourseVO>
        implements IOshHomePageCourseService {

    private static final String CARD_TYPE = "course";
    private static final int COVER_SIGN_MINUTES = 1440;

    @Autowired private OshHomePageCourseMapper homePageCourseMapper;
    @Autowired private OssService ossService;
    @Autowired private IOshHomePageModulePathService modulePathService;
    @Autowired private OshHomePageHotCourseEsMapper hotCourseEsMapper;
    @Autowired private OshCourseSectionMapper courseSectionMapper;

    @Override
    public String cardType() {
        return CARD_TYPE;
    }

    @Override
    public List<HotCourseVO> getHotCourses(int limit) {
        return getCards(limit);
    }

    @Override
    public List<HotCourseVO> getHotCoursesFromMysql(int effectiveLimit) {
        return signCoverUrls(getHotCoursesRawFromMysql(effectiveLimit));
    }

    @Override
    public List<HotCourseLatestSectionVO> getLatestCourseUpdates(Long courseId, Integer limit) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        int safeLimit = limit == null ? 2 : Math.max(1, Math.min(limit, 10));
        return courseSectionMapper.selectLatestUpdatedSectionsByCourseId(courseId, safeLimit);
    }

    @Override
    public int syncHotCoursesToEs() {
        return syncToEs();
    }

    @Override
    protected AbstractOshHomePageAliasEsMapper<OshHomePageHotCourseEsDocument> esMapper() {
        return hotCourseEsMapper;
    }

    @Override
    protected List<HotCourseVO> queryFromMysql(int limit) {
        return getHotCoursesFromMysql(limit);
    }

    @Override
    protected List<HotCourseVO> queryFromMysqlForSync(int limit) {
        return getHotCoursesRawFromMysql(limit);
    }

    @Override
    protected List<HotCourseVO> toVos(List<OshHomePageHotCourseEsDocument> docs) {
        List<HotCourseVO> result = new ArrayList<>(docs.size());
        for (OshHomePageHotCourseEsDocument doc : docs) {
            HotCourseVO vo = new HotCourseVO();
            vo.setId(doc.getId());
            vo.setTitle(doc.getTitle());
            vo.setCover(doc.getCoverKey());
            vo.setPrice(doc.getPrice());
            vo.settPrice(doc.getTPrice());
            vo.setBuyCount(doc.getBuyCount());
            vo.setViewCount(doc.getViewCount());
            vo.setVideoCount(doc.getVideoCount());
            vo.setRatingScore(doc.getRatingScore());
            vo.setServiceContent(doc.getServiceContent());
            vo.setResourceType(doc.getResourceType());
            vo.setHotScore(doc.getHotScore());
            vo.setTags(doc.getTags());
            vo.setDetailUrl(doc.getDetailUrl());
            vo.setListUrl(doc.getListUrl());
            result.add(vo);
        }
        return signCoverUrls(fillLiveVideoCounts(result));
    }

    @Override
    protected List<OshHomePageHotCourseEsDocument> toDocs(List<HotCourseVO> vos, LocalDateTime syncTime) {
        List<OshHomePageHotCourseEsDocument> docs = new ArrayList<>(vos.size());
        for (int i = 0; i < vos.size(); i++) {
            HotCourseVO vo = vos.get(i);
            OshHomePageHotCourseEsDocument doc = new OshHomePageHotCourseEsDocument();
            doc.setId(vo.getId());
            doc.setTitle(vo.getTitle());
            doc.setCoverKey(vo.getCover());
            doc.setPrice(vo.getPrice());
            doc.setTPrice(vo.gettPrice());
            doc.setBuyCount(vo.getBuyCount());
            doc.setViewCount(vo.getViewCount());
            doc.setVideoCount(vo.getVideoCount());
            doc.setRatingScore(vo.getRatingScore());
            doc.setServiceContent(vo.getServiceContent());
            doc.setResourceType(vo.getResourceType());
            doc.setHotScore(vo.getHotScore());
            doc.setTags(vo.getTags());
            doc.setDetailUrl(vo.getDetailUrl());
            doc.setListUrl(vo.getListUrl());
            doc.setSortNo(i + 1);
            doc.setSourceLimit(syncLimit());
            doc.setSyncTime(syncTime);
            docs.add(doc);
        }
        return docs;
    }

    private List<HotCourseVO> getHotCoursesRawFromMysql(int effectiveLimit) {
        List<HotCourseVO> courses = homePageCourseMapper.selectHotCourses(effectiveLimit);
        if (courses.isEmpty()) {
            return courses;
        }

        List<Long> courseIds = courses.stream().map(HotCourseVO::getId).collect(Collectors.toList());
        List<HotCourseVO> tagRels = homePageCourseMapper.selectTagsByCourseIds(courseIds);
        Map<Long, List<String>> tagMap = tagRels.stream()
                .collect(Collectors.groupingBy(
                        HotCourseVO::getCourseId,
                        Collectors.mapping(HotCourseVO::getTagName, Collectors.toList())
                ));

        for (HotCourseVO vo : courses) {
            List<String> tags = tagMap.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setTags(tags.subList(0, Math.min(2, tags.size())));
            vo.setDetailUrl(modulePathService.getDetailPath("course", vo.getId()));
            vo.setListUrl(modulePathService.getListPath("course"));
        }
        return fillLiveVideoCounts(courses);
    }

    private List<HotCourseVO> signCoverUrls(List<HotCourseVO> courses) {
        if (courses == null || courses.isEmpty()) {
            return courses;
        }
        for (HotCourseVO vo : courses) {
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossService.getLimitedUrl(vo.getCover(), COVER_SIGN_MINUTES));
            }
        }
        return courses;
    }

    private List<HotCourseVO> fillLiveVideoCounts(List<HotCourseVO> courses) {
        if (courses == null || courses.isEmpty()) {
            return courses;
        }
        List<Long> courseIds = courses.stream()
                .map(HotCourseVO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return courses;
        }
        Map<Long, Integer> videoCountMap = homePageCourseMapper.selectVideoCountsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(
                        HotCourseVO::getCourseId,
                        vo -> vo.getVideoCount() == null ? 0 : vo.getVideoCount(),
                        (left, right) -> right
                ));
        for (HotCourseVO course : courses) {
            course.setVideoCount(videoCountMap.getOrDefault(course.getId(), 0));
        }
        return courses;
    }
}
