package com.backstage.system.mapper.tool;

import com.backstage.system.domain.vo.tool.ToolAnnouncementVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OshToolAnnouncementMapper {

    List<ToolAnnouncementVO> selectLatestToolAnnouncementsByChannel(@Param("channel") Integer channel);

    int insertToolAnnouncement(@Param("title") String title,
                               @Param("link") String link,
                               @Param("channel") Integer channel,
                               @Param("iconCode") String iconCode,
                               @Param("operator") String operator);
}
