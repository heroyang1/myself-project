package com.backstage.system.service.homepage;

import com.backstage.system.domain.homepage.vo.HotOpenProjectVO;

import java.util.List;

/**
 * 首页热门开源项目 Service 接口
 *
 * @author jayTatum
 */
public interface IOshHomePageOpenProjectService {

    List<HotOpenProjectVO> getHotOpenProjects(int limit);

    List<HotOpenProjectVO> getHotOpenProjectsFromMysql(int effectiveLimit);

    int syncHotOpenProjectsToEs();
}
