package com.backstage.system.service.impl.book;

import cn.hutool.core.util.StrUtil;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.book.BookBookmarkDO;
import com.backstage.system.domain.vo.book.BookBookmarkCreateReqVO;
import com.backstage.system.domain.vo.book.BookBookmarkVO;
import com.backstage.system.mapper.book.BookBookmarkMapper;
import com.backstage.system.service.book.BookBookmarkService;
import com.backstage.system.service.book.IBookService;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookBookmarkServiceImpl implements BookBookmarkService {

    private static final int MAX_SELECTED_TEXT_LENGTH = 2000;
    private static final int MAX_CONTEXT_LENGTH = 500;
    private static final int MAX_NOTE_LENGTH = 500;
    private static final String DEFAULT_COLOR = "#fef08a";

    @Resource
    private BookBookmarkMapper bookBookmarkMapper;

    @Resource
    private IBookService bookService;

    @Override
    public Long createBookmark(BookBookmarkCreateReqVO reqVO) {
        Long userId = requireLoginUser();
        bookService.selectBookChapterContent(reqVO.getBookId(), reqVO.getChapterId());

        BookBookmarkDO bookmark = new BookBookmarkDO();
        bookmark.setUserId(userId);
        bookmark.setBookId(reqVO.getBookId());
        bookmark.setChapterId(reqVO.getChapterId());
        bookmark.setChapterTitle(limit(reqVO.getChapterTitle(), 200));
        bookmark.setSelectedText(limit(reqVO.getSelectedText(), MAX_SELECTED_TEXT_LENGTH));
        bookmark.setPrefixText(limit(reqVO.getPrefixText(), MAX_CONTEXT_LENGTH));
        bookmark.setSuffixText(limit(reqVO.getSuffixText(), MAX_CONTEXT_LENGTH));
        bookmark.setStartOffset(reqVO.getStartOffset());
        bookmark.setEndOffset(reqVO.getEndOffset());
        bookmark.setProgressPercent(normalizeProgress(reqVO.getProgressPercent()));
        bookmark.setNote(limit(reqVO.getNote(), MAX_NOTE_LENGTH));
        bookmark.setColor(normalizeColor(reqVO.getColor()));
        bookmark.setCreateBy(userId);
        bookmark.setUpdateBy(userId);
        bookBookmarkMapper.insert(bookmark);
        return bookmark.getId();
    }

    @Override
    public List<BookBookmarkVO> listBookmarks(Long bookId, Long chapterId) {
        Long userId = requireLoginUser();
        LambdaQueryWrapper<BookBookmarkDO> wrapper = new LambdaQueryWrapper<BookBookmarkDO>()
                .eq(BookBookmarkDO::getUserId, userId)
                .eq(BookBookmarkDO::getBookId, bookId);
        if (chapterId != null) {
            wrapper.eq(BookBookmarkDO::getChapterId, chapterId);
        }
        wrapper.orderByDesc(BookBookmarkDO::getCreateTime).orderByDesc(BookBookmarkDO::getId);
        return bookBookmarkMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteBookmark(Long id) {
        Long userId = requireLoginUser();
        BookBookmarkDO bookmark = bookBookmarkMapper.selectById(id);
        if (bookmark == null || !userId.equals(bookmark.getUserId())) {
            throw new ServiceException("书签不存在");
        }
        bookBookmarkMapper.deleteById(id);
    }

    private BookBookmarkVO toVO(BookBookmarkDO bookmark) {
        BookBookmarkVO vo = new BookBookmarkVO();
        BeanUtils.copyProperties(bookmark, vo);
        return vo;
    }

    private Long requireLoginUser() {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            throw new ServiceException("请先登录后再使用书签功能");
        }
        return userId;
    }

    private String normalizeColor(String color) {
        if (StrUtil.isBlank(color)) {
            return DEFAULT_COLOR;
        }
        String trimmed = color.trim();
        if (!trimmed.matches("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$")) {
            return DEFAULT_COLOR;
        }
        return trimmed;
    }

    private BigDecimal normalizeProgress(BigDecimal progressPercent) {
        if (progressPercent == null) {
            return BigDecimal.ZERO;
        }
        if (progressPercent.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal max = BigDecimal.valueOf(100);
        if (progressPercent.compareTo(max) > 0) {
            return max;
        }
        return progressPercent;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
