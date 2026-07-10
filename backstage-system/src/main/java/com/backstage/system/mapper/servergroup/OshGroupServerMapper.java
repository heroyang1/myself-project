package com.backstage.system.mapper.servergroup;

import com.backstage.system.domain.servergroup.OshGroupActivity;
import com.backstage.system.domain.servergroup.OshGroupOrder;
import com.backstage.system.domain.servergroup.OshGroupWork;
import com.backstage.system.domain.vo.GroupActivityListVO;
import com.backstage.system.domain.vo.GroupWorkListVO;
import com.backstage.system.domain.vo.ServerGroupUserVo;
import com.backstage.system.domain.vo.MyGroupListVO;
import com.backstage.system.domain.vo.UserSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务器拼团Mapper接口
 * 
 * @author system
 * @date 2026-04-18
 */
@Mapper
public interface OshGroupServerMapper {
    
    /**
     * 查询拼团活动列表（C端展示用）
     * 
     * @param status 状态筛选（可选）
     * @return 拼团活动列表
     */
    List<GroupActivityListVO> selectGroupActivityList(@Param("status") Integer status);
    
    /**
     * 根据ID查询拼团活动详情
     * 
     * @param id 拼团活动ID
     * @return 拼团活动
     */
    OshGroupActivity selectGroupActivityById(@Param("id") Long id);

    /** 参团创建事务内锁定系统活动，串行化最后名额校验。 */
    OshGroupActivity selectGroupActivityByIdForUpdate(@Param("id") Long id);

    /**
     * 查询我加入的拼团列表（性能优化拆分版）
     */
    List<MyGroupListVO> selectMyJoinedGroupList(@Param("userId") Long userId, @Param("groupStatus") Integer groupStatus);

    /**
     * 查询我发起的拼团列表（性能优化拆分版）
     */
    List<MyGroupListVO> selectMyInitiatedGroupList(@Param("userId") Long userId, @Param("groupStatus") Integer groupStatus);
    
    /**
     * 查询拼团详情
     * 
     * @param id 拼团活动ID
     * @return 拼团详情
     */
    OshGroupActivity selectGroupDetailById(@Param("id") Long id);
    
    /**
     * 查询参团用户列表
     * 
     * @param groupActivityId 拼团活动ID
     * @return 参团用户列表
     */
    List<ServerGroupUserVo> selectGroupUsers(@Param("groupActivityId") Long groupActivityId);
    
    /**
     * 检查用户是否已参团
     * 
     * @param groupActivityId 拼团活动ID
     * @param userId 用户ID
     * @return 参团记录数
     */
    int checkUserJoined(@Param("groupActivityId") Long groupActivityId, @Param("userId") Long userId);
    
    /**
     * 根据用户发起拼团ID查询参团用户列表
     * 
     * @param initiatedId 用户发起拼团ID
     * @return 参团用户列表
     */
    List<ServerGroupUserVo> selectGroupUsersByInitiatedId(@Param("initiatedId") Long initiatedId);
    
    /**
     * 检查用户是否已参团（用户发起拼团）
     * 
     * @param initiatedId 用户发起拼团ID
     * @param userId 用户ID
     * @return 参团记录数
     */
    int checkUserJoinedByInitiatedId(@Param("initiatedId") Long initiatedId, @Param("userId") Long userId);
    
    /**
     * 更新参团记录服务器时间
     * 
     * @param id 参团记录ID
     * @param serverStartTime 服务器开始时间
     * @param serverExpireTime 服务器到期时间
     * @return 影响行数
     */
    int updateGroupWorkServerTime(@Param("id") Long id, @Param("serverStartTime") LocalDateTime serverStartTime, @Param("serverExpireTime") LocalDateTime serverExpireTime);
    
    /**
     * 根据活动和用户查询参团记录
     * 
     * @param activityId 活动ID
     * @param userId 用户ID
     * @return 参团记录
     */
    OshGroupWork selectGroupWorkByActivityAndUser(@Param("activityId") Long activityId, @Param("userId") Long userId);

    /** 支付/取消必须按订单保存的精确 work 主键定位，禁止 activityId + userId + LIMIT 1。 */
    OshGroupWork selectGroupWorkById(@Param("id") Long id);

    OshGroupWork selectActiveGroupWorkForUser(@Param("sourceType") String sourceType,
                                               @Param("sourceId") Long sourceId,
                                               @Param("userId") Long userId);

    int countActiveSystemOrders(@Param("activityId") Long activityId);

    int countActiveInitiatedOrders(@Param("initiatedId") Long initiatedId);

    int countActiveSystemOrdersForUser(@Param("activityId") Long activityId, @Param("userId") Long userId);

    int countActiveInitiatedOrdersForUser(@Param("initiatedId") Long initiatedId, @Param("userId") Long userId);
    
    /**
     * 插入参团记录
     * 
     * @param work 参团记录
     * @return 影响行数
     */
    int insertGroupWork(OshGroupWork work);

    /**
     * 查询全量组团记录列表（管理端）
     *
     * @param groupStatus 组团状态筛选（可选）：0-进行中 1-已成团 2-已结束
     * @return 组团记录列表
     */
    List<GroupWorkListVO> selectGroupWorkList(@Param("groupStatus") Integer groupStatus);
    
    /**
     * 更新拼团活动人数（乐观锁）
     * 
     * @param id 拼团活动ID
     * @param currentNum 当前人数
     * @param status 状态
     * @param serverStartTime 服务器开始时间
     * @param serverEndTime 服务器结束时间
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateGroupActivityWithLock(
            @Param("id") Long id,
            @Param("currentNum") Integer currentNum,
            @Param("status") Integer status,
            @Param("serverStartTime") LocalDateTime serverStartTime,
            @Param("serverEndTime") LocalDateTime serverEndTime,
            @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * 更新拼团活动人数+1
     * 
     * @param id 拼团活动ID
     * @param currentNum 当前人数
     * @param status 状态
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int incrementGroupActivityCurrentNum(
            @Param("id") Long id,
            @Param("currentNum") Integer currentNum,
            @Param("status") Integer status,
            @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * 更新拼团活动状态为已结束（达到人数上限）
     * 
     * @param id 拼团活动ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateGroupActivityStatusEnded(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * 更新参团记录状态
     * 
     * @param id 参团记录ID
     * @param groupStatus 组团状态
     * @return 影响行数
     */
    int updateGroupWorkStatus(@Param("id") Long id, @Param("groupStatus") Integer groupStatus);

    int updatePendingGroupWorkToPaid(@Param("id") Long id);
    
    /**
     * 插入拼团订单
     * 
     * @param order 拼团订单
     * @return 影响行数
     */
    int insertGroupOrder(OshGroupOrder order);
    
    /**
     * 根据订单号查询订单
     * 
     * @param orderNo 订单号
     * @return 订单
     */
    OshGroupOrder selectGroupOrderByOrderNo(@Param("orderNo") String orderNo);

    OshGroupOrder selectGroupOrderByOrderNoForUpdate(@Param("orderNo") String orderNo);
    
    /**
     * 更新订单状态
     * 
     * @param id 订单ID
     * @param status 订单状态
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateOrderStatus(@Param("id") Long id, @Param("status") String status, @Param("updateTime") LocalDateTime updateTime);

    int updatePendingGroupOrderToCancelled(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    int updatePendingGroupWorkToCancelled(@Param("id") Long id);

    int updatePendingOrderToPaid(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * 计算动态价格
     * 
     * @param basePrice 基础价格
     * @param totalDuration 总时长（月）
     * @param remainingMonths 剩余月数
     * @return 实际价格
     */
    BigDecimal calculateDynamicPrice(
            @Param("basePrice") BigDecimal basePrice,
            @Param("totalDuration") Integer totalDuration,
            @Param("remainingMonths") BigDecimal remainingMonths);
    
    /**
     * 查询超时未支付订单
     * 
     * @param timeoutTime 超时时间
     * @return 超时订单列表
     */
    List<OshGroupOrder> selectTimeoutOrders(@Param("timeoutTime") LocalDateTime timeoutTime);
    
    /**
     * 模糊查询用户名列表（用于手动添加参团用户）
     * 
     * @param keyword 搜索关键词（支持用户名、昵称模糊匹配）
     * @param limit 返回数量限制
     * @return 用户信息列表
     */
    List<UserSearchVO> selectUsernamesByKeyword(
            @Param("keyword") String keyword,
            @Param("limit") Integer limit);
    
    /**
     * 插入支付流水记录（用于拼团订单）
     * 
     * @param payment 支付流水记录
     * @return 影响行数
     */
    int insertGroupPayment(@Param("paymentNo") String paymentNo, @Param("orderNo") String orderNo, 
                           @Param("orderId") Long orderId,
                           @Param("amount") BigDecimal amount,
                           @Param("provider") String provider,
                           @Param("channel") Integer channel,
                           @Param("requestPayload") String requestPayload);

    /**
     * 校验支付流水是否已存在（按 payment_no，仅统计未逻辑删除的记录）
     * 用于 createGroupPayment 的幂等校验，避免重复创建支付流水。
     *
     * @param paymentNo 支付流水号
     * @return 已存在的未删除记录数
     */
    int existsPaymentByPaymentNo(@Param("paymentNo") String paymentNo);

    /**
     * 回写订单的 group_work_id（订单创建时 work 尚未生成，插入后再更新）
     *
     * @param orderId     订单ID
     * @param groupWorkId 参团记录ID
     * @return 影响行数
     */
    int updateOrderGroupWorkId(@Param("orderId") Long orderId, @Param("groupWorkId") Long groupWorkId);

    int updateOrderInitiatedId(@Param("orderId") Long orderId, @Param("initiatedId") Long initiatedId);

    // ==================== 定时任务 + 安全校验相关方法 ====================

    /**
     * 批量更新订单状态（用于超时取消）
     *
     * @param ids        订单ID列表
     * @param status     目标状态
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int batchUpdateOrderStatus(@Param("ids") List<Long> ids, @Param("status") String status, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 查询所有进行中的拼团活动（status=1）
     *
     * @return 活动列表
     */
    List<OshGroupActivity> selectAllActiveActivities();

    /**
     * 统计指定活动的有效参团人数（group_status IN (0,1)）
     *
     * @param activityId 活动ID
     * @return 人数
     */
    int countGroupWorkByActivityId(@Param("activityId") Long activityId);

    /**
     * 直接更新活动的 current_num（用于数据同步任务）
     *
     * @param id         活动ID
     * @param currentNum 实际人数
     * @return 影响行数
     */
    int updateActivityCurrentNum(@Param("id") Long id, @Param("currentNum") int currentNum);

    /**
     * 原子递增活动 current_num（CAS 模式，防超卖）
     *
     * @param id 活动ID
     * @return 影响行数（0 表示已满员）
     */
    int atomicIncrementCurrentNum(@Param("id") Long id);

    /**
     * 查询即将到期的参团记录（用于到期前提醒）
     *
     * @param startExpire 到期起始时间
     * @param endExpire   到期截止时间
     * @return 即将到期的参团记录
     */
    List<OshGroupWork> selectExpiringWorks(@Param("startExpire") LocalDateTime startExpire, @Param("endExpire") LocalDateTime endExpire);

    /**
     * 查询长期未成团且已过期的组团（用于清理）
     *
     * @param expireBefore 过期截止时间
     * @return 过期的活动列表
     */
    List<OshGroupActivity> selectExpiredUnformedActivities(@Param("expireBefore") LocalDateTime expireBefore);

    /**
     * 校验用户是否为某个拼团的成员（安全校验用）
     *
     * @param userId     用户ID
     * @param activityId 活动ID（osh_group_activity.id 或 osh_group_user_initiated.id）
     * @return 匹配记录数
     */
    int isUserInGroup(@Param("userId") Long userId, @Param("activityId") Long activityId);

    /**
     * 校验用户是否为指定订单的所有者（安全校验用）
     *
     * @param orderNo 订单号
     * @param userId  用户ID
     * @return 匹配记录数（1=匹配）
     */
    int checkOrderOwnership(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    /**
     * 统计指定状态的订单数量（用于每日快照）
     *
     * @param status 订单状态
     * @return 数量
     */
    int countOrdersByStatus(@Param("status") String status);

    /**
     * 统计拼团活动总数（用于每日快照）
     *
     * @param status 活动状态（可选）
     * @return 数量
     */
    int countActivities(@Param("status") Integer status);

    /**
     * 更新拼团活动状态（通用方法，用于定时任务）
     *
     * @param id         活动ID
     * @param status     目标状态
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateGroupActivityStatus(@Param("id") Long id, @Param("status") Integer status, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 原子递减活动 current_num（订单取消时释放名额）
     *
     * @param id 活动ID
     * @return 影响行数
     */
    int decrementActivityCurrentNum(@Param("id") Long id);

    /**
     * 批量更新指定活动下所有进行中的 work 记录为已成团，并设置服务器时间
     *
     * @param activityId      活动ID（osh_group_work.group_activity_id）
     * @param groupStatus     目标状态
     * @param serverStartTime 服务器开始时间
     * @param serverExpireTime 服务器到期时间
     * @return 影响行数
     */
    int batchUpdateWorkStatusByActivity(
            @Param("activityId") Long activityId,
            @Param("groupStatus") Integer groupStatus,
            @Param("serverStartTime") LocalDateTime serverStartTime,
            @Param("serverExpireTime") LocalDateTime serverExpireTime);
}
