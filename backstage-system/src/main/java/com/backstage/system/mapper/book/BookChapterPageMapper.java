package com.backstage.system.mapper.book;

import com.backstage.system.domain.book.BookChapterPageDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BookChapterPageMapper extends BaseMapper<BookChapterPageDO> {
    @Delete("DELETE FROM osh_book_chapter_page WHERE book_id = #{bookId} AND chapter_id = #{chapterId}")
    int physicalDeleteByBookAndChapter(@Param("bookId") Long bookId, @Param("chapterId") Long chapterId);

    @Delete("<script>" +
            "DELETE FROM osh_book_chapter_page WHERE book_id = #{bookId} AND chapter_id IN " +
            "<foreach collection='chapterIds' item='chapterId' open='(' separator=',' close=')'>#{chapterId}</foreach>" +
            "</script>")
    int physicalDeleteByBookAndChapterIds(@Param("bookId") Long bookId, @Param("chapterIds") List<Long> chapterIds);

    @Delete("DELETE FROM osh_book_chapter_page WHERE book_id = #{bookId}")
    int physicalDeleteByBook(@Param("bookId") Long bookId);
}
