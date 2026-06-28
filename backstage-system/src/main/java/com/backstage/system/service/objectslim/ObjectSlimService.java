package com.backstage.system.service.objectslim;

import com.backstage.system.domain.objectslim.OshObjectSlimTask;
import com.backstage.system.domain.objectslim.dto.ObjectSlimPageQuery;
import com.backstage.system.domain.objectslim.dto.ObjectSlimRunRequest;
import com.backstage.system.domain.objectslim.dto.ObjectSlimScanRequest;
import com.backstage.system.domain.objectslim.vo.ObjectSlimConfigVO;
import com.backstage.system.domain.objectslim.vo.ObjectSlimOverviewVO;
import com.backstage.system.domain.objectslim.vo.ObjectSlimRunResultVO;
import com.github.pagehelper.PageInfo;

public interface ObjectSlimService {
    ObjectSlimConfigVO getConfig();

    ObjectSlimConfigVO setEnabled(boolean enabled);

    ObjectSlimOverviewVO overview();

    PageInfo<OshObjectSlimTask> page(ObjectSlimPageQuery query);

    ObjectSlimRunResultVO scan(ObjectSlimScanRequest request);

    ObjectSlimRunResultVO runSlim(ObjectSlimRunRequest request);

    ObjectSlimRunResultVO runFromJob(String jobParam);

    void retry(Long id);

    void rollback(Long id);

    void deleteBackup(Long id);

    String getPreviewUrl(Long id, Integer minute);
}
