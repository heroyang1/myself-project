package com.backstage.system.mapper.book;

import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.UserBookRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 用户电子书关联 数据层
 *
 * @author backstage
 */
public interface UserBookRelationMapper extends BaseMapper<UserBookRelation>
{
    /**
     * 查询用户与某本电子书的关联记录
     */
    UserBookRelation selectByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * 查询用户购买的电子书列表
     */
    List<BookDO> selectUserBookList(Long userId);

    /**
     * 分页查询用户购买的电子书列表
     */
    Page<BookDO> selectUserBookListPage(@Param("userId") Long userId, Page<BookDO> page);

    int upsertPurchased(@Param("userId") Long userId,
                        @Param("bookId") Long bookId,
                        @Param("purchasePrice") BigDecimal purchasePrice,
                        @Param("orderNo") String orderNo,
                        @Param("operator") String operator);
}
