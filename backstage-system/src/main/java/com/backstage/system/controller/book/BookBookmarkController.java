package com.backstage.system.controller.book;

import com.backstage.common.annotation.OshResourceId;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.vo.book.BookBookmarkCreateReqVO;
import com.backstage.system.domain.vo.book.BookBookmarkVO;
import com.backstage.system.service.book.BookBookmarkService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/pc/book/bookmark")
public class BookBookmarkController {

    @Resource
    private BookBookmarkService bookBookmarkService;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    @OshUserEvent(module = "电子书模块", actionType = "新增书签", resourceType = ResourceType.BOOK_TYPE, description = "新增电子书书签")
    public R<Long> create(@Valid @RequestBody BookBookmarkCreateReqVO reqVO) {
        return R.ok(bookBookmarkService.createBookmark(reqVO), "书签已添加");
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    @OshUserEvent(module = "电子书模块", actionType = "查询书签", resourceType = ResourceType.BOOK_TYPE, description = "查询电子书书签")
    public R<List<BookBookmarkVO>> list(@OshResourceId @RequestParam Long bookId,
                                        @RequestParam(required = false) Long chapterId) {
        return R.ok(bookBookmarkService.listBookmarks(bookId, chapterId));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    @OshUserEvent(module = "电子书模块", actionType = "删除书签", resourceType = ResourceType.BOOK_TYPE, description = "删除电子书书签")
    public R<String> delete(@RequestParam Long id) {
        bookBookmarkService.deleteBookmark(id);
        return R.ok("删除成功");
    }
}
