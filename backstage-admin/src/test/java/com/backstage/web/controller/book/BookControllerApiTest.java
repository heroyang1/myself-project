package com.backstage.web.controller.book;

import com.backstage.RuoYiApplication;
import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.book.BookTagDO;
import com.backstage.system.domain.questionanswer.Question;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.book.BookChapterVO;
import com.backstage.system.mapper.book.BookChapterMapper;
import com.backstage.system.mapper.book.BookMapper;
import com.backstage.system.mapper.book.BookTagDOMapper;
import com.backstage.system.mapper.questionanswer.OshQAQuestionMapper;
import com.backstage.system.service.ISysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuoYiApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class BookControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BookChapterMapper bookChapterMapper;

    @Autowired
    private BookTagDOMapper bookTagDOMapper;

    @Autowired
    private OshQAQuestionMapper oshQaQuestionMapper;

    @MockBean
    private ISysConfigService sysConfigService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        Mockito.when(sysConfigService.selectCaptchaEnabled()).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldCreateUpdateAndDeleteBookWithLevelTagsAndChapters() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        ObjectNode createPayload = objectMapper.createObjectNode();
        createPayload.put("title", "api-book-" + suffix);
        createPayload.put("cover", "common/image/book/test-cover-" + suffix + ".png");
        createPayload.put("desc", "create book api test");
        createPayload.put("price", 29.90);
        createPayload.put("t_price", 59.90);
        createPayload.put("level", 4);
        createPayload.putArray("tags").add("api").add("ebook");
        ArrayNode createChapters = createPayload.putArray("chapters");
        createChapters.addObject()
                .put("title", "chapter-one-" + suffix)
                .put("content", "<h2>Chapter One</h2><p>test content</p>")
                .put("chapterNo", 1)
                .put("sortOrder", 1)
                .put("isFree", 1);
        createChapters.addObject()
                .put("title", "chapter-two-" + suffix)
                .put("content", "<h2>Chapter Two</h2><p>more content</p>")
                .put("chapterNo", 2)
                .put("sortOrder", 2)
                .put("isFree", 0);

        MvcResult createResult = performAsUser(buildCurrentUser(), post("/pc/book/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long bookId = extractDataId(createResult);
        BookDO created = bookMapper.selectById(bookId);
        assertNotNull(created);
        assertEquals("api-book-" + suffix, created.getTitle());
        assertEquals(Integer.valueOf(4), created.getLevel());
        assertEquals(new BigDecimal("59.90"), created.getOriginalPrice());

        List<BookChapterVO> createdChapters = bookChapterMapper.selectBookChapterListByBookId(bookId);
        assertEquals(2, createdChapters.size());
        assertEquals("chapter-one-" + suffix, createdChapters.get(0).getTitle());

        List<BookTagDO> createdTags = bookTagDOMapper.selectList(new LambdaQueryWrapper<BookTagDO>()
                .eq(BookTagDO::getBookId, bookId));
        assertEquals(2, createdTags.size());

        Long firstChapterId = createdChapters.get(0).getId();
        ObjectNode updatePayload = objectMapper.createObjectNode();
        updatePayload.put("id", bookId);
        updatePayload.put("title", "api-book-updated-" + suffix);
        updatePayload.put("cover", "common/image/book/test-cover-" + suffix + ".png");
        updatePayload.put("desc", "update book api test");
        updatePayload.put("price", 19.90);
        updatePayload.put("t_price", 39.90);
        updatePayload.put("level", 6);
        updatePayload.putArray("tags").add("updated-tag");
        ArrayNode updateChapters = updatePayload.putArray("chapters");
        updateChapters.addObject()
                .put("id", firstChapterId)
                .put("title", "rewritten-chapter-one-" + suffix)
                .put("content", "<h2>Rewritten Chapter One</h2><p>updated content</p>")
                .put("chapterNo", 1)
                .put("sortOrder", 1)
                .put("isFree", 1);
        updateChapters.addObject()
                .put("title", "new-chapter-three-" + suffix)
                .put("content", "<h2>New Chapter Three</h2><p>new content</p>")
                .put("chapterNo", 2)
                .put("sortOrder", 2)
                .put("isFree", 0);

        performAsUser(buildCurrentUser(), post("/pc/book/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        BookDO updated = bookMapper.selectById(bookId);
        assertNotNull(updated);
        assertEquals("api-book-updated-" + suffix, updated.getTitle());
        assertEquals(Integer.valueOf(6), updated.getLevel());
        assertEquals(new BigDecimal("39.90"), updated.getOriginalPrice());

        List<BookChapterVO> updatedChapters = bookChapterMapper.selectBookChapterListByBookId(bookId);
        assertEquals(2, updatedChapters.size());
        assertTrue(updatedChapters.stream().anyMatch(item -> ("rewritten-chapter-one-" + suffix).equals(item.getTitle())));
        assertTrue(updatedChapters.stream().anyMatch(item -> ("new-chapter-three-" + suffix).equals(item.getTitle())));

        List<BookTagDO> updatedTags = bookTagDOMapper.selectList(new LambdaQueryWrapper<BookTagDO>()
                .eq(BookTagDO::getBookId, bookId));
        assertEquals(1, updatedTags.size());
        assertEquals("updated-tag", updatedTags.get(0).getTagName());

        performAsUser(buildCurrentUser(), get("/pc/book/getById")
                .param("id", String.valueOf(bookId))
                .param("forEdit", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.level").value(6))
                .andExpect(jsonPath("$.data.book_details.length()").value(2));

        performAsUser(buildCurrentUser(), delete("/pc/book/delete")
                .param("id", String.valueOf(bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertNull(bookMapper.selectById(bookId));
    }

    @Test
    public void shouldCreateAndListQuestionsByBookResource() throws Exception {
        Long bookId = createStandaloneBook();
        String suffix = String.valueOf(System.currentTimeMillis());
        String questionContent = "ebook qna question content-" + suffix;
        ObjectNode questionPayload = objectMapper.createObjectNode();
        questionPayload.put("resourceType", "book");
        questionPayload.put("resourceNo", bookId);
        questionPayload.put("content", questionContent);
        questionPayload.put("isPaidOnly", 0);

        performAsUser(buildCurrentUser(), post("/pc/qna/question/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(questionPayload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Question question = oshQaQuestionMapper.selectOne(new LambdaQueryWrapper<Question>()
                .eq(Question::getResourceType, "book")
                .eq(Question::getResourceNo, bookId)
                .eq(Question::getContent, questionContent)
                .orderByDesc(Question::getId)
                .last("limit 1"));
        assertNotNull(question);
        assertEquals(Long.valueOf(1L), question.getUserId());
        ObjectNode listPayload = objectMapper.createObjectNode();
        listPayload.put("resourceType", "book");
        listPayload.put("resourceNo", bookId);
        listPayload.put("type", "all");
        listPayload.put("pageNum", 1);
        listPayload.put("pageSize", 10);

        performAsUser(buildCurrentUser(), post("/pc/qna/question/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(listPayload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].resourceType").value("book"))
                .andExpect(jsonPath("$.rows[0].resourceNo").value(bookId))
                .andExpect(jsonPath("$.rows[0].content").value(questionContent));
    }

    @Test
    public void shouldReturnAnonymousBookDetailWithIsbuyFlag() throws Exception {
        Long bookId = createStandaloneBook();

        ThreadLocalUtil.remove();
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/pc/book/getById")
                        .param("id", String.valueOf(bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(bookId))
                .andExpect(jsonPath("$.data.isbuy").value(false))
                .andExpect(jsonPath("$.data.level").value(3));
    }

    private Long createStandaloneBook() {
        String suffix = String.valueOf(System.currentTimeMillis());
        BookDO book = new BookDO();
        book.setTitle("qna-test-book-" + suffix);
        book.setCover("common/image/book/question-" + suffix + ".png");
        book.setDescription("qna test book description");
        book.setPrice(new BigDecimal("9.90"));
        book.setOriginalPrice(new BigDecimal("19.90"));
        book.setStatus("0");
        book.setLevel(3);
        book.setCreateBy(1L);
        book.setUpdateBy(1L);
        bookMapper.insert(book);
        return book.getId();
    }

    private Long extractDataId(MvcResult mvcResult) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return jsonNode.get("data").asLong();
    }

    private OshUser buildCurrentUser() {
        OshUser oshUser = new OshUser();
        oshUser.setId(1L);
        oshUser.setUsername("integration_test_user");
        return oshUser;
    }

    private ResultActions performAsUser(OshUser oshUser, RequestBuilder requestBuilder) throws Exception {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, oshUser.getId());
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, oshUser);
        ThreadLocalUtil.set(OshUserConstants.LEVEL, "6");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        oshUser.getUsername(),
                        "N/A",
                        Arrays.asList(
                                new SimpleGrantedAuthority("book:create"),
                                new SimpleGrantedAuthority("book:update"),
                                new SimpleGrantedAuthority("book:detail"),
                                new SimpleGrantedAuthority("book:delete"),
                                new SimpleGrantedAuthority("book:restore"),
                                new SimpleGrantedAuthority("qna:question:create"),
                                new SimpleGrantedAuthority("qna:question:list")
                        )
                )
        );
        return mockMvc.perform(requestBuilder);
    }
}
