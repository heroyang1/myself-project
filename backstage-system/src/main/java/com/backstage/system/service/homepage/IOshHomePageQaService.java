package com.backstage.system.service.homepage;

import com.backstage.system.domain.homepage.vo.HotQaVO;

import java.util.List;

public interface IOshHomePageQaService {
    List<HotQaVO> getHotQa(int limit);
    List<HotQaVO> getHotQaFromMysql(int effectiveLimit);
    int syncHotQaToEs();
}
