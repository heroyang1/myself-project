package com.backstage.system.service.homepage;

import com.backstage.system.domain.homepage.vo.HotInfoGapVO;

import java.util.List;

public interface IOshHomePageInfoGapService {
    List<HotInfoGapVO> getHotInfoGap(int limit);
    List<HotInfoGapVO> getHotInfoGapFromMysql(int effectiveLimit);
    int syncHotInfoGapToEs();
}
