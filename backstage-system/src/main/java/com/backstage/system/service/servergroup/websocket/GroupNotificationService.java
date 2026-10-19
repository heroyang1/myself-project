package com.backstage.system.service.servergroup.websocket;

import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.domain.vo.ServerGroupUserVo;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import com.backstage.system.mapper.servergroup.OshGroupServerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 拼团通知服务
 *
 * 基于现有 {@link WebSocketNotifyService} 和 {@link com.backstage.common.websocket.OshWebSocketHandler}，
 * 封装拼团业务场景的 WebSocket 推送逻辑。
 *
 * <p>推送场景：</p>
 * <ul>
 *   <li>{@code notifyGroupFormed} — 成团通知：推送给所有参团用户</li>
 *   <li>{@code notifyGroupFull} — 满员结束通知：推送给所有参团用户</li>
 *   <li>{@code notifyOrderPaid} — 支付成功确认：推送给下单用户</li>
 *   <li>{@code notifyAlmostFull} — 即将满员提醒：广播给所有在线用户（管理端监控）</li>
 * </ul>
 *
 * <p>消息类型（WsNotifyMessage.type）统一以 {@code GROUP_} 为前缀，
 * 前端据此路由到不同的 UI 组件。</p>
 *
 * @author system
 * @date 2026-07-12
 */
@Service
public class GroupNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(GroupNotificationService.class);

    /** 即将满员时剩余名额阈值 */
    private static final int ALMOST_FULL_THRESHOLD = 3;

    @Autowired
    private WebSocketNotifyService webSocketNotifyService;

    @Autowired
    private OshGroupServerMapper groupServerMapper;

    /**
     * 成团通知：推送给拼团下所有已支付用户
     *
     * @param sourceType system / initiated
     * @param groupId    system 时为 activityId，initiated 时为 initiatedId
     * @param title      活动标题（用于通知文案）
     */
    public void notifyGroupFormed(String sourceType, Long groupId, String title) {
        List<Long> userIds = fetchGroupUserIds(sourceType, groupId);
        WsNotifyMessage msg = new WsNotifyMessage();
        msg.setType("GROUP_FORMED");
        msg.setTitle("拼团成功");
        msg.setContent("恭喜！拼团「" + safeTitle(title) + "」已成功成团，服务器即将开通。");
        msg.setBizId(String.valueOf(groupId));
        msg.setRequireAuth(true);

        for (Long userId : userIds) {
            msg.setTargetUserId(userId);
            webSocketNotifyService.send(userId, msg);
        }
        logger.info("【拼团通知】成团通知已推送，sourceType={}, groupId={}, 用户数={}", sourceType, groupId, userIds.size());
    }

    /**
     * 满员结束通知：推送给拼团下所有已支付用户
     */
    public void notifyGroupFull(String sourceType, Long groupId, String title) {
        List<Long> userIds = fetchGroupUserIds(sourceType, groupId);
        WsNotifyMessage msg = new WsNotifyMessage();
        msg.setType("GROUP_FULL");
        msg.setTitle("拼团已满员");
        msg.setContent("拼团「" + safeTitle(title) + "」已满员结束。");
        msg.setBizId(String.valueOf(groupId));
        msg.setRequireAuth(true);

        for (Long userId : userIds) {
            msg.setTargetUserId(userId);
            webSocketNotifyService.send(userId, msg);
        }
        logger.info("【拼团通知】满员通知已推送，sourceType={}, groupId={}, 用户数={}", sourceType, groupId, userIds.size());
    }

    /**
     * 支付成功确认：推送给下单用户
     */
    public void notifyOrderPaid(Long userId, String orderNo, String title) {
        WsNotifyMessage msg = new WsNotifyMessage();
        msg.setTargetUserId(userId);
        msg.setType("ORDER_PAID");
        msg.setTitle("支付成功");
        msg.setContent("拼团「" + safeTitle(title) + "」订单支付成功，等待成团。");
        msg.setBizId(orderNo);
        msg.setRequireAuth(true);
        webSocketNotifyService.send(userId, msg);
        logger.info("【拼团通知】支付成功通知已推送，userId={}, orderNo={}", userId, orderNo);
    }

    /**
     * 即将满员提醒：广播给所有在线用户（管理端实时监控看板）
     */
    public void notifyAlmostFull(Long groupId, String title, int remainNum) {
        if (remainNum > ALMOST_FULL_THRESHOLD) {
            return;
        }
        WsNotifyMessage msg = new WsNotifyMessage();
        msg.setType("GROUP_ALMOST_FULL");
        msg.setTitle("拼团即将满员");
        msg.setContent("拼团「" + safeTitle(title) + "」仅剩 " + remainNum + " 个名额！");
        msg.setBizId(String.valueOf(groupId));
        webSocketNotifyService.broadcast(msg);
        logger.info("【拼团通知】即将满员广播已推送，groupId={}, remainNum={}", groupId, remainNum);
    }

    // ==================== 内部方法 ====================

    /**
     * 查询拼团下所有已支付用户的 userId 列表
     */
    private List<Long> fetchGroupUserIds(String sourceType, Long groupId) {
        try {
            List<ServerGroupUserVo> users;
            if ("initiated".equals(sourceType)) {
                users = groupServerMapper.selectGroupUsersByInitiatedId(groupId);
            } else {
                users = groupServerMapper.selectGroupUsers(groupId);
            }
            if (users == null || users.isEmpty()) {
                logger.warn("【拼团通知】未查询到参团用户，sourceType={}, groupId={}", sourceType, groupId);
                return java.util.Collections.emptyList();
            }
            return users.stream()
                    .map(ServerGroupUserVo::getUserId)
                    .filter(uid -> uid != null && uid > 0)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("【拼团通知】查询参团用户失败，sourceType={}, groupId={}", sourceType, groupId, e);
            return java.util.Collections.emptyList();
        }
    }

    private String safeTitle(String title) {
        if (title == null || title.isEmpty()) {
            return "拼团活动";
        }
        return webSocketNotifyService.truncate(title);
    }
}
