package com.backstage.system.service.homepage;

import com.backstage.system.domain.homepage.vo.HotFeedbackVO;

import java.util.List;

/**
 * 首页热门用户反馈 Service 接口
 *
 * @author jayTatum
 */
public interface IOshHomePageFeedbackService {

    /**
     * 查询首页热门用户反馈，优先读取 ES，失败或为空时降级 MySQL。
     *
     * @param limit 请求数量（支持 1..5，超过截断为 5，非正数返回空列表）
     */
    List<HotFeedbackVO> getHotFeedback(int limit);

    /**
     * 从 MySQL 查询首页热门用户反馈（含 detailUrl 补全）。
     */
    List<HotFeedbackVO> getHotFeedbackFromMysql(int effectiveLimit);

    /**
     * 全量同步首页热门用户反馈 Top 5 到 ES（先清空索引再批量写入）。
     */
    int syncHotFeedbackToEs();
}
