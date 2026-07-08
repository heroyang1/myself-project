package com.backstage.system.service.homepage;

import com.backstage.system.domain.homepage.vo.HotWebsiteVO;

import java.util.List;

public interface IOshHomePageWebsiteService {
    List<HotWebsiteVO> getHotWebsites(int limit);
    List<HotWebsiteVO> getHotWebsitesFromMysql(int effectiveLimit);
    int syncHotWebsitesToEs();
}
