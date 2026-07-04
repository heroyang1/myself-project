package com.backstage.system.mapper.homepage;

import com.backstage.system.domain.homepage.vo.HotCourseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 首页热门课程 Mapper
 *
 * @author jayTatum
 */
@Mapper
public interface OshHomePageCourseMapper {

    /**
     * 查询热门课程（基于热度公式排序）
     *
     * @param limit 返回数量
     * @return 热门课程列表
     */
    List<HotCourseVO> selectHotCourses(@Param("limit") int limit);

    /**
     * 根据课程ID批量查询关联标签（内部使用，结果合并到 HotCourseVO.tags）
     *
     * @param courseIds 课程ID列表
     * @return 课程-标签关联列表
     */
    List<HotCourseVO> selectTagsByCourseIds(@Param("courseIds") List<Long> courseIds);

    /**
     * 根据课程ID批量查询实时视频数量，避免首页热门课受 osh_course.video_count 脏数据影响。
     *
     * @param courseIds 课程ID列表
     * @return 课程-视频数量列表
     */
    List<HotCourseVO> selectVideoCountsByCourseIds(@Param("courseIds") List<Long> courseIds);
}
