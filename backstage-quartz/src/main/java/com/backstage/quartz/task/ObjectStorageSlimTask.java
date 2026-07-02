package com.backstage.quartz.task;

import com.backstage.system.domain.objectslim.vo.ObjectSlimRunResultVO;
import com.backstage.system.service.objectslim.ObjectSlimService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ObjectStorageSlimTask {
    private static final Logger log = LoggerFactory.getLogger(ObjectStorageSlimTask.class);

    @Autowired
    private ObjectSlimService objectSlimService;

    @XxlJob("osh-object-storage-slimmer")
    public void run() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("对象存储瘦身任务开始, param={0}", jobParam == null ? "" : jobParam);
        try {
            ObjectSlimRunResultVO result = objectSlimService.runFromJob(jobParam);
            XxlJobHelper.log("对象存储瘦身任务完成, processed={0}, success={1}, failed={2}, skipped={3}, message={4}",
                    result.getProcessed(), result.getSuccess(), result.getFailed(), result.getSkipped(), result.getMessage());
        } catch (Exception e) {
            log.error("对象存储瘦身任务失败: {}", e.getMessage(), e);
            XxlJobHelper.log("对象存储瘦身任务失败: {0}", e.getMessage());
            throw e;
        }
    }
}
