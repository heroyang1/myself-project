package com.backstage.system.service.book;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.UserBookRelation;
import com.backstage.system.domain.book.BookChapterPageDO;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.vo.book.BookChapterVO;
import com.backstage.system.domain.vo.book.BookDetailVO;
import com.backstage.system.domain.vo.book.BookMenuVO;
import com.backstage.system.mapper.book.BookChapterMapper;
import com.backstage.system.mapper.book.BookChapterPageMapper;
import com.backstage.system.mapper.book.BookMapper;
import com.backstage.system.mapper.book.BookTagDOMapper;
import com.backstage.system.mapper.book.UserBookRelationMapper;
import com.backstage.system.service.impl.BookServiceImpl;
import com.backstage.common.async.AsyncTaskSupport;
import org.junit.After;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BookComicAccessSecurityTest {

    private static final Long BOOK_ID = 1008L;
    private static final Long FREE_CHAPTER_ID = 3001L;
    private static final Long PAID_CHAPTER_ID = 3002L;

    @After
    public void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    public void shouldHidePaidComicPagesFromUnauthorizedDetailAndKeepMenuSanitized() {
        Fixture fixture = createFixture();
        when(fixture.bookChapterPageMapper.selectList(any())).thenReturn(Arrays.asList(freePage(), paidPage()));

        BookDetailVO detail = fixture.service.selectBookDetail(BOOK_ID, false);
        BookMenuVO menu = fixture.service.selectBookMenu(BOOK_ID);

        assertEquals(1, detail.getBookDetails().get(0).getPages().size());
        assertEquals(0, detail.getBookDetails().get(1).getPages().size());
        assertNull(menu.getMenus().get(0).getPages());
        assertNull(menu.getMenus().get(1).getPages());

        verify(fixture.bookChapterPageMapper).selectList(any());
    }

    @Test
    public void shouldReturnPaidComicPagesForPurchasedUser() {
        Fixture fixture = createFixture();
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 9L);
        ThreadLocalUtil.set(OshUserConstants.LEVEL, "1");
        UserBookRelation relation = new UserBookRelation();
        relation.setPurchased(1);
        when(fixture.userBookRelationMapper.selectByUserIdAndBookId(9L, BOOK_ID)).thenReturn(relation);
        when(fixture.bookChapterPageMapper.selectList(any())).thenReturn(Arrays.asList(freePage(), paidPage()));

        BookDetailVO detail = fixture.service.selectBookDetail(BOOK_ID, false);

        assertTrue(detail.getIsbuy());
        assertEquals("http://assets.example/free-page.jpg", detail.getBookDetails().get(0).getPages().get(0).getImageUrl());
        assertEquals("http://assets.example/paid-page.jpg", detail.getBookDetails().get(1).getPages().get(0).getImageUrl());

        verify(fixture.bookChapterPageMapper).selectList(any());
    }

    @Test
    public void shouldUseCapturedLevelWhenAggregationThreadLosesThreadLocal() {
        Fixture fixture = createFixture(clearingThreadExecutor());
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 8L);
        ThreadLocalUtil.set(OshUserConstants.LEVEL, "4");
        when(fixture.bookMapper.selectReadableById(BOOK_ID)).thenReturn(null);
        when(fixture.bookMapper.selectDeletedById(BOOK_ID)).thenReturn(comicBook());
        when(fixture.bookChapterPageMapper.selectList(any())).thenReturn(Collections.emptyList());

        BookDetailVO detail = fixture.service.selectBookDetail(BOOK_ID, false);

        assertEquals(BOOK_ID, detail.getId());
        verify(fixture.bookMapper).selectDeletedById(BOOK_ID);
    }

    private Fixture createFixture() {
        return createFixture((Executor) Runnable::run);
    }

    private Fixture createFixture(Executor aggregationTaskExecutor) {
        BookMapper bookMapper = mock(BookMapper.class);
        BookChapterMapper bookChapterMapper = mock(BookChapterMapper.class);
        BookChapterPageMapper bookChapterPageMapper = mock(BookChapterPageMapper.class);
        BookTagDOMapper bookTagDOMapper = mock(BookTagDOMapper.class);
        UserBookRelationMapper userBookRelationMapper = mock(UserBookRelationMapper.class);

        BookServiceImpl service = new BookServiceImpl();
        ReflectionTestUtils.setField(service, "bookMapper", bookMapper);
        ReflectionTestUtils.setField(service, "bookChapterMapper", bookChapterMapper);
        ReflectionTestUtils.setField(service, "bookChapterPageMapper", bookChapterPageMapper);
        ReflectionTestUtils.setField(service, "bookTagDOMapper", bookTagDOMapper);
        ReflectionTestUtils.setField(service, "userBookRelationMapper", userBookRelationMapper);
        ReflectionTestUtils.setField(service, "asyncTaskSupport", new AsyncTaskSupport());
        ReflectionTestUtils.setField(service, "aggregationTaskExecutor", aggregationTaskExecutor);

        when(bookMapper.selectReadableById(BOOK_ID)).thenReturn(comicBook());
        when(bookChapterMapper.selectBookChapterListByBookId(BOOK_ID)).thenReturn(Arrays.asList(
                chapter(FREE_CHAPTER_ID, "免费试读", 1),
                chapter(PAID_CHAPTER_ID, "付费章节", 0)
        ));
        when(bookTagDOMapper.selectBookTagListByBookId(BOOK_ID)).thenReturn(Collections.emptyList());

        return new Fixture(service, bookMapper, bookChapterPageMapper, userBookRelationMapper);
    }

    private static Executor clearingThreadExecutor() {
        return command -> {
            Thread thread = new Thread(() -> {
                ThreadLocalUtil.remove();
                command.run();
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        };
    }

    private BookDO comicBook() {
        BookDO book = new BookDO();
        book.setId(BOOK_ID);
        book.setTitle("程序员入门前必懂的计算机通识");
        book.setCover("");
        book.setDescription("comic security test");
        book.setPrice(new BigDecimal("19.90"));
        book.setOriginalPrice(new BigDecimal("39.90"));
        book.setLevel(2);
        book.setMediaType(2);
        book.setStatus("0");
        return book;
    }

    private BookChapterVO chapter(Long id, String title, Integer isFree) {
        BookChapterVO chapter = new BookChapterVO();
        chapter.setId(id);
        chapter.setTitle(title);
        chapter.setChapterNo(id.equals(FREE_CHAPTER_ID) ? 1 : 2);
        chapter.setSortOrder(chapter.getChapterNo());
        chapter.setIsFree(isFree);
        return chapter;
    }

    private static BookChapterPageDO freePage() {
        return page(FREE_CHAPTER_ID, "http://assets.example/free-page.jpg");
    }

    private static BookChapterPageDO paidPage() {
        return page(PAID_CHAPTER_ID, "http://assets.example/paid-page.jpg");
    }

    private static BookChapterPageDO page(Long chapterId, String imageUrl) {
        BookChapterPageDO page = new BookChapterPageDO();
        page.setBookId(BOOK_ID);
        page.setChapterId(chapterId);
        page.setPageNo(1);
        page.setImageUrl(imageUrl);
        return page;
    }

    private static class Fixture {
        private final BookServiceImpl service;
        private final BookMapper bookMapper;
        private final BookChapterPageMapper bookChapterPageMapper;
        private final UserBookRelationMapper userBookRelationMapper;

        private Fixture(BookServiceImpl service, BookMapper bookMapper, BookChapterPageMapper bookChapterPageMapper,
                        UserBookRelationMapper userBookRelationMapper) {
            this.service = service;
            this.bookMapper = bookMapper;
            this.bookChapterPageMapper = bookChapterPageMapper;
            this.userBookRelationMapper = userBookRelationMapper;
        }
    }
}
