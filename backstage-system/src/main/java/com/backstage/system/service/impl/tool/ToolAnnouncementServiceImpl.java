package com.backstage.system.service.impl.tool;

import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.system.domain.vo.tool.ToolAnnouncementVO;
import com.backstage.system.mapper.tool.OshToolAnnouncementMapper;
import com.backstage.system.service.tool.IToolAnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ToolAnnouncementServiceImpl implements IToolAnnouncementService {

    @Autowired
    private OshToolAnnouncementMapper oshToolAnnouncementMapper;

    @Override
    public List<ToolAnnouncementVO> listNoticeAnnouncements() {
        return listByChannel(AnnouncementChannelEnum.SYSTEM_NOTICE);
    }

    @Override
    public List<ToolAnnouncementVO> listDynamicAnnouncements() {
        return listByChannel(AnnouncementChannelEnum.USER_NOTICE);
    }

    @Override
    public List<ToolAnnouncementVO> listLatestSystemNotices() {
        return listNoticeAnnouncements();
    }

    @Override
    public List<ToolAnnouncementVO> listLatestUserNotices() {
        return listDynamicAnnouncements();
    }

    private List<ToolAnnouncementVO> listByChannel(AnnouncementChannelEnum channel) {
        List<ToolAnnouncementVO> list = oshToolAnnouncementMapper.selectLatestToolAnnouncementsByChannel(channel.getCode());
        return list == null ? Collections.emptyList() : list;
    }
}
