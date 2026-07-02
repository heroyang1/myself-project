package com.backstage.system.mapper.book;

import com.backstage.system.controller.book.BookListReqVO;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.vo.book.BookListVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 电子书 数据层
 *
 * @author backstage
 */
@Mapper
public interface BookMapper extends BaseMapper<BookDO> {
    /**
     * 查询电子书列表
     *
     * @param bookDO 电子书
     * @return 电子书集合
     */
    List<BookListVO> getBookPageList(Page<BookDO> pageParam, @Param("reqVO") BookListReqVO reqVO);

    Page<BookListVO> selectFreeBookPage(Page<BookDO> page);

    @Select("select id, no, title, cover, description, try_content, price, original_price, sub_count, level, media_type, status, remark, " +
            "case when create_by regexp '^[0-9]+$' then create_by else null end as create_by, create_time, " +
            "case when update_by regexp '^[0-9]+$' then update_by else null end as update_by, update_time, delete_flag " +
            "from osh_book where id = #{id} and delete_flag = 0")
    BookDO selectReadableById(@Param("id") Long id);

    @Select("select id, no, title, cover, description, try_content, price, original_price, sub_count, level, media_type, status, remark, " +
            "case when create_by regexp '^[0-9]+$' then create_by else null end as create_by, create_time, " +
            "case when update_by regexp '^[0-9]+$' then update_by else null end as update_by, update_time, delete_flag " +
            "from osh_book where no = #{no} and delete_flag = 0 limit 1")
    BookDO selectReadableByNo(@Param("no") String no);

    @Select("select id, no, title, cover, description, try_content, price, original_price, sub_count, level, media_type, status, remark, " +
            "case when create_by regexp '^[0-9]+$' then create_by else null end as create_by, create_time, " +
            "case when update_by regexp '^[0-9]+$' then update_by else null end as update_by, update_time, delete_flag " +
            "from osh_book where id = #{id} and delete_flag = 1")
    BookDO selectDeletedById(@Param("id") Long id);

    @Update("update osh_book set delete_flag = 0, update_time = now() where id = #{id} and delete_flag = 1")
    int restoreById(@Param("id") Long id);
}
