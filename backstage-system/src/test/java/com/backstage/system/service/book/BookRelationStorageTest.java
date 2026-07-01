package com.backstage.system.service.book;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.async.AsyncTaskSupport;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.UserBookRelation;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.vo.book.BookChapterPageVO;
import com.backstage.system.domain.vo.book.BookChapterVO;
import com.backstage.system.domain.vo.book.BookDetailVO;
import com.backstage.system.domain.vo.book.BookMenuVO;
import com.backstage.system.domain.vo.book.BookRelationStatusVO;
import com.backstage.system.mapper.book.BookChapterMapper;
import com.backstage.system.mapper.book.BookMapper;
import com.backstage.system.mapper.book.BookTagDOMapper;
import com.backstage.system.mapper.book.UserBookRelationMapper;
import com.backstage.system.service.impl.BookServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookRelationStorageTest {

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void shouldStoreBookFavoriteInUserBookRelation() {
        UserBookRelationMapper relationMapper = Mockito.mock(UserBookRelationMapper.class);
        BookServiceImpl service = Mockito.spy(new BookServiceImpl());
        ReflectionTestUtils.setField(service, "userBookRelationMapper", relationMapper);
        Mockito.doReturn(new BookDO()).when(service).getById(7L);

        UserBookRelation relation = new UserBookRelation();
        relation.setId(11L);
        relation.setUserId(3L);
        relation.setBookId(7L);
        relation.setFavorited(0);
        when(relationMapper.selectByUserIdAndBookId(3L, 7L)).thenReturn(relation);
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 3L);

        service.favoriteBook(7L, 1);

        assertEquals(Integer.valueOf(1), relation.getFavorited());
        assertNotNull(relation.getFavoriteTime());
        verify(relationMapper).updateById(relation);
    }

    @Test
    void shouldReadFavoriteAndPurchaseStatusFromUserBookRelation() {
        UserBookRelationMapper relationMapper = Mockito.mock(UserBookRelationMapper.class);
        BookServiceImpl service = new BookServiceImpl();
        ReflectionTestUtils.setField(service, "userBookRelationMapper", relationMapper);

        UserBookRelation relation = new UserBookRelation();
        relation.setFavorited(1);
        relation.setFollowed(0);
        relation.setPurchased(1);
        relation.setFavoriteTime(LocalDateTime.now());
        when(relationMapper.selectByUserIdAndBookId(3L, 7L)).thenReturn(relation);
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 3L);

        BookRelationStatusVO status = service.getBookRelationStatus(7L);

        assertEquals(Integer.valueOf(1), status.getFavorited());
        assertEquals(Integer.valueOf(1), status.getPurchased());
    }

    @Test
    void shouldRequirePurchaseForVipPaidBookEvenWhenUserLevelIsEnough() {
        UserBookRelationMapper relationMapper = Mockito.mock(UserBookRelationMapper.class);
        BookServiceImpl service = new BookServiceImpl();
        ReflectionTestUtils.setField(service, "userBookRelationMapper", relationMapper);

        BookDO book = new BookDO();
        book.setId(7L);
        book.setLevel(2);
        book.setPrice(BigDecimal.TEN);

        com.backstage.system.domain.BookChapter chapter = new com.backstage.system.domain.BookChapter();
        chapter.setIsFree(0);

        ThreadLocalUtil.set(OshUserConstants.USER_ID, 3L);
        ThreadLocalUtil.set(OshUserConstants.LEVEL, 6);
        when(relationMapper.selectByUserIdAndBookId(3L, 7L)).thenReturn(null);

        Boolean readable = ReflectionTestUtils.invokeMethod(service, "canReadChapter", book, chapter);

        assertFalse(Boolean.TRUE.equals(readable));
    }

    @Test
    void shouldAllowPurchasedVipPaidBookChapter() {
        UserBookRelationMapper relationMapper = Mockito.mock(UserBookRelationMapper.class);
        BookServiceImpl service = new BookServiceImpl();
        ReflectionTestUtils.setField(service, "userBookRelationMapper", relationMapper);

        BookDO book = new BookDO();
        book.setId(7L);
        book.setLevel(2);
        book.setPrice(BigDecimal.TEN);

        com.backstage.system.domain.BookChapter chapter = new com.backstage.system.domain.BookChapter();
        chapter.setIsFree(0);

        UserBookRelation relation = new UserBookRelation();
        relation.setPurchased(1);
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 3L);
        ThreadLocalUtil.set(OshUserConstants.LEVEL, 1);
        when(relationMapper.selectByUserIdAndBookId(3L, 7L)).thenReturn(relation);

        Boolean readable = ReflectionTestUtils.invokeMethod(service, "canReadChapter", book, chapter);

        assertTrue(Boolean.TRUE.equals(readable));
    }

    @Test
    void shouldSanitizeChapterContentAndComicPagesFromBookDetail() {
        BookMapper bookMapper = Mockito.mock(BookMapper.class);
        BookChapterMapper chapterMapper = Mockito.mock(BookChapterMapper.class);
        BookTagDOMapper tagMapper = Mockito.mock(BookTagDOMapper.class);
        BookServiceImpl service = newBookServiceForCatalog(bookMapper, chapterMapper, tagMapper);

        BookChapterVO chapter = chapterWithSensitiveContent();
        when(bookMapper.selectReadableById(7L)).thenReturn(comicBook());
        when(chapterMapper.selectBookChapterListByBookId(7L)).thenReturn(Collections.singletonList(chapter));
        when(tagMapper.selectBookTagListByBookId(7L)).thenReturn(Collections.emptyList());

        BookDetailVO detail = service.selectBookDetail(7L, false);

        assertEquals(1, detail.getBookDetails().size());
        assertNull(detail.getBookDetails().get(0).getContent());
        assertNull(detail.getBookDetails().get(0).getPages());
    }

    @Test
    void shouldSanitizeChapterContentAndComicPagesFromBookMenu() {
        BookMapper bookMapper = Mockito.mock(BookMapper.class);
        BookChapterMapper chapterMapper = Mockito.mock(BookChapterMapper.class);
        BookTagDOMapper tagMapper = Mockito.mock(BookTagDOMapper.class);
        BookServiceImpl service = newBookServiceForCatalog(bookMapper, chapterMapper, tagMapper);

        BookChapterVO chapter = chapterWithSensitiveContent();
        when(bookMapper.selectReadableById(7L)).thenReturn(comicBook());
        when(chapterMapper.selectBookChapterListByBookId(7L)).thenReturn(Collections.singletonList(chapter));

        BookMenuVO menu = service.selectBookMenu(7L);

        assertEquals(1, menu.getMenus().size());
        assertNull(menu.getMenus().get(0).getContent());
        assertNull(menu.getMenus().get(0).getPages());
    }

    private BookServiceImpl newBookServiceForCatalog(BookMapper bookMapper,
                                                     BookChapterMapper chapterMapper,
                                                     BookTagDOMapper tagMapper) {
        BookServiceImpl service = new BookServiceImpl();
        ReflectionTestUtils.setField(service, "bookMapper", bookMapper);
        ReflectionTestUtils.setField(service, "bookChapterMapper", chapterMapper);
        ReflectionTestUtils.setField(service, "bookTagDOMapper", tagMapper);
        ReflectionTestUtils.setField(service, "asyncTaskSupport", new AsyncTaskSupport());
        ReflectionTestUtils.setField(service, "aggregationTaskExecutor", (Executor) Runnable::run);
        return service;
    }

    private BookDO comicBook() {
        BookDO book = new BookDO();
        book.setId(7L);
        book.setTitle("Paid Comic");
        book.setLevel(2);
        book.setPrice(BigDecimal.TEN);
        book.setMediaType(2);
        return book;
    }

    private BookChapterVO chapterWithSensitiveContent() {
        BookChapterVO chapter = new BookChapterVO();
        chapter.setId(11L);
        chapter.setTitle("Chapter 1");
        chapter.setContent("secret text");

        BookChapterPageVO page = new BookChapterPageVO();
        page.setImageUrl("private/comic/page-1.png");
        chapter.setPages(Collections.singletonList(page));
        return chapter;
    }
}
