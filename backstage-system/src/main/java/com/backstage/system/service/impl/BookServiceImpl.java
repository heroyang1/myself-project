package com.backstage.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.backstage.common.enums.AnnouncementModuleEnum;
import com.backstage.common.enums.ResourceCodePrefixEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.async.AsyncExecutorNames;
import com.backstage.common.async.AsyncTaskSupport;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.common.utils.StringUtils;
import com.backstage.common.utils.generate.GenerateUtil;
import com.backstage.system.controller.book.BookListReqVO;
import com.backstage.system.domain.book.BookChapterPageDO;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.BookChapter;
import com.backstage.system.domain.UserBookRelation;
import com.backstage.system.domain.book.BookTagDO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.book.*;
import com.backstage.system.enums.book.BookMediaTypeEnum;
import com.backstage.system.mapper.book.BookChapterMapper;
import com.backstage.system.mapper.book.BookChapterPageMapper;
import com.backstage.system.mapper.book.BookMapper;
import com.backstage.system.mapper.book.UserBookRelationMapper;
import com.backstage.system.mapper.book.BookTagDOMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.mapper.user.UserManageMapper;
import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.mapper.announcement.OshAnnouncementMapper;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.service.announcement.AnnouncementRefreshBroadcaster;
import com.backstage.system.service.book.BookChapterService;
import com.backstage.system.service.book.IBookService;
import com.backstage.system.service.order.OrderCheckoutService;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 电子书 服务层实现
 *
 * @author backstage
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, BookDO> implements IBookService
{
    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

    @Resource
    private BookMapper bookMapper;

    @Resource
    private BookChapterMapper bookChapterMapper;

    @Resource
    private BookChapterPageMapper bookChapterPageMapper;

    @Resource
    private BookChapterService bookChapterService;

    @Resource
    private UserBookRelationMapper userBookRelationMapper;

    @Resource
    private BookTagDOMapper bookTagDOMapper;

    @Resource
    private OshUserMapper oshUserMapper;

    @Resource
    private UserManageMapper userManageMapper;

    @Autowired
    private com.backstage.system.service.common.OssService ossService;

    @Autowired
    private com.backstage.system.service.book.IBookEsService bookEsService;

    @Autowired
    private AsyncTaskSupport asyncTaskSupport;

    @Resource(name = AsyncExecutorNames.AGGREGATION)
    private Executor aggregationTaskExecutor;

    @Autowired
    private OrderCheckoutService orderCheckoutService;

    @Resource
    private OshOrderMapper oshOrderMapper;

    @Resource
    private OshAnnouncementMapper oshAnnouncementMapper;

    @Resource
    private AnnouncementRefreshBroadcaster announcementRefreshBroadcaster;

    /**
     * 查询电子书列表
     *
     * @param book 电子书
     * @return 电子书集合
     */
    /**
     * 查询电子书列表
     *
     * @param reqVO 电子书
     * @return 电子书集合
     */

    @Override
    public Page<BookListVO> getBookPageList(BookListReqVO reqVO) {
        int currentLevel = UserContextUtil.getCurrentLevelSafely();
        if (currentLevel < 1) {
            currentLevel = 1;
        }
        if (Boolean.TRUE.equals(reqVO.getIncludeDeleted()) && currentLevel < 4) {
            reqVO.setIncludeDeleted(false);
        }
        Page<BookDO> pageParam = new Page<>(reqVO.getPageNum(), reqVO.getPageSize());
        List<BookListVO> bookListVOS = bookMapper.getBookPageList(pageParam, reqVO);

        // tagNames 逗号字符串 -> tagNameList 数组
        for (BookListVO vo : bookListVOS) {
            vo.setMediaType(BookMediaTypeEnum.normalize(vo.getMediaType()));
            if (StringUtils.isNotEmpty(vo.getTagNames())) {
                vo.setTagNameList(Arrays.asList(vo.getTagNames().split(",")));
            }
            // 处理封面URL - 生成临时访问链接（30分钟有效期）
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossService.getLimitedUrl(vo.getCover(), 30));
            }
        }

        Page<BookListVO> result = new Page<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal());
        result.setRecords(bookListVOS);
        return result;
    }

    // 获取筛选的电子书列表
    @Override
    public Page<BookListVO> getFilterBookList(String filter) {
        // 分页：第1页，每页12条
        Page<BookDO> pageParam = new Page<>(1, 12);

        Page<BookListVO> voPage = bookMapper.selectFreeBookPage(pageParam);

        // SQL 查出的逗号分隔 tagNames → 转成 List<String>
        for (BookListVO vo : voPage.getRecords()) {
            vo.setMediaType(BookMediaTypeEnum.normalize(vo.getMediaType()));
            if (StrUtil.isNotBlank(vo.getTagNames())) {
                List<String> tagList = Arrays.asList(vo.getTagNames().split(","));
                vo.setTagNameList(tagList);
            }
            // 处理封面URL - 生成临时访问链接（30分钟有效期）
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossService.getLimitedUrl(vo.getCover(), 30));
            }
        }
        return voPage;
    }


    @Override
    public List<String> getTagList() {
        return bookTagDOMapper.getTagList();
    }

    /**
     * 查询电子书详情
     *
     * @param id 电子书ID
     * @param forEdit 是否用于编辑（true时返回原始相对路径，false时返回临时访问URL）
     * @return 电子书详情
     */
    @Override
    public BookDetailVO selectBookDetail(Long id, Boolean forEdit) {
        if (Boolean.TRUE.equals(forEdit)) {
            requireManageLevel();
        }
        BookDetailAggregate aggregate = loadBookDetailAggregate(id, UserContextUtil.getCurrentUserIdSafely());
        return buildBookDetailVO(aggregate, forEdit);
    }

    /**
     * 查询电子书章节内容
     *
     * @param bookId 电子书ID
     * @param id 章节ID
     * @return 章节内容
     */
    @Override
    public BookChapterContentVO selectBookChapterContent(Long bookId, Long id) {
        BookDO bookDO = requireReadableBook(bookId);
        BookChapter chapter = bookChapterMapper.selectBookChapterByBookIdAndId(bookId, id);
        checkEntityNotNull(chapter, "record not found");
        if (!canReadChapter(bookDO, chapter)) {
            throw new ServiceException("access denied");
        }

        BookChapterContentVO vo = new BookChapterContentVO();
        BeanUtils.copyProperties(chapter, vo);
        vo.setIsFree(chapter.getIsFree());
        vo.setMediaType(BookMediaTypeEnum.normalize(bookDO.getMediaType()));
        if (BookMediaTypeEnum.isComic(bookDO.getMediaType())) {
            vo.setPages(listComicPages(bookId, id, false));
        }
        return vo;
    }

    private boolean canReadChapter(BookDO bookDO, BookChapter chapter) {
        if (Integer.valueOf(1).equals(chapter.getIsFree())) {
            return true;
        }
        int bookLevel = Optional.ofNullable(bookDO.getLevel()).orElse(1);
        if (bookLevel <= 1) {
            return true;
        }
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (isVipPaidBook(bookDO)) {
            return checkUserHasBought(userId, bookDO.getId());
        }
        int currentLevel = UserContextUtil.getCurrentLevelSafely();
        if (currentLevel >= bookLevel) {
            return true;
        }
        return checkUserHasBought(userId, bookDO.getId());
    }

    private boolean isVipPaidBook(BookDO bookDO) {
        return Integer.valueOf(2).equals(bookDO.getLevel())
                && Optional.ofNullable(bookDO.getPrice()).orElse(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0;
    }

    private List<BookChapterPageVO> listComicPages(Long bookId, Long chapterId, Boolean forEdit) {
        List<BookChapterPageDO> rows = bookChapterPageMapper.selectList(
                new LambdaQueryWrapper<BookChapterPageDO>()
                        .eq(BookChapterPageDO::getBookId, bookId)
                        .eq(chapterId != null, BookChapterPageDO::getChapterId, chapterId)
                        .orderByAsc(BookChapterPageDO::getChapterId)
                        .orderByAsc(BookChapterPageDO::getPageNo)
                        .orderByAsc(BookChapterPageDO::getId)
        );
        return rows.stream().map(row -> toComicPageVO(row, forEdit)).collect(Collectors.toList());
    }

    private Map<Long, List<BookChapterPageVO>> listComicPagesByChapter(Long bookId, Boolean forEdit) {
        Map<Long, List<BookChapterPageVO>> result = new HashMap<>();
        for (BookChapterPageVO page : listComicPages(bookId, null, forEdit)) {
            if (page.getChapterId() == null) {
                continue;
            }
            result.computeIfAbsent(page.getChapterId(), key -> new ArrayList<>()).add(page);
        }
        return result;
    }

    private BookChapterPageVO toComicPageVO(BookChapterPageDO row, Boolean forEdit) {
        BookChapterPageVO vo = new BookChapterPageVO();
        BeanUtils.copyProperties(row, vo);
        vo.setImageUrl(resolveBookAssetUrl(row.getImageUrl(), forEdit));
        return vo;
    }

    private String resolveBookAssetUrl(String url, Boolean forEdit) {
        if (StringUtils.isEmpty(url) || Boolean.TRUE.equals(forEdit) || url.startsWith("http") || url.startsWith("data:")) {
            return url;
        }
        return ossService.getLimitedUrl(url, 120);
    }

    private void attachComicPages(List<BookChapterVO> chapters, Long bookId, Boolean forEdit) {
        if (chapters == null || chapters.isEmpty()) {
            return;
        }
        Map<Long, List<BookChapterPageVO>> pageMap = listComicPagesByChapter(bookId, forEdit);
        for (BookChapterVO chapter : chapters) {
            chapter.setPages(pageMap.getOrDefault(chapter.getId(), Collections.emptyList()));
        }
    }

    private void replaceComicPages(Long bookId, Long chapterId, List<BookChapterPageVO> pages) {
        bookChapterPageMapper.physicalDeleteByBookAndChapter(bookId, chapterId);
        if (pages == null || pages.isEmpty()) {
            return;
        }
        int pageNo = 1;
        for (BookChapterPageVO page : pages) {
            if (StringUtils.isEmpty(page.getImageUrl())) {
                continue;
            }
            BookChapterPageDO row = new BookChapterPageDO();
            BeanUtils.copyProperties(page, row);
            row.setId(null);
            row.setBookId(bookId);
            row.setChapterId(chapterId);
            row.setPageNo(pageNo++);
            row.setImageUrl(normalizeStoredAssetUrl(page.getImageUrl()));
            bookChapterPageMapper.insert(row);
        }
    }

    private String normalizeStoredAssetUrl(String url) {
        if (StringUtils.isEmpty(url) || !url.startsWith("http")) {
            return url;
        }
        try {
            java.net.URL parsed = new java.net.URL(url);
            String path = parsed.getPath();
            if (StringUtils.isEmpty(path)) {
                return url;
            }
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            int firstSlash = normalized.indexOf('/');
            return firstSlash >= 0 ? normalized.substring(firstSlash + 1) : normalized;
        } catch (Exception ignored) {
            return url;
        }
    }

    private void deleteComicPagesByBook(Long bookId) {
        bookChapterPageMapper.physicalDeleteByBook(bookId);
    }

    /**
     * 查询电子书章节菜单
     *
     * @param id 电子书ID
     * @return 章节菜单
     */
    @Override
    public BookMenuVO selectBookMenu(Long id) {
        CompletableFuture<BookDO> bookFuture = asyncTaskSupport.supplyAsync(() -> {
            return requireReadableBook(id);
        }, aggregationTaskExecutor);
        CompletableFuture<List<BookChapterVO>> chapterFuture = asyncTaskSupport.supplyAsync(
                () -> bookChapterMapper.selectBookChapterListByBookId(id), aggregationTaskExecutor);

        asyncTaskSupport.awaitAll(bookFuture, chapterFuture);

        BookDO bookDO = asyncTaskSupport.join(bookFuture);
        List<BookChapterVO> chapters = sanitizeChapterCatalog(asyncTaskSupport.join(chapterFuture));

        BookMenuVO vo = new BookMenuVO();

        // 设置电子书基本信息
        BookSimpleVO simpleVO = new BookSimpleVO();
        BeanUtils.copyProperties(bookDO, simpleVO);
        simpleVO.setMediaType(BookMediaTypeEnum.normalize(bookDO.getMediaType()));
        if (StringUtils.isNotEmpty(simpleVO.getCover())) {
            simpleVO.setCover(ossService.getLimitedUrl(simpleVO.getCover(), 30));
        }
        vo.setDetail(simpleVO);

        vo.setMenus(chapters);

        return vo;
    }

    /**
     * 查询用户购买的电子书列表
     *
     * @param userId 用户ID
     * @return 电子书集合
     */
    @Override
    public List<BookDO> selectUserBookList(Long userId)
    {
        return userBookRelationMapper.selectUserBookList(userId);
    }

    /**
     * 分页查询用户购买的电子书列表
     *
     * @param userId 用户ID
     * @param page 分页参数
     * @return 电子书分页集合
     */
    @Override
    public Page<BookDO> selectUserBookListPage(Long userId, Page<BookDO> page) {
        return userBookRelationMapper.selectUserBookListPage(userId, page);
    }

    @Override
    @Transactional
    public void createBookChapter(BookChapterSaveUpdateVO reqVO) {
        requireManageLevel();
        BookDO bookDO = getById(reqVO.getBookId());
        checkEntityNotNull(bookDO, "电子书不存在");

        BookChapter bookChapter = new BookChapter();
        BeanUtils.copyProperties(reqVO, bookChapter);

        Integer chapterNo = reqVO.getChapterNo();
        if (chapterNo == null) {
            Integer maxChapterNo = bookChapterMapper.selectMaxChapterNoByBookId(reqVO.getBookId());
            chapterNo = maxChapterNo == null ? 1 : maxChapterNo + 1;
        }
        if (chapterNo < 1) {
            throw new ServiceException("章节序号不能小于1");
        }
        bookChapter.setChapterNo(chapterNo);
        bookChapter.setSortOrder(reqVO.getSortOrder() == null ? chapterNo : reqVO.getSortOrder());
        bookChapter.setIsFree(reqVO.getIsFree());
        if (bookChapter.getSortOrder() < 1) {
            throw new ServiceException("排序值不能小于1");
        }

        bookChapterMapper.insert(bookChapter);
        reqVO.setId(bookChapter.getId());
        if (BookMediaTypeEnum.isComic(bookDO.getMediaType())) {
            replaceComicPages(reqVO.getBookId(), bookChapter.getId(), reqVO.getPages());
        }
    }

    @Override
    @Transactional
    public void updateBookChapter(BookChapterSaveUpdateVO reqVO) {
        requireManageLevel();
        BookDO bookDO = getById(reqVO.getBookId());
        checkEntityNotNull(bookDO, "电子书不存在");

        BookChapter bookChapter = bookChapterMapper.selectBookChapterByBookIdAndId(reqVO.getBookId(), reqVO.getId());
        checkEntityNotNull(bookChapter, "chapter not found");

        if (reqVO.getTitle() != null) {
            bookChapter.setTitle(reqVO.getTitle());
        }
        if (reqVO.getContent() != null) {
            bookChapter.setContent(reqVO.getContent());
        }
        if (reqVO.getChapterNo() != null) {
            if (reqVO.getChapterNo() < 1) {
                throw new ServiceException("章节序号不能小于1");
            }
            bookChapter.setChapterNo(reqVO.getChapterNo());
        }
        if (reqVO.getSortOrder() != null) {
            if (reqVO.getSortOrder() < 1) {
                throw new ServiceException("排序值不能小于1");
            }
            bookChapter.setSortOrder(reqVO.getSortOrder());
        }
        if (reqVO.getIsFree() != null) {
            bookChapter.setIsFree(reqVO.getIsFree());
        }

        if (bookChapter.getSortOrder() == null && bookChapter.getChapterNo() != null) {
            bookChapter.setSortOrder(bookChapter.getChapterNo());
        }

        bookChapterMapper.updateById(bookChapter);
        if (BookMediaTypeEnum.isComic(bookDO.getMediaType())) {
            replaceComicPages(reqVO.getBookId(), reqVO.getId(), reqVO.getPages());
        } else {
            bookChapterPageMapper.physicalDeleteByBookAndChapter(reqVO.getBookId(), reqVO.getId());
        }
    }

    /**
     * 新增电子书
     *
     * @param reqVO 电子书请求VO
     * @return 电子书响应VO
     */
    @Override
    @Transactional
    public Long createBook(BookSaveReqVO reqVO) {
        requireManageLevel();
        validateBookAccessLevel(reqVO.getLevel());

        BookDO bookDO = new BookDO();
        BeanUtils.copyProperties(reqVO, bookDO);
        bookDO.setNo(GenerateUtil.generateResourceCode(ResourceCodePrefixEnum.BOOK));
        bookDO.setStatus("0");
        if (bookDO.getLevel() == null) {
            bookDO.setLevel(1);
        }
        bookDO.setMediaType(BookMediaTypeEnum.normalize(reqVO.getMediaType()));
        normalizeBookPricing(bookDO, reqVO);
        // 新增电子书
        bookMapper.insert(bookDO);
        // 新增电子书标签表记录
        List<BookTagDO> bookTagDOS = packageBookTagInsertDOList(bookDO.getId(), reqVO);
        if (!bookTagDOS.isEmpty()) {
            bookTagDOMapper.insert(bookTagDOS);
        }
        // 批量新增章节
        batchInsertChapters(bookDO.getId(), reqVO.getChapters(), bookDO.getMediaType());
        // 同步ES
        bookEsService.syncBookToEs(bookDO.getId());
        publishBookCreateAnnouncement(bookDO);
        return bookDO.getId();
    }

    private void publishBookCreateAnnouncement(BookDO bookDO) {
        try {
            String typeName = BookMediaTypeEnum.isComic(bookDO.getMediaType()) ? "漫画" : "电子书";
            String iconCode = BookMediaTypeEnum.isComic(bookDO.getMediaType()) ? "comic" : "book";
            String title = "新增" + typeName + "《" + Optional.ofNullable(bookDO.getTitle()).orElse("未命名") + "》";
            oshAnnouncementMapper.insertBookAnnouncement(
                    title,
                    "/detail/book/" + bookDO.getId(),
                    iconCode,
                    2,
                    bookDO.getId(),
                    0,
                    String.valueOf(UserContextUtil.getCurrentUserIdSafely())
            );
            announcementRefreshBroadcaster.broadcastRefresh(AnnouncementModuleEnum.BOOK);
        } catch (Exception ex) {
            log.warn("publish book create announcement failed, bookId={}", bookDO == null ? null : bookDO.getId(), ex);
        }
    }

    /**
     * 封装标签数据
     *
     * @param reqVO 电子书DO
     */
    private List<BookTagDO> packageBookTagInsertDOList(Long id, BookSaveReqVO reqVO) {
        List<BookTagDO> insertList = new ArrayList<>();
        List<String> tags = reqVO.getTags() == null ? Collections.emptyList() : reqVO.getTags();
        for (int i = 0; i < tags.size(); i++) {
            BookTagDO bookTagInsertDO = new BookTagDO();
            bookTagInsertDO.setBookId(id);
            bookTagInsertDO.setTagName(tags.get(i));
            bookTagInsertDO.setSortOrder(i + 1);
            insertList.add(bookTagInsertDO);
        }
        return insertList;
    }

    /**
     * 批量插入章节
     *
     * @param bookId   电子书ID
     * @param chapters 章节列表
     */
    private void batchInsertChapters(Long bookId, List<BookChapterSaveUpdateVO> chapters, Integer mediaType) {
        if (chapters == null || chapters.isEmpty()) {
            return;
        }

        List<BookChapter> chapterList = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            BookChapterSaveUpdateVO chapterVO = chapters.get(i);
            BookChapter bookChapter = new BookChapter();
            BeanUtils.copyProperties(chapterVO, bookChapter);
            bookChapter.setBookId(bookId);

            // 处理章节号
            Integer chapterNo = chapterVO.getChapterNo();
            if (chapterNo == null) {
                chapterNo = i + 1;
            }
            if (chapterNo < 1) {
                throw new ServiceException("章节序号不能小于1");
            }
            bookChapter.setChapterNo(chapterNo);

            // 处理排序
            Integer sortOrder = chapterVO.getSortOrder();
            if (sortOrder == null) {
                sortOrder = chapterNo;
            }
            if (sortOrder < 1) {
                throw new ServiceException("排序值不能小于1");
            }
            bookChapter.setSortOrder(sortOrder);

            chapterList.add(bookChapter);
        }

        // 批量插入
        bookChapterService.saveBatch(chapterList);
        if (BookMediaTypeEnum.isComic(mediaType)) {
            for (int i = 0; i < chapterList.size(); i++) {
                BookChapter chapter = chapterList.get(i);
                BookChapterSaveUpdateVO chapterVO = chapters.get(i);
                chapterVO.setId(chapter.getId());
                replaceComicPages(bookId, chapter.getId(), chapterVO.getPages());
            }
        }
    }

    /**
     * 修改电子书
     *
     * @param reqVO 电子书请求VO
     * @return 电子书响应VO
     */
    @Override
    @Transactional
    public void updateBook(BookSaveReqVO reqVO)
    {
        requireManageLevel();
        validateBookAccessLevel(reqVO.getLevel());
        BookDO book = getById(reqVO.getId());
        checkEntityNotNull(book, "电子书不存在");
        BookDO bookDO = new BookDO();
        BeanUtils.copyProperties(reqVO, bookDO);
        if (bookDO.getLevel() == null) {
            bookDO.setLevel(1);
        }
        bookDO.setMediaType(BookMediaTypeEnum.normalize(reqVO.getMediaType() == null ? book.getMediaType() : reqVO.getMediaType()));
        normalizeBookPricing(bookDO, reqVO);
        bookMapper.updateById(bookDO);

        // 更新电子书标签
        // 先删除该电子书原有的标签
        bookTagDOMapper.delete(
                new LambdaQueryWrapper<BookTagDO>()
                        .eq(BookTagDO::getBookId, reqVO.getId())
        );
        // 再新增新的标签
        List<BookTagDO> bookTagDOS = packageBookTagInsertDOList(reqVO.getId(), reqVO);
        if (!bookTagDOS.isEmpty()) {
            bookTagDOMapper.insert(bookTagDOS);
        }

        syncBookChapters(reqVO.getId(), reqVO.getChapters(), bookDO.getMediaType());
        if (!BookMediaTypeEnum.isComic(bookDO.getMediaType())) {
            deleteComicPagesByBook(reqVO.getId());
        }

        // 同步ES
        bookEsService.syncBookToEs(reqVO.getId());
    }

    private void syncBookChapters(Long bookId, List<BookChapterSaveUpdateVO> chapters, Integer mediaType) {
        if (chapters == null) {
            return;
        }
        List<BookChapterVO> existingChapters = bookChapterMapper.selectBookChapterListByBookId(bookId);
        List<Long> existingIds = existingChapters.stream()
                .map(BookChapterVO::getId)
                .collect(Collectors.toList());
        List<Long> incomingIds = new ArrayList<>();
        boolean comicBook = BookMediaTypeEnum.isComic(mediaType);

        for (int i = 0; i < chapters.size(); i++) {
            BookChapterSaveUpdateVO chapterVO = chapters.get(i);
            chapterVO.setBookId(bookId);
            if (chapterVO.getChapterNo() == null || chapterVO.getChapterNo() < 1) {
                chapterVO.setChapterNo(i + 1);
            }
            if (chapterVO.getSortOrder() == null || chapterVO.getSortOrder() < 1) {
                chapterVO.setSortOrder(i + 1);
            }
            if (chapterVO.getIsFree() == null) {
                chapterVO.setIsFree(0);
            }

            if (chapterVO.getId() == null) {
                BookChapter bookChapter = new BookChapter();
                BeanUtils.copyProperties(chapterVO, bookChapter);
                bookChapter.setBookId(bookId);
                bookChapterMapper.insert(bookChapter);
                chapterVO.setId(bookChapter.getId());
                incomingIds.add(bookChapter.getId());
                if (comicBook) {
                    replaceComicPages(bookId, bookChapter.getId(), chapterVO.getPages());
                }
                continue;
            }

            if (!existingIds.contains(chapterVO.getId())) {
                throw new ServiceException("chapter not found");
            }
            BookChapter bookChapter = bookChapterMapper.selectBookChapterByBookIdAndId(bookId, chapterVO.getId());
            checkEntityNotNull(bookChapter, "chapter not found");
            bookChapter.setTitle(chapterVO.getTitle());
            bookChapter.setContent(chapterVO.getContent());
            bookChapter.setChapterNo(chapterVO.getChapterNo());
            bookChapter.setSortOrder(chapterVO.getSortOrder());
            bookChapter.setIsFree(chapterVO.getIsFree());
            bookChapterMapper.updateById(bookChapter);
            incomingIds.add(chapterVO.getId());
            if (comicBook) {
                replaceComicPages(bookId, chapterVO.getId(), chapterVO.getPages());
            }
        }

        List<Long> toDeleteIds = existingChapters.stream()
                .map(BookChapterVO::getId)
                .filter(id -> !incomingIds.contains(id))
                .collect(Collectors.toList());

        if (!toDeleteIds.isEmpty()) {
            bookChapterPageMapper.physicalDeleteByBookAndChapterIds(bookId, toDeleteIds);
            bookChapterMapper.delete(new LambdaQueryWrapper<BookChapter>()
                    .eq(BookChapter::getBookId, bookId)
                    .in(BookChapter::getId, toDeleteIds));
        }
    }

    /**
     * 删除电子书（逻辑删除）
     *
     * @param id 电子书ID
     */
    @Override
    @Transactional
    public void deleteBook(Long id)
    {
        requireManageLevel();
        BookDO bookDO = getById(id);
        checkEntityNotNull(bookDO, "电子书不存在");
        bookMapper.deleteById(id);
        deleteComicPagesByBook(id);

        // 删除关联标签
        bookTagDOMapper.delete(new LambdaQueryWrapper<BookTagDO>()
                .eq(BookTagDO::getBookId, id));

        // 删除关联章节
        bookChapterMapper.delete(new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, id));

        // 从ES删除
        bookEsService.deleteBookFromEs(id);
    }

    @Override
    @Transactional
    public void restoreBook(Long id)
    {
        requireFounderLevel();
        BookDO deletedBook = bookMapper.selectDeletedById(id);
        checkEntityNotNull(deletedBook, "电子书不存在");
        bookMapper.restoreById(id);
        bookEsService.syncBookToEs(id);
    }

    /**
     * 检查实体是否为空，为空则抛出异常
     *
     * @param entity 实体对象
     * @param message 异常信息
     */
    private void checkEntityNotNull(Object entity, String message)
    {
        if (StringUtils.isNull(entity)) {
            throw new ServiceException(message);
        }
    }

    /**
     * 检查用户是否购买电子书
     *
     * @param userId 用户ID
     * @param bookId 电子书ID
     * @return 是否购买
     */
    private boolean checkUserHasBought(Long userId, Long bookId)
    {
        if (StringUtils.isNull(userId)) {
            return false;
        }
        UserBookRelation relation = userBookRelationMapper.selectByUserIdAndBookId(userId, bookId);
        return relation != null && Integer.valueOf(1).equals(relation.getPurchased());
    }

    /**
     * 检查用户是否购买电子书，未购买则抛出异常
     *
     * @param userId 用户ID
     * @param bookId 电子书ID
     */
    private void checkUserHasBoughtOrThrow(Long userId, Long bookId)
    {
        if (!checkUserHasBought(userId, bookId)) {
            throw new ServiceException("access denied");
        }
    }

    /**
     * 获取或创建用户电子书关联记录
     */
    private UserBookRelation getOrCreateRelation(Long userId, Long bookId) {
        UserBookRelation relation = userBookRelationMapper.selectByUserIdAndBookId(userId, bookId);
        if (relation == null) {
            relation = new UserBookRelation();
            relation.setUserId(userId);
            relation.setBookId(bookId);
            relation.setFavorited(0);
            relation.setFollowed(0);
            relation.setPurchased(0);
            userBookRelationMapper.insert(relation);
        }
        return relation;
    }

    @Override
    @Transactional
    public void favoriteBook(Long bookId, Integer status) {
        Long userId = UserContextUtil.getCurrentUserId();
        BookDO bookDO = getById(bookId);
        checkEntityNotNull(bookDO, "电子书不存在");

        UserBookRelation relation = getOrCreateRelation(userId, bookId);
        relation.setFavorited(status);
        relation.setFavoriteTime(status == 1 ? LocalDateTime.now() : null);
        userBookRelationMapper.updateById(relation);
    }

    @Override
    @Transactional
    public void followBook(Long bookId, Integer status) {
        Long userId = ThreadLocalUtil.getCurrentUserId();
        BookDO bookDO = getById(bookId);
        checkEntityNotNull(bookDO, "电子书不存在");

        UserBookRelation relation = getOrCreateRelation(userId, bookId);
        relation.setFollowed(status);
        relation.setFollowTime(status == 1 ? LocalDateTime.now() : null);
        userBookRelationMapper.updateById(relation);
    }

    @Override
    public OrderCheckoutRespVO purchaseBook(Long bookId, Long userId, String channel) {
        BookDO bookDO = getById(bookId);
        checkEntityNotNull(bookDO, "电子书不存在");

        // 校验是否已购买
        UserBookRelation relation = userBookRelationMapper.selectByUserIdAndBookId(userId, bookId);
        if (relation != null && Integer.valueOf(1).equals(relation.getPurchased())) {
            throw new ServiceException("access denied");
        }

        // 构建订单结算参数，交给订单模块处理
        return orderCheckoutService.checkout(buildBookCheckoutReqVO(bookDO, bookId, userId, channel));
    }

    /**
     * 构建电子书下单结算参数。
     *
     * @param bookDO 电子书信息
     * @param bookId 电子书ID
     * @param userId 用户ID
     * @param channel 支付渠道
     * @return 订单结算参数
     */
    private OrderCheckoutReqVO buildBookCheckoutReqVO(BookDO bookDO,
                                                      Long bookId,
                                                      Long userId,
                                                      String channel) {
        OrderCheckoutReqVO reqVO = new OrderCheckoutReqVO();
        reqVO.setUserId(userId);
        reqVO.setProductType(ProductTypeEnum.BOOK.getCode());
        reqVO.setProductId(bookId);
        reqVO.setProductName(bookDO.getTitle());
        reqVO.setOriginalAmount(bookDO.getPrice());
        reqVO.setPayableAmount(bookDO.getPrice());
        reqVO.setDiscountAmount(java.math.BigDecimal.ZERO);
        reqVO.setChannel(channel);
        return reqVO;
    }

    @Override
    @Transactional
    public void grantBookAccess(String orderNo) {
        // 查询订单获取 userId 和 productId
        OshOrder order = oshOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ServiceException("订单不存在: " + orderNo);
        }

        Long userId = order.getUserId();
        Long bookId = order.getProductId();

        // 幂等校验
        UserBookRelation relation = getOrCreateRelation(userId, bookId);
        if (Integer.valueOf(1).equals(relation.getPurchased())) {
            return;
        }

        BookDO bookDO = getById(bookId);
        relation.setPurchased(1);
        relation.setPurchasePrice(bookDO != null ? bookDO.getPrice() : java.math.BigDecimal.ZERO);
        relation.setOrderNo(orderNo);
        relation.setPayTime(LocalDateTime.now());
        userBookRelationMapper.updateById(relation);
    }

    @Override
    public BookRelationStatusVO getBookRelationStatus(Long bookId) {
        Long userId = ThreadLocalUtil.getCurrentUserId();
        return buildBookRelationStatus(userId, bookId);
    }

    private BookRelationStatusVO buildBookRelationStatus(Long userId, Long bookId)
    {
        if (userId == null)
        {
            return buildAnonymousRelationStatus();
        }

        BookRelationStatusVO vo = new BookRelationStatusVO();
        UserBookRelation relation = userBookRelationMapper.selectByUserIdAndBookId(userId, bookId);
        if (relation == null)
        {
            vo.setFavorited(0);
            vo.setFollowed(0);
            vo.setPurchased(0);
            return vo;
        }

        vo.setFavorited(Optional.ofNullable(relation.getFavorited()).orElse(0));
        vo.setFollowed(Optional.ofNullable(relation.getFollowed()).orElse(0));
        vo.setPurchased(Optional.ofNullable(relation.getPurchased()).orElse(0));
        return vo;
    }

    private BookDetailAggregate loadBookDetailAggregate(Long bookId, Long userId)
    {
        CompletableFuture<BookDO> bookFuture = asyncTaskSupport.supplyAsync(() -> requireReadableBook(bookId), aggregationTaskExecutor);
        CompletableFuture<List<BookChapterVO>> chapterFuture = asyncTaskSupport.supplyAsync(
                () -> bookChapterMapper.selectBookChapterListByBookId(bookId), aggregationTaskExecutor);
        CompletableFuture<List<String>> tagFuture = asyncTaskSupport.supplyAsync(
                () -> bookTagDOMapper.selectBookTagListByBookId(bookId), aggregationTaskExecutor);
        CompletableFuture<BookRelationStatusVO> relationFuture = userId == null
                ? asyncTaskSupport.completedFuture(buildAnonymousRelationStatus())
                : asyncTaskSupport.supplyAsync(() -> buildBookRelationStatus(userId, bookId), aggregationTaskExecutor);

        asyncTaskSupport.awaitAll(bookFuture, chapterFuture, tagFuture, relationFuture);
        return new BookDetailAggregate(
                asyncTaskSupport.join(bookFuture),
                asyncTaskSupport.join(chapterFuture),
                asyncTaskSupport.join(tagFuture),
                asyncTaskSupport.join(relationFuture)
        );
    }

    private BookDetailVO buildBookDetailVO(BookDetailAggregate aggregate, Boolean forEdit)
    {
        BookDO bookDO = aggregate.getBook();
        BookDetailVO vo = new BookDetailVO();
        BeanUtils.copyProperties(bookDO, vo);
        vo.setDesc(bookDO.getDescription());
        vo.setTryContent(bookDO.getTryContent());
        vo.setPrice(Optional.ofNullable(bookDO.getPrice()).map(Object::toString).orElse("0"));
        vo.setTPrice(Optional.ofNullable(bookDO.getOriginalPrice()).map(Object::toString).orElse("0"));
        vo.setSubCount(Optional.ofNullable(bookDO.getSubCount()).orElse(0));
        vo.setLevel(bookDO.getLevel());
        vo.setStatus(bookDO.getStatus());
        vo.setDeleteFlag(bookDO.getDeleteFlag() == null ? 0 : bookDO.getDeleteFlag().intValue());
        vo.setCover(resolveBookCover(bookDO.getCover(), forEdit));
        attachBookAuthor(vo, bookDO, forEdit);
        Integer mediaType = BookMediaTypeEnum.normalize(bookDO.getMediaType());
        vo.setMediaType(mediaType);
        List<BookChapterVO> chapters = aggregate.getChapters();
        if (Boolean.TRUE.equals(forEdit) && BookMediaTypeEnum.isComic(mediaType)) {
            attachComicPages(chapters, bookDO.getId(), forEdit);
        } else {
            chapters = sanitizeChapterCatalog(chapters);
        }
        vo.setBookDetails(chapters);
        vo.setTags(aggregate.getTags());
        vo.setIsbuy(aggregate.getRelationStatus().getPurchased() == 1);
        vo.setIsfava(aggregate.getRelationStatus().getFavorited() == 1);
        return vo;
    }

    private List<BookChapterVO> sanitizeChapterCatalog(List<BookChapterVO> chapters)
    {
        if (chapters == null || chapters.isEmpty()) {
            return chapters;
        }
        for (BookChapterVO chapter : chapters) {
            if (chapter == null) {
                continue;
            }
            chapter.setContent(null);
            chapter.setPages(null);
        }
        return chapters;
    }

    private void attachBookAuthor(BookDetailVO vo, BookDO bookDO, Boolean forEdit) {
        Long authorUserId = bookDO.getCreateBy() != null ? bookDO.getCreateBy() : bookDO.getUpdateBy();
        if (authorUserId == null) {
            return;
        }
        OshUser author = oshUserMapper.selectById(authorUserId);
        if (author == null) {
            vo.setAuthorUserId(authorUserId);
            return;
        }
        vo.setAuthorUserId(author.getId());
        vo.setAuthorName(author.getUsername());
        vo.setAuthorAvatar(resolveUserAvatar(author.getAvatar(), forEdit));
        vo.setAuthorIntroduction(author.getIntroduction());
        vo.setAuthorGithubAccount(author.getGithubAccount());
        Map<String, Object> highestRole = findHighestRole(author.getId());
        if (highestRole != null) {
            vo.setAuthorRoleName(toStringValue(highestRole.get("roleName")));
            vo.setAuthorRoleLevel(toIntegerValue(highestRole.get("roleLevel")));
        }
    }

    private Map<String, Object> findHighestRole(Long userId) {
        List<Map<String, Object>> roles = userManageMapper.selectUserRolesByUserIds(Collections.singletonList(userId));
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.stream()
                .filter(role -> role != null)
                .max((left, right) -> Integer.compare(toIntegerValue(left.get("roleLevel")), toIntegerValue(right.get("roleLevel"))))
                .orElse(null);
    }

    private Integer toIntegerValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String resolveUserAvatar(String avatar, Boolean forEdit) {
        if (StringUtils.isEmpty(avatar) || Boolean.TRUE.equals(forEdit)) {
            return avatar;
        }
        if (avatar.startsWith("http") || avatar.startsWith("data:")) {
            return avatar;
        }
        return ossService.getLimitedUrl(avatar, 30);
    }

    private BookDO requireBook(Long bookId)
    {
        BookDO bookDO = getById(bookId);
        checkEntityNotNull(bookDO, "该记录不存在");
        return bookDO;
    }

    private BookDO requireReadableBook(Long bookId)
    {
        BookDO bookDO = bookMapper.selectReadableById(bookId);
        if (bookDO != null) {
            return bookDO;
        }
        if (UserContextUtil.getCurrentLevelSafely() >= 4) {
            bookDO = bookMapper.selectDeletedById(bookId);
        }
        checkEntityNotNull(bookDO, "该记录不存在");
        return bookDO;
    }

    private void requireManageLevel()
    {
        if (UserContextUtil.getCurrentLevelSafely() < 4) {
            throw new ServiceException("access denied");
        }
    }

    private void requireFounderLevel()
    {
        if (UserContextUtil.getCurrentLevelSafely() != 6) {
            throw new ServiceException("access denied");
        }
    }

    private void validateBookAccessLevel(Integer level)
    {
        if (level == null) {
            return;
        }
        if (!(level == 1 || level == 2 || level == 3 || level == 4 || level == 6)) {
            throw new ServiceException("access denied");
        }
    }

    private void normalizeBookPricing(BookDO bookDO, BookSaveReqVO reqVO)
    {
        Integer level = bookDO.getLevel() == null ? 1 : bookDO.getLevel();
        if (level != 2) {
            bookDO.setPrice(BigDecimal.ZERO);
            bookDO.setOriginalPrice(BigDecimal.ZERO);
            return;
        }
        bookDO.setPrice(Optional.ofNullable(reqVO.getPrice()).orElse(BigDecimal.ZERO));
        bookDO.setOriginalPrice(Optional.ofNullable(reqVO.getTPrice()).orElse(BigDecimal.ZERO));
    }

    private String resolveBookCover(String cover, Boolean forEdit)
    {
        if (StringUtils.isEmpty(cover))
        {
            return cover;
        }
        if (Boolean.TRUE.equals(forEdit))
        {
            return cover;
        }
        return ossService.getLimitedUrl(cover, 30);
    }

    private BookRelationStatusVO buildAnonymousRelationStatus()
    {
        BookRelationStatusVO vo = new BookRelationStatusVO();
        vo.setFavorited(0);
        vo.setFollowed(0);
        vo.setPurchased(0);
        return vo;
    }

    private static class BookDetailAggregate
    {
        private final BookDO book;

        private final List<BookChapterVO> chapters;

        private final List<String> tags;

        private final BookRelationStatusVO relationStatus;

        private BookDetailAggregate(BookDO book, List<BookChapterVO> chapters, List<String> tags, BookRelationStatusVO relationStatus)
        {
            this.book = book;
            this.chapters = chapters;
            this.tags = tags;
            this.relationStatus = relationStatus;
        }

        public BookDO getBook()
        {
            return book;
        }

        public List<BookChapterVO> getChapters()
        {
            return chapters;
        }

        public List<String> getTags()
        {
            return tags;
        }

        public BookRelationStatusVO getRelationStatus()
        {
            return relationStatus;
        }
    }
}
