package com.backstage.system.service.book;

import com.backstage.system.domain.vo.book.BookBookmarkCreateReqVO;
import com.backstage.system.domain.vo.book.BookBookmarkVO;

import java.util.List;

public interface BookBookmarkService {
    Long createBookmark(BookBookmarkCreateReqVO reqVO);

    List<BookBookmarkVO> listBookmarks(Long bookId, Long chapterId);

    void deleteBookmark(Long id);
}
