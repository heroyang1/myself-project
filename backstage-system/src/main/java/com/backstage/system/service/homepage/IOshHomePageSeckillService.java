package com.backstage.system.service.homepage;

import com.backstage.system.domain.homepage.vo.HotSeckillVO;

import java.util.List;

public interface IOshHomePageSeckillService {
    List<HotSeckillVO> getHotSeckill(int limit);
    List<HotSeckillVO> getHotSeckillFromMysql(int effectiveLimit);
    int syncHotSeckillToEs();
}
