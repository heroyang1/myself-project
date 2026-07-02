package com.backstage.system.service.questionanswer.impl;

import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.common.enums.AnnouncementModuleEnum;
import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import com.backstage.system.service.announcement.IAnnouncementMarqueeQueryService;
import com.backstage.system.service.questionanswer.IOshQAAnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OshQAAnnouncementServiceImpl implements IOshQAAnnouncementService {

    @Autowired
    private IAnnouncementMarqueeQueryService announcementMarqueeQueryService;

    @Override
    public List<AnnouncementMarqueeVO> listNoticeAnnouncements(Integer limit) {
        return announcementMarqueeQueryService.list(
                AnnouncementModuleEnum.QNA,
                AnnouncementChannelEnum.SYSTEM_NOTICE,
                limit
        );
    }

    @Override
    public List<AnnouncementMarqueeVO> listDynamicAnnouncements(Integer limit) {
        return announcementMarqueeQueryService.list(
                AnnouncementModuleEnum.QNA,
                AnnouncementChannelEnum.USER_NOTICE,
                limit
        );
    }
}
