package com.backstage.system.service.homepage;

import com.backstage.system.domain.homepage.vo.HotExamVO;

import java.util.List;

public interface IOshHomePageExamService {
    List<HotExamVO> getHotExams(int limit);
    List<HotExamVO> getHotExamsFromMysql(int effectiveLimit);
    int syncHotExamsToEs();
}
