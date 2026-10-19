package com.backstage.system.mapper.course;

import com.backstage.system.domain.course.vo.CourseSearchUserStateVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OshCourseEsSearchMapper {

    List<CourseSearchUserStateVo> selectUserCourseStates(@Param("userId") Long userId, @Param("courseIds") List<Long> courseIds);

    List<CourseSearchUserStateVo> selectCoursePaidCounts(@Param("courseIds") List<Long> courseIds);
}
