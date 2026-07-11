package com.backstage.system.mapper.internalmember;

import com.backstage.system.domain.internalmember.vo.InternalMemberGrowthStoryDetailVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberGrowthStoryPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InternalMemberGrowthStoryMapper {
    InternalMemberGrowthStoryDetailVO selectStoryDetailByUserId(@Param("userId") Long userId);

    InternalMemberGrowthStoryDetailVO selectPublicStoryDetailByUserId(@Param("userId") Long userId);

    List<InternalMemberGrowthStoryPageVO> selectPublicStoryPage(@Param("keyword") String keyword,
                                                                @Param("excludeUserId") Long excludeUserId);

    int insertStory(@Param("userId") Long userId,
                    @Param("storyTitle") String storyTitle,
                    @Param("storySummary") String storySummary,
                    @Param("storyContent") String storyContent,
                    @Param("factsJson") String factsJson,
                    @Param("generateStatus") String generateStatus,
                    @Param("isPublic") Integer isPublic,
                    @Param("publishedTime") String publishedTime,
                    @Param("operatorId") Long operatorId);

    int updateStory(@Param("userId") Long userId,
                    @Param("storyTitle") String storyTitle,
                    @Param("storySummary") String storySummary,
                    @Param("storyContent") String storyContent,
                    @Param("factsJson") String factsJson,
                    @Param("generateStatus") String generateStatus,
                    @Param("operatorId") Long operatorId);

    int updateStoryVisibility(@Param("userId") Long userId,
                              @Param("isPublic") Integer isPublic,
                              @Param("operatorId") Long operatorId);
}
