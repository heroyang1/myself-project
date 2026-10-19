package com.backstage.system.service.homepage;

import com.backstage.system.domain.homepage.vo.HotGroupVO;

import java.util.List;

public interface IOshHomePageGroupService {
    List<HotGroupVO> getHotGroup(int limit);
    List<HotGroupVO> getHotGroupFromMysql(int effectiveLimit);
    int syncHotGroupToEs();
}
