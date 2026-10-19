package com.backstage.system.service.homepage;


import com.backstage.system.domain.homepage.vo.HotCourseVO;
import com.backstage.system.domain.homepage.vo.HotCourseLatestSectionVO;

import java.util.List;

/**
 * 首页热门课程 Service 接口
 *
 * @author jayTatum
 */
public interface IOshHomePageCourseService {

    /**
     * 查询首页热门课程，优先读取 ES，失败或为空时降级 MySQL
     *
     * @param limit 请求数量（仅支持 1..5，超过截断为 5，非正数返回空列表）
     * @return 热门课程列表（含标签和跳转链接）
     */
    List<HotCourseVO> getHotCourses(int limit);

    /**
     * 从 MySQL 查询首页热门课程原始读模型数据。
     * 返回结果已补全标签与跳转链接，但 cover 仍为原始 OSS key，不做临时签名。
     *
     * @param effectiveLimit 实际返回数量
     * @return 热门课程列表
     */
    List<HotCourseVO> getHotCoursesFromMysql(int effectiveLimit);

    /**
     * 查询课程最近更新的小节
     *
     * @param courseId 课程ID
     * @param limit 返回数量
     * @return 最新更新小节
     */
    List<HotCourseLatestSectionVO> getLatestCourseUpdates(Long courseId, Integer limit);

    /**
     * 全量同步首页热门课程 Top 5 到 ES。
     * 课程卡片实现采用 a/b 双索引轮换 + read alias 原子切换。
     *
     * @return 同步成功写入 ES 的文档数量
     */
    int syncHotCoursesToEs();
}
