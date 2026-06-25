package com.backstage.system.mapper.book;

import com.backstage.system.domain.book.BookTagDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface BookTagDOMapper extends BaseMapper<BookTagDO> {
    default List<String> selectBookTagListByBookId(Long id) {
        LambdaQueryWrapper<BookTagDO> queryWrapper = new LambdaQueryWrapper<BookTagDO>()
                .select(BookTagDO::getTagName)
                .eq(BookTagDO::getBookId, id)
                .eq(BookTagDO::getDeleteFlag, 0);

        List<BookTagDO> list = selectList(queryWrapper);

        return list.stream()
                .map(BookTagDO::getTagName)
                .collect(Collectors.toList());
    }

    /**
     * 查询所有标签名称（去重）
     */
    default List<String> getTagList() {
        LambdaQueryWrapper<BookTagDO> queryWrapper = new LambdaQueryWrapper<BookTagDO>()
                .select(BookTagDO::getTagName)
                .eq(BookTagDO::getDeleteFlag, 0);

        List<BookTagDO> list = selectList(queryWrapper);

        return list.stream()
                .map(BookTagDO::getTagName)
                .distinct()
                .collect(Collectors.toList());
    }
}
