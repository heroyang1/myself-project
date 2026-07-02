package com.backstage.system.service.homepage;

import com.backstage.system.domain.homepage.vo.HotBookVO;

import java.util.List;

public interface IOshHomePageBookService {
    List<HotBookVO> getHotBooks(int limit);
    List<HotBookVO> getHotBooksFromMysql(int effectiveLimit);
    int syncHotBooksToEs();
}
