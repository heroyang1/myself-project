package com.backstage.web.controller.course;

import com.backstage.RuoYiApplication;
import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.course.OshCourseQuestion;
import com.backstage.system.domain.course.OshCourseSection;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.course.OshCourseMapper;
import com.backstage.system.mapper.course.OshCourseQuestionMapper;
import com.backstage.system.mapper.document.OshDocMapper;
import com.backstage.system.request.CourseChapterCreateRequest;
import com.backstage.system.request.CourseCreateRequest;
import com.backstage.system.request.CourseQuestionAnswerRequest;
import com.backstage.system.request.CourseSectionQuestionListRequest;
import com.backstage.system.request.CourseSearchRequest;
import com.backstage.system.request.CourseSectionQuestionRequest;
import com.backstage.system.request.CourseUpdateRequest;
import com.backstage.system.request.CourseVideoSectionCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RuoYiApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class OshCourseControllerSectionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OshCourseMapper oshCourseMapper;

    @Autowired
    private OshCourseQuestionMapper oshCourseQuestionMapper;

    @Autowired
    private OshDocMapper oshDocMapper;

    @Test
    public void shouldPersistAndReloadVideoSectionDocContentForEditPage() throws Exception {
        Long courseId = createTestCourse();
        Long parentId = createParentChapter(courseId);

        CourseVideoSectionCreateRequest createRequest = new CourseVideoSectionCreateRequest();
        createRequest.setCourseId(courseId);
        createRequest.setParentId(parentId);
        createRequest.setTitle("集成测试视频文档小节");
        createRequest.setSort(2);
        createRequest.setFreeFlag(1);
        createRequest.setDuration(600);
        createRequest.setMediaUrl("https://oss.example.com/integration-video.mp4");
        createRequest.setTextContent("<!--OSH_DOC_FORMAT:MARKDOWN--># 集成测试标题\n\n保存后应能重新加载。");
        createRequest.setFileSize(654321L);
        createRequest.setDocMode("CREATE");
        createRequest.setDocTitle("集成测试视频文档小节");

        MvcResult createResult = performAsUser(buildCurrentUser(), post("/pc/course/section/video/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long sectionId = extractDataId(createResult);
        String expectedContent = createRequest.getTextContent();

        performAsUser(buildCurrentUser(), get("/pc/course/section/editContent/" + courseId + "/" + sectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(expectedContent));

        performAsUser(buildCurrentUser(), get("/pc/course/section/content/" + courseId + "/" + sectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(expectedContent));

        String docContent = oshDocMapper.selectPrimaryDocContentBySectionId(sectionId);
        assertEquals(expectedContent, docContent);

        CourseVideoSectionCreateRequest updateRequest = new CourseVideoSectionCreateRequest();
        updateRequest.setId(sectionId);
        updateRequest.setCourseId(courseId);
        updateRequest.setParentId(parentId);
        updateRequest.setTitle("集成测试视频文档小节-更新");
        updateRequest.setSort(2);
        updateRequest.setFreeFlag(0);
        updateRequest.setDuration(610);
        updateRequest.setTextContent("<!--OSH_DOC_FORMAT:MARKDOWN--># 更新后的标题\n\n第二次保存。");
        updateRequest.setFileSize(654321L);
        updateRequest.setDocMode("CREATE");
        updateRequest.setDocTitle("集成测试视频文档小节-更新");

        performAsUser(buildCurrentUser(), post("/pc/course/section/video/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        performAsUser(buildCurrentUser(), get("/pc/course/section/editContent/" + courseId + "/" + sectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(updateRequest.getTextContent()));
    }

    @Test
    public void shouldInsertChapterSectionIntoDatabase() throws Exception {
        Long courseId = createTestCourse();

        CourseChapterCreateRequest request = new CourseChapterCreateRequest();
        request.setCourseId(courseId);
        request.setTitle("集成测试一级章节");
        request.setSort(1);

        MvcResult mvcResult = performAsUser(buildCurrentUser(), post("/pc/course/section/chapter/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long sectionId = extractDataId(mvcResult);
        OshCourseSection saved = oshCourseMapper.selectCourseSectionById(sectionId);
        assertNotNull(saved);
        assertEquals(courseId, saved.getCourseId());
        assertEquals(Long.valueOf(0L), saved.getParentId());
        assertEquals("集成测试一级章节", saved.getTitle());
        assertEquals(Integer.valueOf(1), saved.getFreeFlag());
        assertEquals(Integer.valueOf(1), saved.getStatus());
        assertEquals(Integer.valueOf(0), saved.getDeleteFlag());
        assertEquals("integration_test_user", saved.getCreateBy());
    }

    @Test
    public void shouldInsertVideoSectionWithTextContentIntoDatabase() throws Exception {
        Long courseId = createTestCourse();
        Long parentId = createParentChapter(courseId);

        CourseVideoSectionCreateRequest request = new CourseVideoSectionCreateRequest();
        request.setCourseId(courseId);
        request.setParentId(parentId);
        request.setTitle("集成测试视频小节");
        request.setSort(2);
        request.setFreeFlag(0);
        request.setDuration(600);
        request.setMediaUrl("https://oss.example.com/integration-video.mp4");
        request.setCover("https://oss.example.com/video-cover.png");
        request.setVideoDesc("集成测试视频描述");
        request.setTextContent("这里是视频配套文本内容");
        request.setFileSize(654321L);

        MvcResult mvcResult = performAsUser(buildCurrentUser(), post("/pc/course/section/video/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long sectionId = extractDataId(mvcResult);
        OshCourseSection saved = oshCourseMapper.selectCourseSectionById(sectionId);
        assertNotNull(saved);
        assertEquals(courseId, saved.getCourseId());
        assertEquals(parentId, saved.getParentId());
        assertEquals("video", saved.getType());
        assertEquals("https://oss.example.com/integration-video.mp4", saved.getMediaUrl());
        assertEquals("这里是视频配套文本内容", saved.getTextContent());
        assertEquals(Long.valueOf(654321L), saved.getFileSize());
        assertEquals("integration_test_user", saved.getCreateBy());
    }

    @Test
    public void shouldReturnNotFoundWhenSavingTextSection() throws Exception {
        Long courseId = createTestCourse();
        Long parentId = createParentChapter(courseId);

        MvcResult mvcResult = performAsUser(buildCurrentUser(), post("/pc/course/section/text/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":" + courseId + ",\"parentId\":" + parentId + ",\"title\":\"集成测试图文小节\",\"sort\":3,\"freeFlag\":0,\"cover\":\"https://oss.example.com/text-cover.png\",\"textContent\":\"这里是集成测试图文内容\"}"))
                .andExpect(status().isNotFound())
                .andReturn();
        assertEquals(404, mvcResult.getResponse().getStatus());
    }

    @Test
    public void shouldReturnNotFoundWhenGettingTextSection() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/pc/course/section/text/1"))
                .andExpect(status().isNotFound())
                .andReturn();

        assertEquals(404, mvcResult.getResponse().getStatus());
    }

    @Test
    public void shouldSubmitQuestionForCourse935Section2WithUser1() throws Exception {
        OshCourse beforeCourse = oshCourseMapper.selectCourseById(935L);
        int beforeQuestionCount = beforeCourse.getQuestionCount() == null ? 0 : beforeCourse.getQuestionCount();

        String suffix = String.valueOf(System.currentTimeMillis());
        CourseSectionQuestionRequest request = new CourseSectionQuestionRequest();
        request.setCourseId(935L);
        request.setSectionId(2L);
        request.setTitle("集成测试提问-" + suffix);
        request.setContent("这是用户1针对课程935章节2写入的测试提问-" + suffix);

        MvcResult mvcResult = performAsUser(buildUserOne(), post("/pc/course/section/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long questionId = extractDataId(mvcResult);
        OshCourseQuestion saved = oshCourseQuestionMapper.selectQuestionById(questionId);
        assertNotNull(saved);
        assertEquals(Long.valueOf(935L), saved.getCourseId());
        assertEquals(Long.valueOf(2L), saved.getSectionId());
        assertEquals(Long.valueOf(1L), saved.getUserId());
        assertEquals(Integer.valueOf(1), saved.getRecordType());
        assertEquals("集成测试提问-" + suffix, saved.getTitle());
        assertEquals("这是用户1针对课程935章节2写入的测试提问-" + suffix, saved.getContent());
        OshCourse afterCourse = oshCourseMapper.selectCourseById(935L);
        assertEquals(beforeQuestionCount + 1, afterCourse.getQuestionCount().intValue());
    }

    @Test
    public void shouldSubmitAnswerForQuestionUnderCourse935Section2WithUser1() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        CourseSectionQuestionRequest questionRequest = new CourseSectionQuestionRequest();
        questionRequest.setCourseId(935L);
        questionRequest.setSectionId(2L);
        questionRequest.setTitle("集成测试回答前置提问-" + suffix);
        questionRequest.setContent("这是回答测试的前置提问-" + suffix);

        MvcResult questionResult = performAsUser(buildUserOne(), post("/pc/course/section/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long questionId = extractDataId(questionResult);

        CourseQuestionAnswerRequest answerRequest = new CourseQuestionAnswerRequest();
        answerRequest.setQuestionId(questionId);
        answerRequest.setContent("这是用户1针对问题" + questionId + "写入的测试回答-" + suffix);

        MvcResult answerResult = performAsUser(buildUserOne(), post("/pc/course/question/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long answerId = extractDataId(answerResult);
        OshCourseQuestion answer = oshCourseQuestionMapper.selectQuestionById(answerId);
        assertNotNull(answer);
        assertEquals(Long.valueOf(935L), answer.getCourseId());
        assertEquals(Long.valueOf(2L), answer.getSectionId());
        assertEquals(Long.valueOf(1L), answer.getUserId());
        assertEquals(Integer.valueOf(2), answer.getRecordType());
        assertEquals(questionId, answer.getQuestionId());
        assertEquals(questionId, answer.getParentId());
        assertEquals("这是用户1针对问题" + questionId + "写入的测试回答-" + suffix, answer.getContent());

        OshCourseQuestion question = oshCourseQuestionMapper.selectQuestionById(questionId);
        assertNotNull(question);
        assertEquals(Integer.valueOf(1), question.getReplyCount());
    }

    @Test
    public void shouldSortSectionQuestionsByLikeCountAndCreateTimeWhenAnonymous() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        Long courseId = createTestCourse();
        Long sectionId = createParentChapter(courseId);
        insertQuestionRecord(courseId, sectionId, 11L, "匿名排序低赞新问题-" + suffix, "内容1", 3, new java.util.Date(System.currentTimeMillis()));
        insertQuestionRecord(courseId, sectionId, 12L, "匿名排序高赞旧问题-" + suffix, "内容2", 9, new java.util.Date(System.currentTimeMillis() - 10000L));
        insertQuestionRecord(courseId, sectionId, 13L, "匿名排序高赞新问题-" + suffix, "内容3", 9, new java.util.Date(System.currentTimeMillis()));

        CourseSectionQuestionListRequest request = new CourseSectionQuestionListRequest();
        request.setCourseId(courseId);
        request.setSectionId(sectionId);

        mockMvc.perform(post("/pc/course/section/questions/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("匿名排序高赞新问题-" + suffix))
                .andExpect(jsonPath("$.data[1].title").value("匿名排序高赞旧问题-" + suffix))
                .andExpect(jsonPath("$.data[2].title").value("匿名排序低赞新问题-" + suffix));
    }

    @Test
    public void shouldSortSectionQuestionsByOwnerThenLikeCountAndCreateTimeWhenLoggedIn() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        OshUser currentUser = buildUserOne();
        Long courseId = createOwnedCourse(currentUser, 1);
        Long sectionId = createParentChapter(courseId);
        insertQuestionRecord(courseId, sectionId, currentUser.getId(), "我的低赞问题-" + suffix, "内容1", 1, new java.util.Date(System.currentTimeMillis() - 20000L));
        insertQuestionRecord(courseId, sectionId, 22L, "别人的高赞问题-" + suffix, "内容2", 99, new java.util.Date(System.currentTimeMillis()));
        insertQuestionRecord(courseId, sectionId, currentUser.getId(), "我的高赞问题-" + suffix, "内容3", 8, new java.util.Date(System.currentTimeMillis()));

        CourseSectionQuestionListRequest request = new CourseSectionQuestionListRequest();
        request.setCourseId(courseId);
        request.setSectionId(sectionId);

        performAsUser(currentUser, post("/pc/course/section/questions/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("我的高赞问题-" + suffix))
                .andExpect(jsonPath("$.data[1].title").value("我的低赞问题-" + suffix))
                .andExpect(jsonPath("$.data[2].title").value("别人的高赞问题-" + suffix));
    }

    @Test
    public void shouldListQuestionAnswersInCreateTimeOrder() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        CourseSectionQuestionRequest questionRequest = new CourseSectionQuestionRequest();
        questionRequest.setCourseId(935L);
        questionRequest.setSectionId(2L);
        questionRequest.setTitle("回答列表测试提问-" + suffix);
        questionRequest.setContent("回答列表测试提问内容-" + suffix);

        MvcResult questionResult = performAsUser(buildUserOne(), post("/pc/course/section/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long questionId = extractDataId(questionResult);

        CourseQuestionAnswerRequest firstAnswerRequest = new CourseQuestionAnswerRequest();
        firstAnswerRequest.setQuestionId(questionId);
        firstAnswerRequest.setContent("第一条回答-" + suffix);

        CourseQuestionAnswerRequest secondAnswerRequest = new CourseQuestionAnswerRequest();
        secondAnswerRequest.setQuestionId(questionId);
        secondAnswerRequest.setContent("第二条回答-" + suffix);

        performAsUser(buildUserOne(), post("/pc/course/question/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstAnswerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Thread.sleep(5L);

        performAsUser(buildUserOne(), post("/pc/course/question/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondAnswerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/pc/course/question/answers/" + questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].content").value("第一条回答-" + suffix))
                .andExpect(jsonPath("$.data[1].content").value("第二条回答-" + suffix));
    }

    @Test
    public void shouldSearchCoursesWithCollectionAndBuyFlagsForLoggedInUser() throws Exception {
        CourseSearchRequest request = new CourseSearchRequest();
        request.setKeyword("Spring Boot 企业级开发实战");
        request.setPageNum(1);
        request.setPageSize(10);

        performAsUser(buildUserOne(), post("/pc/course/search/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.rows[0].id").value(935))
                .andExpect(jsonPath("$.data.rows[0].buyFlag").value(1))
                .andExpect(jsonPath("$.data.rows[0].collectionFlag").value(0));
    }

    @Test
    public void shouldSearchCoursesOrderedBySalesCountThenCreateTimeDesc() throws Exception {
        CourseSearchRequest request = new CourseSearchRequest();
        request.setPageNum(1);
        request.setPageSize(2);

        mockMvc.perform(post("/pc/course/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.rows[0].id").value(942))
                .andExpect(jsonPath("$.data.rows[1].id").value(957));
    }

    @Test
    public void shouldSearchLoginCoursesOrderedByBuyFlagThenScoreThenCreateTimeDesc() throws Exception {
        CourseSearchRequest request = new CourseSearchRequest();
        request.setPageNum(1);
        request.setPageSize(2);

        performAsUser(buildUserOne(), post("/pc/course/search/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.rows[0].id").value(935))
                .andExpect(jsonPath("$.data.rows[1].id").value(942));
    }

    @Test
    public void shouldMapCollectionCountInCourseDetail() {
        Long courseId = createPublishedTestCourse();

        com.backstage.system.domain.course.vo.OshCourseDetailVo detail = oshCourseMapper.getCourseDetail(courseId, 1L, false);

        assertNotNull(detail);
        assertEquals(courseId, detail.getId());
        assertTrue(detail.getCollectionCount() == null || detail.getCollectionCount() >= 0);
    }

    @Test
    public void shouldApprovePendingCourseByAuditApi() throws Exception {
        Long courseId = createPendingAuditCourse();

        performAsUser(buildCurrentUser(), post("/pc/course/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":" + courseId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(courseId));

        OshCourse approvedCourse = oshCourseMapper.selectCourseById(courseId);
        assertNotNull(approvedCourse);
        assertEquals(Integer.valueOf(2), approvedCourse.getStatus());
    }

    @Test
    public void shouldBlockUpdateAfterSaveForSameUserWithinOneMinute() throws Exception {
        OshUser currentUser = buildLockTestUser();
        Long courseId = createOwnedCourse(currentUser, 1);

        CourseCreateRequest saveRequest = new CourseCreateRequest();
        saveRequest.setTitle("锁测试新增课程-" + System.currentTimeMillis());
        saveRequest.setCover("https://oss.example.com/lock-save-cover.png");
        saveRequest.setIntro("锁测试新增课程简介");
        saveRequest.setServiceContent("锁测试新增课程服务内容");
        saveRequest.setPrice(new BigDecimal("9.90"));
        saveRequest.setTPrice(new BigDecimal("19.90"));
        saveRequest.setType("media");

        MvcResult saveResult = performAsUser(currentUser, post("/pc/course/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long savedCourseId = extractDataId(saveResult);
        OshCourse savedCourse = oshCourseMapper.selectCourseById(savedCourseId);
        assertNotNull(savedCourse);
        assertEquals(Integer.valueOf(2), savedCourse.getStatus());

        CourseUpdateRequest updateRequest = new CourseUpdateRequest();
        updateRequest.setId(courseId);
        updateRequest.setTitle("锁测试修改课程标题");

        performAsUser(currentUser, post("/pc/course/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(Matchers.containsString("acquire lock failed")));
    }

    @Test
    public void shouldPublishCourseAfterUpdate() throws Exception {
        OshUser currentUser = buildCurrentUser();
        Long courseId = createOwnedCourse(currentUser, 1);

        System.out.println(courseId);
        CourseUpdateRequest updateRequest = new CourseUpdateRequest();
        updateRequest.setId(courseId);
        updateRequest.setTitle("更新后直接发布课程");

        performAsUser(currentUser, post("/pc/course/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        OshCourse updatedCourse = oshCourseMapper.selectCourseById(courseId);
        assertNotNull(updatedCourse);
        assertEquals(Integer.valueOf(2), updatedCourse.getStatus());
    }

    private Long extractDataId(MvcResult mvcResult) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return jsonNode.get("data").asLong();
    }

    private Long createTestCourse() {
        String suffix = String.valueOf(System.currentTimeMillis());
        OshCourse course = new OshCourse();
        course.setTitle("集成测试课程-" + suffix);
        course.setCover("https://oss.example.com/course-cover.png");
        course.setIntro("集成测试课程简介");
        course.setServiceContent("集成测试服务内容");
        course.setPrice(new BigDecimal("0.01"));
        course.setTPrice(new BigDecimal("0.01"));
        course.setType("media");
        course.setStatus(0);
        course.setCreateBy("integration_test_user");
        course.setUpdateBy("integration_test_user");
        oshCourseMapper.insertCourse(course);
        return course.getId();
    }

    private Long createPublishedTestCourse() {
        String suffix = String.valueOf(System.currentTimeMillis());
        OshCourse course = new OshCourse();
        course.setTitle("已发布课程详情测试-" + suffix);
        course.setCover("https://oss.example.com/course-detail-cover.png");
        course.setIntro("课程详情映射测试");
        course.setServiceContent("课程详情映射测试服务内容");
        course.setPrice(new BigDecimal("9.90"));
        course.setTPrice(new BigDecimal("19.90"));
        course.setType("media");
        course.setStatus(2);
        course.setCreateBy("integration_test_user");
        course.setUpdateBy("integration_test_user");
        oshCourseMapper.insertCourse(course);
        return course.getId();
    }

    private Long createPendingAuditCourse() {
        String suffix = String.valueOf(System.currentTimeMillis());
        OshCourse course = new OshCourse();
        course.setTitle("待审核课程-" + suffix);
        course.setCover("https://oss.example.com/course-audit-cover.png");
        course.setIntro("待审核课程简介");
        course.setServiceContent("待审核课程服务内容");
        course.setPrice(new BigDecimal("19.90"));
        course.setTPrice(new BigDecimal("29.90"));
        course.setType("media");
        course.setStatus(1);
        course.setCreateBy("integration_test_user");
        course.setUpdateBy("integration_test_user");
        oshCourseMapper.insertCourse(course);
        return course.getId();
    }

    private Long createOwnedCourse(OshUser user, Integer status) {
        String suffix = String.valueOf(System.currentTimeMillis());
        OshCourse course = new OshCourse();
        course.setTitle("锁测试课程-" + suffix);
        course.setCover("https://oss.example.com/lock-course-cover.png");
        course.setIntro("锁测试课程简介");
        course.setServiceContent("锁测试课程服务内容");
        course.setPrice(new BigDecimal("19.90"));
        course.setTPrice(new BigDecimal("29.90"));
        course.setType("media");
        course.setStatus(status);
        course.setCreateBy(user.getUsername());
        course.setUpdateBy(user.getUsername());
        oshCourseMapper.insertCourse(course);
        return course.getId();
    }

    private Long insertQuestionRecord(Long courseId, Long sectionId, Long userId, String title, String content, Integer likeCount, java.util.Date createTime) {
        OshCourseQuestion question = new OshCourseQuestion();
        question.setCourseId(courseId);
        question.setSectionId(sectionId);
        question.setUserId(userId);
        question.setQuestionId(0L);
        question.setParentId(0L);
        question.setRecordType(1);
        question.setTitle(title);
        question.setContent(content);
        question.setSolveStatus(0);
        question.setAcceptedAnswerId(0L);
        question.setReplyCount(0);
        question.setLikeCount(likeCount);
        question.setStatus(1);
        question.setDeleteFlag(0);
        question.setCreateBy("question_test_user_" + userId);
        question.setCreateTime(createTime);
        question.setUpdateBy("question_test_user_" + userId);
        question.setUpdateTime(createTime);
        oshCourseQuestionMapper.insertCourseQuestion(question);
        return question.getId();
    }

    private Long createParentChapter(Long courseId) {
        OshCourseSection section = new OshCourseSection();
        section.setCourseId(courseId);
        section.setParentId(0L);
        section.setTitle("集成测试父章节");
        section.setSort(1);
        section.setFreeFlag(1);
        section.setStatus(1);
        section.setDeleteFlag(0);
        section.setCreateBy("integration_test_user");
        section.setUpdateBy("integration_test_user");
        oshCourseMapper.insertCourseSection(section);
        return section.getId();
    }

    private OshUser buildCurrentUser() {
        OshUser oshUser = new OshUser();
        oshUser.setId(1L);
        oshUser.setUsername("integration_test_user");
        return oshUser;
    }

    private OshUser buildUserOne() {
        OshUser oshUser = new OshUser();
        oshUser.setId(1L);
        oshUser.setUsername("user_1_test");
        return oshUser;
    }

    private OshUser buildLockTestUser() {
        long userId = System.currentTimeMillis();
        OshUser oshUser = new OshUser();
        oshUser.setId(userId);
        oshUser.setUsername("lock_test_user_" + userId);
        return oshUser;
    }

    private ResultActions performAsUser(OshUser oshUser, RequestBuilder requestBuilder) throws Exception {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, oshUser.getId());
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, oshUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                oshUser.getUsername(),
                null,
                Arrays.asList(
                        new SimpleGrantedAuthority("course:section:video"),
                        new SimpleGrantedAuthority("course:section:text"),
                        new SimpleGrantedAuthority("course:update"),
                        new SimpleGrantedAuthority("course:create")
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return mockMvc.perform(requestBuilder);
    }
}
