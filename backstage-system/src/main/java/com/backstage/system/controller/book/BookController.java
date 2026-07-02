package com.backstage.system.controller.book;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshResourceId;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.domain.R;
import com.backstage.common.response.PageResponse;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.vo.book.BookChapterContentVO;
import com.backstage.system.domain.vo.book.BookChapterSaveUpdateVO;
import com.backstage.system.domain.vo.book.BookDetailVO;
import com.backstage.system.domain.vo.book.BookListVO;
import com.backstage.system.domain.vo.book.BookMenuVO;
import com.backstage.system.domain.vo.book.BookSaveReqVO;
import com.backstage.system.enums.behavior.ContributionResourceType;
import com.backstage.system.service.behavior.ContributionService;
import com.backstage.system.service.book.IBookEsService;
import com.backstage.system.service.book.IBookService;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * 电子书 Controller
 *
 * @author backstage
 */
@RestController
@RequestMapping("/pc/book")
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    @Resource
    private IBookService bookService;

    @Autowired
    private IBookEsService bookEsService;

    @Autowired
    private SearchEsProperties searchEsProperties;

    @Resource
    private ContributionService contributionService;

    /**
     * 电子书列表
     */
    @ApiOperation(value = "电子书列表")
    @OshUserEvent(module = "电子书模块", actionType = "查询", resourceType = ResourceType.BOOK_TYPE, description = "查询电子书列表")
    @Anonymous
    @PostMapping("/page")
    public R<Page<BookListVO>> list(@RequestBody BookListReqVO reqVO) {
        reqVO.setUserLevel(UserContextUtil.getCurrentLevel());
        reqVO.setUserId(UserContextUtil.getCurrentUserIdSafely());
        if (searchEsProperties.isEnabled()) {
            try {
                log.info("使用 ES 查询电子书");
                return R.ok(bookEsService.searchBooks(reqVO));
            } catch (Exception ex) {
                log.warn("book search fallback to mysql after es failure, reqVO={}", reqVO, ex);
            }
        }
        Page<BookListVO> pageResult = bookService.getBookPageList(reqVO);
        return R.ok(pageResult);
    }

    /**
     * 电子书搜索
     */
    @ApiOperation(value = "电子书搜索")
    @PostMapping("/search")
    @OshUserEvent(module = "电子书模块", actionType = "搜索", resourceType = ResourceType.BOOK_TYPE, description = "搜索电子书", recordAnonymous = true)
    @Anonymous
    public R<PageResponse<BookListVO>> search(@RequestBody BookListReqVO reqVO) {
        reqVO.setUserId(UserContextUtil.getCurrentUserIdSafely());
        reqVO.setUserLevel(UserContextUtil.getCurrentLevelSafely());
        Page<BookListVO> pageResult = bookService.getBookPageList(reqVO);
        int pageNum = reqVO.getPageNum() == null ? 1 : reqVO.getPageNum().intValue();
        int pageSize = reqVO.getPageSize() == null ? 12 : reqVO.getPageSize().intValue();
        return R.ok(PageResponse.of(pageResult.getRecords(), pageResult.getTotal(), pageNum, pageSize), "ok");
    }

    /**
     * 查看电子书详情
     */
    @ApiOperation(value = "电子书详情")
    @OshUserEvent(module = "电子书模块", actionType = "浏览", resourceType = ResourceType.BOOK_TYPE, description = "浏览电子书详情")
    @GetMapping("/getById")
    public R<BookDetailVO> getById(@OshResourceId @RequestParam Long id, @RequestParam(required = false, defaultValue = "false") Boolean forEdit) {
        BookDetailVO detail = bookService.selectBookDetail(id, forEdit);
        return R.ok(detail);
    }

    /**
     * 查看电子书章节内容
     */
    @ApiOperation(value = "章节内容详情")
    @OshUserEvent(module = "电子书模块", actionType = "学习", resourceType = ResourceType.BOOK_TYPE, description = "查询章节内容")
    @GetMapping("/detail")
    public R<BookChapterContentVO> detail(@OshResourceId @RequestParam Long book_id, @RequestParam Long id) {
        BookChapterContentVO content = bookService.selectBookChapterContent(book_id, id);
        return R.ok(content);
    }

    /**
     * 查看电子书章节菜单
     */
    @ApiOperation(value = "章节菜单")
    @OshUserEvent(module = "电子书模块", actionType = "查询", resourceType = ResourceType.BOOK_TYPE, description = "查询章节菜单")
    @GetMapping("/menus")
    public R<BookMenuVO> menus(@OshResourceId @RequestParam Long id) {
        BookMenuVO menu = bookService.selectBookMenu(id);
        return R.ok(menu);
    }

    /**
     * 新增电子书
     */
    @ApiOperation(value = "新增电子书")
    @OshUserEvent(module = "电子书模块", actionType = "新增", resourceType = ResourceType.BOOK_TYPE, resourceNameExpression = "#p0.title", description = "创建电子书")
    @OshUserLevel(4)
    @PostMapping("/create")
    public R<Long> create(@Valid @RequestBody BookSaveReqVO reqVO) {
        Long bookId = bookService.createBook(reqVO);
        contributionService.recordContribution(ContributionResourceType.BOOK.getCode(), bookId, reqVO.getTitle());
        return R.ok(bookId, "创建成功");
    }

    /**
     * 修改电子书
     */
    @ApiOperation(value = "修改电子书")
    @OshUserEvent(module = "电子书模块", actionType = "修改", resourceType = ResourceType.BOOK_TYPE, resourceNameExpression = "#p0.title", description = "更新电子书")
    @OshUserLevel(4)
    @PostMapping("/update")
    public R<String> update(@Valid @RequestBody BookSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            return R.fail("电子书ID不能为空");
        }
        bookService.updateBook(reqVO);
        return R.ok("修改成功");
    }

    /**
     * 删除电子书
     */
    @ApiOperation(value = "删除电子书")
    @OshUserEvent(module = "电子书模块", actionType = "删除", resourceType = ResourceType.BOOK_TYPE, description = "删除电子书")
    @OshUserLevel(4)
    @DeleteMapping("/delete")
    public R<String> delete(@OshResourceId @RequestParam("id") Long id) {
        bookService.deleteBook(id);
        return R.ok("删除成功");
    }

    /**
     * 恢复电子书
     */
    @ApiOperation(value = "恢复电子书")
    @OshUserEvent(module = "电子书模块", actionType = "恢复", resourceType = ResourceType.BOOK_TYPE, description = "恢复逻辑删除的电子书")
    @OshUserLevel(6)
    @PostMapping("/restore")
    public R<String> restore(@OshResourceId @RequestParam("id") Long id) {
        bookService.restoreBook(id);
        return R.ok("恢复成功");
    }

    /**
     * 查询所有电子书标签列表
     */
    @ApiOperation(value = "标签列表")
    @OshUserEvent(module = "电子书模块", actionType = "查询", resourceType = ResourceType.BOOK_TYPE, description = "查询标签列表", recordAnonymous = true)
    @Anonymous
    @GetMapping("/getTagList")
    public R<List<String>> getTagList() {
        return R.ok(bookService.getTagList());
    }

    /**
     * 新增电子书章节
     */
    @ApiOperation(value = "新增章节")
    @OshUserEvent(module = "电子书模块", actionType = "新增", resourceType = ResourceType.BOOK_TYPE, description = "创建章节")
    @OshUserLevel(4)
    @PostMapping("/chapter/create")
    public R<String> createBookChapter(@RequestBody BookChapterSaveUpdateVO reqVO) {
        if (reqVO.getBookId() == null) {
            return R.fail("电子书ID不能为空");
        }
        bookService.createBookChapter(reqVO);
        return R.ok("创建成功");
    }

    /**
     * 修改电子书章节
     */
    @ApiOperation(value = "修改章节")
    @OshUserEvent(module = "电子书模块", actionType = "修改", resourceType = ResourceType.BOOK_TYPE, description = "更新章节")
    @OshUserLevel(4)
    @PostMapping("/chapter/update")
    public R<String> updateBookChapter(@RequestBody BookChapterSaveUpdateVO reqVO) {
        if (reqVO.getId() == null) {
            return R.fail("章节ID不能为空");
        }
        if (reqVO.getBookId() == null) {
            return R.fail("电子书ID不能为空");
        }
        bookService.updateBookChapter(reqVO);
        return R.ok("修改成功");
    }

    /**
     * 筛选电子书列表
     */
    @ApiOperation(value = "筛选电子书")
    @OshUserEvent(module = "电子书模块", actionType = "查询", resourceType = ResourceType.BOOK_TYPE, description = "筛选电子书")
    @OshUserLevel(4)
    @GetMapping("/getFilterBookList")
    public R<Page<BookListVO>> getFilterBookList(@RequestParam String filter) {
        Page<BookListVO> bookList = bookService.getFilterBookList(filter);
        return R.ok(bookList);
    }

    /**
     * 全量同步电子书到 ES
     */
    @ApiOperation(value = "全量同步电子书到ES")
    @PostMapping("/esSync/all")
    @OshUserEvent(module = "电子书模块", actionType = "更新资源", resourceType = ResourceType.BOOK_TYPE, description = "同步电子书到ES")
    @OshUserLevel(4)
    public R<Integer> syncAllBooksToEs() {
        return R.ok(bookEsService.syncAllBooksToEs(), "ok");
    }

    @ApiOperation(value = "Ebook import placeholder")
    @PostMapping("/import/ebook")
    @OshUserLevel(4)
    public R<String> importEbookPlaceholder(@RequestBody(required = false) Object payload) {
        return R.fail("ebook import is not implemented yet");
    }
}
