package com.backstage.quartz.task;

import com.backstage.system.service.homepage.es.OshHomePageCardEsSyncOrchestrator;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 首页卡片 ES 同步任务（xxl-job）。
 *
 * handler 名称：
 * <ul>
 *   <li>homepage-card-es-sync       —— 全量同步所有卡片，cron 推荐 0 0/5 * * * ?</li>
 *   <li>homepage-card-es-sync-one   —— 按 cardType 同步单卡，应急或灰度首次接入时使用</li>
 * </ul>
 *
 * @author jayTatum
 */
@Component
public class OshHomePageCardEsSyncTask {

    private static final Logger log = LoggerFactory.getLogger(OshHomePageCardEsSyncTask.class);

    @Resource
    private OshHomePageCardEsSyncOrchestrator orchestrator;

    @XxlJob("homepage-card-es-sync")
    public void syncAll() {
        XxlJobHelper.log("首页卡片 ES 同步任务开始");
        log.info("[homepage-card-es-sync] xxl-job start");
        try {
            Map<String, Integer> result = orchestrator.syncAll();
            result.forEach((card, n) ->
                    XxlJobHelper.log("card={0}, written={1}", card, n));
            log.info("[homepage-card-es-sync] xxl-job finished, result={}", result);
        } catch (Exception ex) {
            XxlJobHelper.log("首页卡片 ES 同步任务失败: {0}", ex.getMessage());
            log.error("[homepage-card-es-sync] xxl-job failed, error={}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @XxlJob("homepage-card-es-sync-one")
    public void syncOne() {
        String cardType = XxlJobHelper.getJobParam();
        XxlJobHelper.log("首页卡片 ES 同步任务（单卡）开始, cardType={0}", cardType);
        log.info("[homepage-card-es-sync-one] xxl-job start, cardType={}", cardType);
        try {
            int written = orchestrator.syncOne(cardType);
            XxlJobHelper.log("单卡 {0} 同步完成, written={1}", cardType, written);
            log.info("[homepage-card-es-sync-one] done, cardType={}, written={}", cardType, written);
        } catch (Exception ex) {
            XxlJobHelper.log("单卡 {0} 同步失败: {1}", cardType, ex.getMessage());
            log.error("[homepage-card-es-sync-one] failed, cardType={}, error={}",
                    cardType, ex.getMessage(), ex);
            throw ex;
        }
    }
}
