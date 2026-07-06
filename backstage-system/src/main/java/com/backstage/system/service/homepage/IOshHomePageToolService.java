package com.backstage.system.service.homepage;

import com.backstage.system.domain.homepage.vo.HotToolVO;

import java.util.List;

/**
 * 首页热门工具 Service 接口
 *
 * @author jayTatum
 */
public interface IOshHomePageToolService {

    /**
     * 查询首页热门工具，优先读取 ES，失败或为空时降级 MySQL。
     */
    List<HotToolVO> getHotTools(int limit);

    /**
     * 从 MySQL 查询首页热门工具（含 detailUrl 补全）。
     */
    List<HotToolVO> getHotToolsFromMysql(int effectiveLimit);

    /**
     * 全量同步首页热门工具 Top 5 到 ES。
     */
    int syncHotToolsToEs();
}
