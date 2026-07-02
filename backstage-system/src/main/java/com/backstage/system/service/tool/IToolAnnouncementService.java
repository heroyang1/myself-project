package com.backstage.system.service.tool;

import com.backstage.system.domain.vo.tool.ToolAnnouncementVO;

import java.util.List;

public interface IToolAnnouncementService {

    List<ToolAnnouncementVO> listNoticeAnnouncements();

    List<ToolAnnouncementVO> listDynamicAnnouncements();

    List<ToolAnnouncementVO> listLatestSystemNotices();

    List<ToolAnnouncementVO> listLatestUserNotices();
}
