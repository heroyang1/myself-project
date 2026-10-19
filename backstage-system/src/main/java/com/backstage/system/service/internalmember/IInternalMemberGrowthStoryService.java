package com.backstage.system.service.internalmember;

import com.backstage.system.domain.internalmember.dto.InternalMemberGrowthStoryPageQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberGrowthStoryVisibilityUpdateDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberGrowthStoryDetailVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberGrowthStoryPageVO;

import java.util.List;

public interface IInternalMemberGrowthStoryService {
    InternalMemberGrowthStoryDetailVO generateMyStory(Long userId);

    InternalMemberGrowthStoryDetailVO getMyStory(Long userId);

    InternalMemberGrowthStoryDetailVO updateMyStoryVisibility(Long userId, InternalMemberGrowthStoryVisibilityUpdateDTO dto);

    List<InternalMemberGrowthStoryPageVO> selectPublicStoryPage(Long userId, InternalMemberGrowthStoryPageQueryDTO query);

    InternalMemberGrowthStoryDetailVO getStoryDetail(Long userId, Long targetUserId);
}
