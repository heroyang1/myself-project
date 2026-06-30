package com.backstage.system.service.questionanswer;

import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;

import java.util.List;

public interface IOshQAAnnouncementService {

    List<AnnouncementMarqueeVO> listNoticeAnnouncements(Integer limit);

    List<AnnouncementMarqueeVO> listDynamicAnnouncements(Integer limit);
}
