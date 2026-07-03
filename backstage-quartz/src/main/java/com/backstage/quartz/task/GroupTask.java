package com.backstage.quartz.task;

import com.backstage.system.service.servergroup.IOshGroupServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 拼团模块定时任务 Bean
 *
 * 对应 sys_job 中的 invoke_target 前缀 "groupTask"
 * 配置 SQL：sql/2026/05/init_group_quartz_jobs.sql
 *
 * @author system
 * @date 2026-06-24
 */
@Component("groupTask")
public class GroupTask {

    private static final Logger log = LoggerFactory.getLogger(GroupTask.class);

    @Lazy
    @Resource
    private IOshGroupServerService groupServerService;

    /**
     * 取消超时未支付的 pending 订单
     * cron: 0 * /5 * * * ?
     */
    public void cancelExpiredOrders() {
        int count = groupServerService.cancelExpiredOrders();
        log.info("[拼团定时任务] 取消超时订单: {} 条", count);
    }

    /**
     * 关闭已过招募期的用户发起拼团
     * cron: 0 0 2 * * ?
     */
    public void expireInitiatedGroups() {
        int count = groupServerService.expireInitiatedGroups();
        log.info("[拼团定时任务] 过期用户发起: {} 条", count);
    }

    /**
     * 自动更新拼团活动状态
     * cron: 0 30 2 * * ?
     */
    public void autoUpdateActivityStatus() {
        int count = groupServerService.autoUpdateActivityStatus();
        log.info("[拼团定时任务] 自动更新活动状态: {} 条", count);
    }

    /**
     * 同步活动当前参与人数
     * cron: 0 * /10 * * * ?
     */
    public void refreshActivityCurrentNum() {
        int count = groupServerService.refreshActivityCurrentNum();
        log.info("[拼团定时任务] 刷新活动人数: {} 条", count);
    }

    /**
     * 服务器到期前提醒
     * cron: 0 0 9 * * ?
     */
    public void notifyExpiringServers() {
        int count = groupServerService.notifyExpiringServers();
        log.info("[拼团定时任务] 到期提醒: {} 条", count);
    }

    /**
     * 清理长期未成团且已过期的组团
     * cron: 0 0 3 * * ?
     */
    public void cleanupExpiredGroups() {
        int count = groupServerService.cleanupExpiredGroups();
        log.info("[拼团定时任务] 清理过期组团: {} 条", count);
    }

    /**
     * 每日数据快照
     * cron: 0 0 4 * * ?
     */
    public void dailySnapshot() {
        groupServerService.dailySnapshot();
        log.info("[拼团定时任务] 每日数据快照完成");
    }
}
