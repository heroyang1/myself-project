package com.backstage.system.service.servergroup;

import com.backstage.system.domain.dto.GroupCreateDTO;
import com.backstage.system.domain.servergroup.OshGroupOrder;
import com.backstage.system.domain.vo.GroupActivityListVO;
import com.backstage.system.domain.vo.GroupCreateVO;
import com.backstage.system.domain.vo.GroupDetailVO;
import com.backstage.system.domain.vo.GroupWorkListVO;
import com.backstage.system.domain.vo.InitiableActivityVO;
import com.backstage.system.domain.vo.MyGroupListVO;
import com.backstage.system.domain.vo.UserInitiatedActivityListVO;
import com.backstage.system.domain.vo.UserSearchVO;
import com.backstage.system.domain.vo.group.JoinGroupVO;
import com.backstage.system.domain.vo.group.ServerTutorialVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务器拼团Service接口
 * 
 * @author system
 * @date 2026-04-18
 */
public interface IOshGroupServerService {
    
    /**
     * 查询拼团活动列表（C端）
     * 
     * @param status 状态筛选（可选）
     * @return 拼团活动列表
     */
    List<GroupActivityListVO> selectGroupActivityList(Integer status);
    
    /**
     * 查询用户发起拼团活动列表（C端）
     * 
     * @param status 状态筛选（可选）
     * @param type 类型筛选（可选）
     * @return 用户发起拼团活动列表
     */
    List<UserInitiatedActivityListVO> selectUserInitiatedActivityList(Integer status, String type);
    
    /**
     * 查询可发起的拼团活动列表
     * 
     * @return 可发起的拼团活动列表
     */
    List<InitiableActivityVO> selectInitiableActivities();
    
    /**
     * 查询我的拼团列表
     * 
     * @param userId 用户ID
     * @param groupStatus 组团状态筛选（可选）
     * @return 我的拼团列表
     */
    List<MyGroupListVO> selectMyGroupList(Long userId, Integer groupStatus);
    
    /**
     * 查询拼团详情
     * 
     * @param activityId 拼团活动ID
     * @param userId 当前用户ID（可为null）
     * @return 拼团详情
     */
    GroupDetailVO selectGroupDetail(Long activityId, Long userId);
    
    /**
     * 根据用户发起记录ID查询拼团详情
     * 
     * @param initiatedId 用户发起记录ID
     * @param userId 当前用户ID（可为null）
     * @return 拼团详情
     */
    GroupDetailVO selectGroupDetailByInitiatedId(Long initiatedId, Long userId);
    
    /**
     * 参与拼团
     * 
     * @param activityId 拼团活动ID或用户发起记录ID
     * @param userId 用户ID
     * @param payMethod 支付方式
     * @return 参团结果（包含订单号、支付状态等）
     */
    JoinGroupVO joinGroup(Long activityId, Long userId, String payMethod);
    
    /**
     * 发起拼团（创建组团）
     * 
     * @param dto 发起拼团请求参数
     * @param userId 用户ID
     * @return 发起结果
     */
    GroupCreateVO createGroupWork(GroupCreateDTO dto, Long userId);

    /**
     * 查询全量组团记录列表（管理端）
     *
     * @param groupStatus 组团状态筛选（可选）：0-进行中 1-已成团 2-已结束
     * @return 组团记录列表
     */
    List<GroupWorkListVO> selectGroupWorkList(Integer groupStatus);
    
    /**
     * 模糊查询用户名列表（用于手动添加参团用户）
     *
     * @param keyword 搜索关键词（支持用户名、昵称模糊匹配）
     * @param limit 返回数量限制（默认20）
     * @return 用户信息列表
     */
    List<UserSearchVO> searchUsernames(String keyword, Integer limit);
    
    /**
     * 手动添加用户到拼团（管理员操作）
     *
     * @param activityId 拼团活动ID
     * @param userId 用户ID
     * @param remark 备注说明
     * @param operatorId 操作人ID（管理员）
     * @return 添加结果
     */
    Map<String, Object> addUserToGroup(Long activityId, Long userId, String remark, Long operatorId);
    
    /**
     * 根据订单号查询订单（用于支付接口获取金额）
     * 
     * @param orderNo 订单号
     * @return 订单
     */
    OshGroupOrder selectGroupOrderByOrderNo(String orderNo);
    
    /**
     * 获取服务器教程
     * 
     * @param activityId 拼团活动ID（osh_group_user_initiated表ID或osh_group_activity表ID）
     * @return 服务器教程VO
     */
    ServerTutorialVO getServerTutorial(Long activityId);
    
    /**
     * 获取服务器SSH连接信息
     * 
     * @param activityId 拼团活动ID（osh_group_user_initiated表ID或osh_group_activity表ID）
     * @param userId 用户ID
     * @return 服务器SSH信息VO
     */
    com.backstage.system.domain.vo.group.ServerSshInfoVO getServerSshInfo(Long activityId, Long userId);
    
    /**
     * 处理拼团订单支付成功后的业务逻辑
     * 
     * 该方法由 GroupPaidHandler 调用，在支付成功后执行：
     * 1. 更新 osh_group_order 表的订单状态为已支付
     * 2. 更新 osh_group_work 表的参团记录状态
     * 3. 判断并更新 osh_group_user_initiated 表的拼团状态（成团/结束）
     * 4. 设置服务器时间（成团时）
     * 
     * @param orderNo 订单号（osh_group_order.order_no）
     * @return 是否处理成功
     */
    boolean handlePaymentSuccess(String orderNo);
    
    /**
     * 创建拼团订单的支付流水记录
     * 
     * 在调用支付接口前，必须先创建支付流水记录，以便支付回调时能够被正确处理。
     * 支付流水用于记录支付状态，与 osh_group_order 订单表配合使用。
     * 
     * @param orderNo 订单号（osh_group_order.order_no）
     * @param amount 支付金额
     * @param clientIp 客户端IP
     */
    void createGroupPayment(String orderNo, Long orderId, String amount, String clientIp);

    // ==================== 定时任务业务方法 ====================

    /**
     * 取消超时未支付的 pending 订单（每5分钟执行）
     * 仅释放名额，不退款。
     *
     * @return 取消的订单数量
     */
    int cancelExpiredOrders();

    /**
     * 关闭已过招募期的用户发起拼团（每天凌晨2点执行）
     *
     * @return 关闭的记录数量
     */
    int expireInitiatedGroups();

    /**
     * 自动更新拼团活动状态（每天凌晨2:30执行）
     * 根据参团人数和时间自动更新：1进行中→2拼团成功→3已结束
     *
     * @return 更新的活动数量
     */
    int autoUpdateActivityStatus();

    /**
     * 同步活动当前参与人数（每10分钟执行）
     * 基于 osh_group_work 表刷新 osh_group_activity.current_num
     *
     * @return 同步的活动数量
     */
    int refreshActivityCurrentNum();

    /**
     * 服务器到期前提醒（每天9点执行）
     * 在到期前N天提醒用户
     *
     * @return 提醒的记录数量
     */
    int notifyExpiringServers();

    /**
     * 清理长期未成团且已过期的组团（每天凌晨3点执行）
     *
     * @return 清理的活动数量
     */
    int cleanupExpiredGroups();

    /**
     * 每日数据快照（每天凌晨4点执行）
     * 生成前一天拼团数据快照（订单数/成交额/成团率等）
     */
    void dailySnapshot();

    // ==================== 安全校验方法 ====================

    /**
     * 校验用户是否为某个拼团的成员
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return 是否为成员
     */
    boolean isUserInGroup(Long userId, Long activityId);

    /**
     * 校验用户是否为指定订单的所有者
     *
     * @param orderNo 订单号
     * @param userId  用户ID
     * @return 是否为订单所有者
     */
    boolean checkOrderOwnership(String orderNo, Long userId);
}
