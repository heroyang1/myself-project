package com.backstage.system.service.servergroup.impl;

import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.DateUtils;
import com.backstage.common.utils.DesensitizedUtil;
import com.backstage.common.utils.StringUtils;
import com.backstage.common.utils.uuid.IdUtils;
import com.backstage.system.domain.dto.GroupCreateDTO;
import com.backstage.system.domain.servergroup.OshGroupActivity;
import com.backstage.system.domain.servergroup.OshGroupOrder;
import com.backstage.system.domain.servergroup.OshGroupUserInitiated;
import com.backstage.system.domain.servergroup.OshGroupWork;
import com.backstage.system.domain.vo.GroupActivityListVO;
import com.backstage.system.domain.vo.GroupCreateVO;
import com.backstage.system.domain.vo.GroupDetailVO;
import com.backstage.system.domain.vo.GroupPriceCalculable;
import com.backstage.system.domain.vo.GroupWorkListVO;
import com.backstage.system.domain.vo.InitiableActivityVO;
import com.backstage.system.domain.vo.ServerGroupUserVo;
import com.backstage.system.domain.vo.MyGroupListVO;
import com.backstage.system.domain.vo.UserInitiatedActivityListVO;
import com.backstage.system.domain.vo.UserSearchVO;
import com.backstage.system.domain.vo.group.AddUserToGroupVO;
import com.backstage.system.domain.vo.group.JoinGroupVO;
import com.backstage.system.domain.vo.group.ServerTutorialVO;
import com.backstage.system.domain.vo.group.ServerSshInfoVO;
import com.backstage.system.domain.vo.group.SharePosterVO;
import com.backstage.system.domain.servergroup.enums.GroupConstants;
import com.backstage.system.domain.servergroup.enums.GroupOrderStatusEnum;
import com.backstage.system.domain.servergroup.enums.GroupStatusEnum;
import com.alibaba.fastjson2.JSONObject;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.mapper.servergroup.OshGroupServerMapper;
import com.backstage.system.mapper.servergroup.OshGroupUserInitiatedMapper;
import com.backstage.system.service.servergroup.IOshGroupServerService;
import com.backstage.system.service.servergroup.util.GroupPriceCalculator;
import com.backstage.system.utils.OssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 服务器拼团Service实现类
 * 
 * @author system
 * @date 2026-04-18
 */
@Service
public class OshGroupServerServiceImpl implements IOshGroupServerService {
    
    private static final Logger logger = LoggerFactory.getLogger(OshGroupServerServiceImpl.class);

    // ==================== Redis 缓存 Key 常量 ====================
    // 注意：活动列表缓存包含计算字段（canJoin、isSuccess、currentPrice、remainingMonths），
    // 这些字段依赖 currentNum 等实时数据，缓存期间可能与 DB 不一致。
    // TTL 设为 2 分钟是实时性与性能的折中方案。
    // 未来可考虑：只缓存静态字段，计算字段每次实时计算。
    private static final String CACHE_ACTIVITY_LIST_PREFIX = "group:activity:list:";
    private static final String CACHE_ACTIVITY_DETAIL_PREFIX = "group:activity:detail:";
    private static final long CACHE_ACTIVITY_LIST_TTL = 2;   // 活动列表缓存 2 分钟（含计算字段，缩短以保证数据时效性）
    private static final long CACHE_ACTIVITY_DETAIL_TTL = 10; // 活动详情缓存 10 分钟
    
    @Autowired
    private OshGroupServerMapper groupServerMapper;
    
    @Autowired
    private OshGroupUserInitiatedMapper userInitiatedMapper;

    /**
     * 通用订单 Mapper：拼团支付链路要在 osh_order 同步写入对应记录，
     * 否则 PaymentSuccessConsumer 找不到订单 productType，无法路由到 GroupPaidHandler。
     */
    @Autowired
    private OshOrderMapper oshOrderMapper;

    @Autowired
    private OshPaymentMapper oshPaymentMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Value("${pay.group.admin-manual-join-enabled:false}")
    private boolean adminManualJoinEnabled;
    
    /**
     * 查询拼团活动列表
     */
    @Override
    public List<GroupActivityListVO> selectGroupActivityList(Integer status) {
        // 1. 从 Redis 缓存获取
        String cacheKey = CACHE_ACTIVITY_LIST_PREFIX + (status == null ? "all" : status);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                logger.debug("【拼团缓存】命中活动列表缓存，key={}", cacheKey);
                return (List<GroupActivityListVO>) cached;
            }
        } catch (Exception e) {
            logger.warn("【拼团缓存】读取缓存失败，降级为DB查询", e);
        }

        List<GroupActivityListVO> list = groupServerMapper.selectGroupActivityList(status);
        
        // 3. 处理计算字段
        for (GroupActivityListVO vo : list) {
            // 计算isSuccess：current_num >= group_min_num
            vo.setIsSuccess(vo.getCurrentNum() >= vo.getGroupMinNum());
            
            // 计算还差几人成团
            vo.setRemainNum(Math.max(vo.getGroupMinNum() - vo.getCurrentNum(), 0));
            
            // 计算canJoin：status = 1 或 2
            vo.setCanJoin(vo.getStatus() == 1 || vo.getStatus() == 2);
            
            // 设置封面完整URL（非空时才转换）
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossUtil.getFullFilePath(vo.getCover()));
            }
            
            // 计算价格和剩余时间
            calculatePriceAndTime(vo);
        }

        // 4. 写入缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, list, CACHE_ACTIVITY_LIST_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("【拼团缓存】写入缓存失败，key={}", cacheKey, e);
        }
        
        return list;
    }
    
    /**
     * 查询用户发起拼团活动列表
     */
    @Override
    public List<UserInitiatedActivityListVO> selectUserInitiatedActivityList(Integer status, String type) {
        // 查询数据
        List<UserInitiatedActivityListVO> list = userInitiatedMapper.selectUserInitiatedActivityList(status, type);
        
        // 处理计算字段
        for (UserInitiatedActivityListVO vo : list) {
            // 计算isSuccess：current_num >= group_min_num
            vo.setIsSuccess(vo.getCurrentNum() >= vo.getGroupMinNum());
            
            // 计算canJoin：status = 1 或 2
            vo.setCanJoin(vo.getStatus() == 1 || vo.getStatus() == 2);
            
            // 设置封面完整URL（非空时才转换）
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossUtil.getFullFilePath(vo.getCover()));
            }
            
            // 计算价格和剩余时间
            calculatePriceAndTime(vo);
        }
        
        return list;
    }
    
    /**
     * 查询我的拼团列表
     * 性能优化：拆分为两个独立查询后 Java 层合并排序，避免 UNION ALL + ORDER BY 全表 filesort
     * 
     * 合并策略：
     * 1. selectMyJoinedGroupList — 从 osh_group_work + osh_group_order 查询用户作为成员加入的拼团
     * 2. selectMyInitiatedGroupList — 从 osh_group_user_initiated 查询用户发起的拼团
     * 3. Java 层按 sourceType + sourceId 去重后合并排序
     */
    @Override
    public List<MyGroupListVO> selectMyGroupList(Long userId, Integer groupStatus) {
        // 1. 查询用户加入的拼团（含团长自己的 work 记录）
        List<MyGroupListVO> joinedList = groupServerMapper.selectMyJoinedGroupList(userId, groupStatus);
        
        // 2. 查询用户发起的拼团（可能因异常导致 work 记录缺失，作为兜底补充）
        List<MyGroupListVO> initiatedList = groupServerMapper.selectMyInitiatedGroupList(userId, groupStatus);
        
        // 3. 合并去重（以 sourceType + sourceId 为唯一键，joined 优先）
        Set<String> joinedKeys = new HashSet<>();
        List<MyGroupListVO> merged = new ArrayList<>();
        for (MyGroupListVO vo : joinedList) {
            String key = vo.getSourceType() + ":" + vo.getSourceId();
            joinedKeys.add(key);
            merged.add(vo);
        }
        for (MyGroupListVO vo : initiatedList) {
            String key = vo.getSourceType() + ":" + vo.getSourceId();
            if (!joinedKeys.contains(key)) {
                merged.add(vo);
            }
        }
        
        // 按 joinTime 降序排序
        merged.sort((a, b) -> {
            if (a.getJoinTime() == null && b.getJoinTime() == null) return 0;
            if (a.getJoinTime() == null) return 1;
            if (b.getJoinTime() == null) return -1;
            return b.getJoinTime().compareTo(a.getJoinTime());
        });
        
        // 处理敏感信息脱敏 + 状态文字
        for (MyGroupListVO vo : merged) {
            // 脱敏管理员联系方式
            if (StringUtils.isNotEmpty(vo.getAdminContact())) {
                vo.setAdminContact(desensitizeContact(vo.getAdminContact()));
            }
            
            // 设置订单状态文字
            vo.setOrderStatusText(getOrderStatusText(vo.getOrderStatus()));
        }
        
        return merged;
    }
    
    /**
     * 查询拼团详情
     */
    @Override
    public GroupDetailVO selectGroupDetail(Long activityId, Long userId) {
        // 查询拼团活动
        OshGroupActivity activity = groupServerMapper.selectGroupActivityById(activityId);
        if (activity == null) {
            throw new ServiceException("拼团活动不存在");
        }
        
        // 构建详情VO
        GroupDetailVO vo = new GroupDetailVO();
        vo.setId(activity.getId());
        vo.setTitle(activity.getTitle());
        vo.setCpu(activity.getCpu());
        vo.setMemory(activity.getMemory());
        vo.setStorage(activity.getStorage());
        vo.setBasePrice(activity.getBasePrice());
        vo.setTotalDuration(activity.getTotalDuration());
        vo.setCurrentNum(activity.getCurrentNum());
        vo.setGroupMinNum(activity.getGroupMinNum());
        vo.setGroupMaxNum(activity.getGroupMaxNum());
        vo.setRemainNum(Math.max(activity.getGroupMinNum() - activity.getCurrentNum(), 0));
        vo.setStatus(activity.getStatus());
        vo.setStartTime(activity.getStartTime());
        vo.setServerStartTime(activity.getServerStartTime());
        vo.setServerExpireTime(activity.getServerEndTime());
        vo.setAdminContact(activity.getAdminContact());
        vo.setServerTutorialUrl(activity.getServerTutorialUrl());
        // 设置封面完整URL（非空时才转换）
        if (StringUtils.isNotEmpty(activity.getCover())) {
            vo.setCover(ossUtil.getFullFilePath(activity.getCover()));
        }
        
        // 计算成团状态
        int groupStatus = activity.getCurrentNum() >= activity.getGroupMinNum() ? 1 : 0;
        vo.setGroupStatus(groupStatus);
        vo.setGroupStatusText(getGroupStatusText(groupStatus));
        
        // 计算价格和剩余时间
        calculatePriceAndTimeForDetail(vo, activity);
        
        // 查询参团用户列表
        List<ServerGroupUserVo> users = groupServerMapper.selectGroupUsers(activityId);
        // 转换用户头像URL + 标记团长（第一个加入的为团长）
        if (users != null && !users.isEmpty()) {
            boolean leaderMarked = false;
            for (ServerGroupUserVo user : users) {
                if (StringUtils.isNotEmpty(user.getAvatar())) {
                    user.setAvatar(ossUtil.getFullFilePath(user.getAvatar()));
                }
                if (!leaderMarked) {
                    user.setIsLeader(true);
                    leaderMarked = true;
                } else {
                    user.setIsLeader(false);
                }
            }
        }
        vo.setUsers(users);
        
        // 判断当前用户是否已参团
        if (userId != null) {
            int count = groupServerMapper.checkUserJoined(activityId, userId);
            vo.setCurrentUserJoined(count > 0);
        } else {
            vo.setCurrentUserJoined(false);
        }
        
        // 计算是否可以参团
        vo.setCanJoin(activity.getStatus() == 1 || activity.getStatus() == 2);
        
        return vo;
    }
    
    /**
     * 根据用户发起记录ID查询拼团详情
     */
    @Override
    public GroupDetailVO selectGroupDetailByInitiatedId(Long initiatedId, Long userId) {
        // 查询用户发起记录
        OshGroupUserInitiated initiated = userInitiatedMapper.selectById(initiatedId);
        if (initiated == null) {
            throw new ServiceException("拼团记录不存在");
        }

        Long activityId = initiated.getActivityId();
        logger.info("【拼团详情】initiatedId={}, 查到的activityId={}", initiatedId, activityId);
        // 通过 activity_id 查询拼团活动模板
        OshGroupActivity activity = groupServerMapper.selectGroupActivityById(activityId);
        if (activity == null) {
            logger.error("【拼团详情】活动模板不存在，activityId={}, initiatedId={}", activityId, initiatedId);
            throw new ServiceException("拼团活动模板不存在，activityId=" + activityId);
        }
        
        // 构建详情VO（优先使用用户发起记录的数据）
        GroupDetailVO vo = new GroupDetailVO();
        vo.setId(initiated.getId()); // 使用用户发起记录的ID
        vo.setTitle(activity.getTitle());
        vo.setCpu(activity.getCpu());
        vo.setMemory(activity.getMemory());
        vo.setStorage(activity.getStorage());
        vo.setBasePrice(initiated.getCustomPrice() != null ? initiated.getCustomPrice() : activity.getBasePrice());
        vo.setTotalDuration(initiated.getDuration() != null ? initiated.getDuration() : activity.getTotalDuration());
        vo.setCurrentNum(initiated.getCurrentNum());
        vo.setGroupMinNum(initiated.getMinNum());
        vo.setGroupMaxNum(initiated.getMaxNum());
        vo.setRemainNum(Math.max(initiated.getMinNum() - initiated.getCurrentNum(), 0));
        
        // 状态映射：group_status -> status
        // 0-招募中 -> 1-进行中
        // 1-已成团 -> 2-拼团成功
        // 2-已结束 -> 3-已结束
        int status;
        if (initiated.getGroupStatus() == 0) {
            status = 1; // 进行中
        } else if (initiated.getGroupStatus() == 1) {
            status = 2; // 拼团成功
        } else {
            status = 3; // 已结束
        }
        vo.setStatus(status);
        vo.setGroupStatus(initiated.getGroupStatus());
        vo.setGroupStatusText(getGroupStatusText(initiated.getGroupStatus()));
        
        vo.setStartTime(initiated.getInitiateTime());
        vo.setExpireTime(initiated.getExpireTime()); // 招募截止时间
        vo.setServerStartTime(initiated.getServerStartTime());
        vo.setServerExpireTime(initiated.getServerExpireTime());
        vo.setAdminContact(activity.getAdminContact());
        vo.setServerTutorialUrl(activity.getServerTutorialUrl());
        
        // 设置封面完整URL
        String cover = activity.getCover();
        if (StringUtils.isNotEmpty(cover)) {
            vo.setCover(ossUtil.getFullFilePath(cover));
        }
        
        // 计算价格和剩余时间 - 使用 GroupPriceCalculator 统一计算
        if (initiated.getGroupStatus() == 1) {
            // 已成团
            if (initiated.getServerExpireTime() != null) {
                GroupPriceCalculator.PriceResult result = GroupPriceCalculator.calculateFormedGroupPrice(
                        vo.getBasePrice(), initiated.getServerExpireTime(), vo.getTotalDuration());
                vo.setRemainingMonths(result.getRemainingMonths());
                vo.setCurrentPrice(result.getCurrentPrice());
                vo.setMemberPrice(result.getMemberPrice());
            } else {
                // 已成团但无到期时间，使用基础价格
                vo.setRemainingMonths(new BigDecimal(initiated.getDuration()));
                GroupPriceCalculator.PriceResult result = GroupPriceCalculator.calculateUnformedGroupPrice(vo.getBasePrice());
                vo.setCurrentPrice(result.getCurrentPrice());
                vo.setMemberPrice(result.getMemberPrice());
            }
        } else {
            // 未成团：显示基础价格
            GroupPriceCalculator.PriceResult result = GroupPriceCalculator.calculateUnformedGroupPrice(vo.getBasePrice());
            vo.setRemainingMonths(null);
            vo.setCurrentPrice(result.getCurrentPrice());
            vo.setMemberPrice(result.getMemberPrice());
        }
        
        // 兜底：如果 currentPrice 仍为空，使用 basePrice
        if (vo.getCurrentPrice() == null && vo.getBasePrice() != null) {
            vo.setCurrentPrice(vo.getBasePrice());
            vo.setMemberPrice(vo.getBasePrice().multiply(new BigDecimal("0.8")));
        }
        
        // 查询参团用户列表（根据发起记录ID查询）
        List<ServerGroupUserVo> users = groupServerMapper.selectGroupUsersByInitiatedId(initiatedId);
        // 转换用户头像URL + 标记团长
        if (users != null && !users.isEmpty()) {
            boolean leaderMarked = false;
            for (ServerGroupUserVo user : users) {
                if (StringUtils.isNotEmpty(user.getAvatar())) {
                    user.setAvatar(ossUtil.getFullFilePath(user.getAvatar()));
                }
                if (!leaderMarked) {
                    user.setIsLeader(true);
                    leaderMarked = true;
                } else {
                    user.setIsLeader(false);
                }
            }
        }
        vo.setUsers(users);
        
        // 判断当前用户是否已参团
        if (userId != null) {
            int count = groupServerMapper.checkUserJoinedByInitiatedId(initiatedId, userId);
            vo.setCurrentUserJoined(count > 0);
        } else {
            vo.setCurrentUserJoined(false);
        }
        
        // 计算是否可以参团
        vo.setCanJoin(initiated.getGroupStatus() == 0 || initiated.getGroupStatus() == 1);
        
        return vo;
    }

    /**
     * 查询拼团详情（双数据源回退）
     * 先查 osh_group_user_initiated，未找到则回退到 osh_group_activity
     */
    @Override
    public GroupDetailVO selectGroupDetailWithFallback(Long activityId, Long userId, String sourceType) {
        if (GroupConstants.SOURCE_TYPE_INITIATED.equals(sourceType)) {
            return selectGroupDetailByInitiatedId(activityId, userId);
        }
        if (GroupConstants.SOURCE_TYPE_SYSTEM.equals(sourceType)) {
            return selectGroupDetail(activityId, userId);
        }
        if (StringUtils.isNotEmpty(sourceType)) {
            throw new ServiceException("拼团来源参数错误");
        }

        OshGroupUserInitiated initiated = userInitiatedMapper.selectById(activityId);
        OshGroupActivity systemActivity = groupServerMapper.selectGroupActivityById(activityId);
        if (initiated != null && systemActivity != null) {
            throw new ServiceException("拼团ID存在来源冲突，请从活动列表重新进入");
        }
        if (initiated != null) {
            return selectGroupDetailByInitiatedId(activityId, userId);
        }
        return selectGroupDetail(activityId, userId);
    }
    
    /**
     * 参与拼团（支持双数据源）
     * 1. 参与用户发起的拼团：activityId 实际为 osh_group_user_initiated 表ID
     * 2. 参与系统活动模板拼团：activityId 为 osh_group_activity 表ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JoinGroupVO joinGroup(Long activityId, Long userId, String payMethod, String sourceType) {
        // 参数校验
        if (activityId == null) {
            throw new ServiceException("拼团ID不能为空");
        }
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        
        if (GroupConstants.SOURCE_TYPE_INITIATED.equals(sourceType)) {
            OshGroupUserInitiated initiated = userInitiatedMapper.selectByIdForUpdate(activityId);
            if (initiated == null) {
                throw new ServiceException("用户发起的拼团不存在");
            }
            logger.info("用户 {} 参与用户发起的拼团，记录ID: {}", userId, activityId);
            return joinUserInitiatedGroup(initiated, userId, payMethod);
        }
        if (GroupConstants.SOURCE_TYPE_SYSTEM.equals(sourceType)) {
            OshGroupActivity activity = groupServerMapper.selectGroupActivityByIdForUpdate(activityId);
            if (activity == null) {
                throw new ServiceException("拼团活动不存在");
            }
            return joinSystemActivity(activity, userId, payMethod);
        }
        if (StringUtils.isNotEmpty(sourceType)) {
            throw new ServiceException("拼团来源参数错误");
        }

        // 兼容旧页面，但碰到两个独立自增表 ID 冲突时拒绝猜测来源。
        OshGroupUserInitiated initiated = userInitiatedMapper.selectById(activityId);
        OshGroupActivity systemActivity = groupServerMapper.selectGroupActivityById(activityId);
        if (initiated != null && systemActivity != null) {
            throw new ServiceException("拼团ID存在来源冲突，请刷新页面后重试");
        }
        if (initiated != null) {
            return joinUserInitiatedGroup(userInitiatedMapper.selectByIdForUpdate(activityId), userId, payMethod);
        }
        OshGroupActivity lockedActivity = groupServerMapper.selectGroupActivityByIdForUpdate(activityId);
        if (lockedActivity == null) {
            throw new ServiceException("拼团活动不存在");
        }
        return joinSystemActivity(lockedActivity, userId, payMethod);
    }
    
    /**
     * 参与用户发起的拼团
     * 
     * 修复说明：join 时只创建待支付订单和参团记录，不增加 currentNum。
     * currentNum 的递增延迟到支付成功回调（handleUserInitiatedPayment）中执行，
     * 避免用户放弃支付时 currentNum 偏大的问题。
     */
    private JoinGroupVO joinUserInitiatedGroup(OshGroupUserInitiated initiated, Long userId, String payMethod) {
        Long initiatedId = initiated.getId();
        
        // 1. 校验拼团状态
        if (initiated.getGroupStatus() == 2) {
            throw new ServiceException("该拼团已结束，请关注新一期拼团活动");
        }
        
        // 2. 校验是否已过期
        if (initiated.getExpireTime() != null && LocalDateTime.now().isAfter(initiated.getExpireTime())) {
            throw new ServiceException("该拼团招募已过期");
        }
        
        // pending 订单也预占名额；父记录已 FOR UPDATE，同一拼团的校验与插入串行执行。
        if (groupServerMapper.countActiveInitiatedOrders(initiatedId) >= initiated.getMaxNum()) {
            throw new ServiceException("该拼团人数已满");
        }
        
        // 待支付也属于有效参团，禁止同一用户反复创建多笔可支付订单。
        int joinedCount = groupServerMapper.countActiveInitiatedOrdersForUser(initiatedId, userId);
        if (joinedCount > 0) {
            throw new ServiceException("您已参与该拼团活动");
        }
        
        // 5. 计算价格和剩余时间
        // 参团时 remainingMonths = duration，因此 actualPrice = customPrice * duration / duration = customPrice
        BigDecimal remainingMonths = new BigDecimal(initiated.getDuration());
        BigDecimal actualPrice = initiated.getCustomPrice();
        
        // 6. 生成订单号
        String orderNo = GroupConstants.ORDER_NO_PREFIX + DateUtils.dateTime() + IdUtils.fastSimpleUUID().substring(0, 8);
        
        // 7. 创建待支付订单（不递增 currentNum）
        OshGroupOrder order = new OshGroupOrder();
        order.setUserId(userId);
        order.setGroupActivityId(initiated.getActivityId()); // 关联活动模板ID
        order.setSourceType(GroupConstants.SOURCE_TYPE_INITIATED);
        order.setInitiatedId(initiatedId);
        order.setOrderNo(orderNo);
        order.setPrice(actualPrice);
        order.setBasePrice(initiated.getCustomPrice());
        order.setRemainingMonths(remainingMonths);
        order.setStatus(GroupOrderStatusEnum.PENDING.getCode()); // 待支付
        order.setPayMethod(GroupConstants.PAY_METHOD_ALIPAY);
        
        int orderResult = groupServerMapper.insertGroupOrder(order);
        if (orderResult <= 0) {
            throw new ServiceException("创建订单失败");
        }
        
        // 8. 创建参团记录（初始状态为进行中，成团由支付回调触发）
        OshGroupWork work = new OshGroupWork();
        work.setGroupActivityId(initiatedId); // 关联用户发起记录ID
        work.setUserId(userId);
        work.setOrderId(order.getId());
        work.setActualPrice(actualPrice);
        work.setRemainingMonths(remainingMonths);
        work.setJoinTime(LocalDateTime.now());
        work.setGroupStatus(0); // 待支付（支付成功后更新为1=已支付）
        
        int workResult = groupServerMapper.insertGroupWork(work);
        if (workResult <= 0) {
            throw new ServiceException("创建参团记录失败");
        }
        
        // 9. 回写订单关联的参团ID到数据库
        order.setGroupWorkId(work.getId());
        groupServerMapper.updateOrderGroupWorkId(order.getId(), work.getId());
        
        // 注意：不递增 currentNum，不判断成团状态
        // currentNum 递增和成团判断延迟到支付成功回调中执行
        
        logger.info("用户{}参与用户发起的拼团{}（待支付），订单号：{}，当前已支付人数={}", 
                userId, initiatedId, orderNo, initiated.getCurrentNum());
        
        // 返回待支付状态
        return JoinGroupVO.pendingPayment(orderNo, actualPrice);
    }
    
    /**
     * 参与系统活动模板拼团
     */
    private JoinGroupVO joinSystemActivity(OshGroupActivity activity, Long userId, String payMethod) {
        Long activityId = activity.getId();

        // 校验拼团状态
        if (activity.getStatus() == 3) {
            throw new ServiceException("该拼团已结束，请关注新一期拼团活动");
        }
        
        // 校验人数上限
        if (groupServerMapper.countActiveSystemOrders(activityId) >= activity.getGroupMaxNum()) {
            throw new ServiceException("该拼团人数已满");
        }
        
        // 待支付也占用名额并阻止同一用户重复下单。
        int joinedCount = groupServerMapper.countActiveSystemOrdersForUser(activityId, userId);
        if (joinedCount > 0) {
            throw new ServiceException("您已参与该拼团活动");
        }
        
        // 计算实际价格和剩余时间（CAS 模式下价格按基础价计算，成团后按实际剩余时间调整）
        BigDecimal remainingMonths = new BigDecimal(activity.getTotalDuration());
        BigDecimal actualPrice = activity.getBasePrice();
        
        // 生成订单号
        String orderNo = GroupConstants.ORDER_NO_PREFIX + DateUtils.dateTime() + IdUtils.fastSimpleUUID().substring(0, 8);
        
        // 创建订单
        OshGroupOrder order = new OshGroupOrder();
        order.setUserId(userId);
        order.setGroupActivityId(activityId);
        order.setSourceType(GroupConstants.SOURCE_TYPE_SYSTEM);
        order.setOrderNo(orderNo);
        order.setPrice(actualPrice);
        order.setBasePrice(activity.getBasePrice());
        order.setRemainingMonths(remainingMonths);
        order.setStatus(GroupOrderStatusEnum.PENDING.getCode()); // 待支付
        order.setPayMethod(GroupConstants.PAY_METHOD_ALIPAY);
        
        int orderResult = groupServerMapper.insertGroupOrder(order);
        if (orderResult <= 0) {
            throw new ServiceException("创建订单失败");
        }
        
        // 创建参团记录（初始状态为进行中，CAS 后根据最新人数判断是否成团）
        OshGroupWork work = new OshGroupWork();
        work.setGroupActivityId(activityId);
        work.setUserId(userId);
        work.setOrderId(order.getId());
        work.setActualPrice(actualPrice);
        work.setRemainingMonths(remainingMonths);
        work.setJoinTime(LocalDateTime.now());
        work.setGroupStatus(0); // 待支付（支付成功后更新为1=已支付）
        
        int workResult = groupServerMapper.insertGroupWork(work);
        if (workResult <= 0) {
            throw new ServiceException("创建参团记录失败");
        }
        
        // 回写订单关联的参团ID到数据库
        order.setGroupWorkId(work.getId());
        groupServerMapper.updateOrderGroupWorkId(order.getId(), work.getId());
        
        // 注意：不递增 currentNum，不判断成团状态
        // currentNum 递增和成团判断延迟到支付成功回调中执行
        
        logger.info("用户{}参与系统活动拼团{}（待支付），订单号：{}", userId, activityId, orderNo);
        
        // 返回待支付状态
        return JoinGroupVO.pendingPayment(orderNo, actualPrice);
    }
    
    /**
     * 计算价格和剩余时间（列表页通用方法）
     * 适用于所有实现 GroupPriceCalculable 接口的 VO，
     * 包括 GroupActivityListVO 和 UserInitiatedActivityListVO。
     */
    private void calculatePriceAndTime(GroupPriceCalculable vo) {
        if (vo.getIsSuccess()) {
            GroupPriceCalculator.PriceResult result = GroupPriceCalculator.calculateFormedGroupPrice(
                    vo.getBasePrice(), vo.getServerEndTime(), vo.getTotalDuration());
            vo.setRemainingMonths(result.getRemainingMonths());
            vo.setCurrentPrice(result.getCurrentPrice());
        } else {
            GroupPriceCalculator.PriceResult result = GroupPriceCalculator.calculateUnformedGroupPrice(vo.getBasePrice());
            vo.setRemainingMonths(result.getRemainingMonths());
            vo.setCurrentPrice(result.getCurrentPrice());
        }
    }
    
    /**
     * 计算价格和剩余时间（详情页）- 使用 GroupPriceCalculator 统一计算
     * 
     * 重构说明：原方法使用内联 ChronoUnit + BigDecimal 计算，
     * 已重构为统一调用 GroupPriceCalculator，消除重复逻辑。
     */
    private void calculatePriceAndTimeForDetail(GroupDetailVO vo, OshGroupActivity activity) {
        if (vo.getGroupStatus() == 1) {
            // 已成团
            if (vo.getServerExpireTime() != null) {
                GroupPriceCalculator.PriceResult result = GroupPriceCalculator.calculateFormedGroupPrice(
                        vo.getBasePrice(), vo.getServerExpireTime(), vo.getTotalDuration());
                vo.setRemainingMonths(result.getRemainingMonths());
                vo.setCurrentPrice(result.getCurrentPrice());
                vo.setMemberPrice(result.getMemberPrice());
            } else {
                // 已成团但无到期时间，使用基础价格
                vo.setRemainingMonths(new BigDecimal(activity.getTotalDuration()));
                GroupPriceCalculator.PriceResult result = GroupPriceCalculator.calculateUnformedGroupPrice(vo.getBasePrice());
                vo.setCurrentPrice(result.getCurrentPrice());
                vo.setMemberPrice(result.getMemberPrice());
            }
        } else {
            // 未成团
            GroupPriceCalculator.PriceResult result = GroupPriceCalculator.calculateUnformedGroupPrice(vo.getBasePrice());
            vo.setRemainingMonths(null);
            vo.setCurrentPrice(result.getCurrentPrice());
            vo.setMemberPrice(result.getMemberPrice());
        }
        
        // 兜底：如果 currentPrice 仍为空，使用 basePrice
        if (vo.getCurrentPrice() == null && vo.getBasePrice() != null) {
            vo.setCurrentPrice(vo.getBasePrice());
            vo.setMemberPrice(GroupPriceCalculator.calculateMemberPrice(vo.getBasePrice()));
        }
    }
    
    /**
     * 获取组团状态文字
     */
    private String getGroupStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        GroupStatusEnum statusEnum = GroupStatusEnum.fromCode(status);
        if (statusEnum == null) {
            return "未知";
        }
        return statusEnum.getDesc();
    }
    
    /**
     * 获取订单状态文字
     */
    private String getOrderStatusText(String status) {
        if (status == null) {
            return "未知";
        }
        GroupOrderStatusEnum statusEnum = GroupOrderStatusEnum.fromCode(status);
        if (statusEnum == null) {
            return "未知";
        }
        return statusEnum.getDesc();
    }
    
    /**
     * 脱敏管理员联系方式
     */
    private String desensitizeContact(String contact) {
        if (StringUtils.isEmpty(contact)) {
            return contact;
        }
        if (contact.length() <= 4) {
            return "****";
        }
        return contact.substring(0, 3) + "****" + contact.substring(contact.length() - 2);
    }

    /**
     * SSH 密码脱敏：只返回首尾字符 + 星号
     * 
     * 示例：
     * - "abcdef" → "a****f"
     * - "abc" → "a*c"
     * - "ab" → "**"
     * - null/空 → "****"
     */
    private String maskSshPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return "****";
        }
        if (password.length() <= 2) {
            return "**";
        }
        if (password.length() <= 3) {
            return password.charAt(0) + "*" + password.charAt(password.length() - 1);
        }
        return password.charAt(0) + "****" + password.charAt(password.length() - 1);
    }

    /**
     * 清除活动相关缓存（列表 + 详情）
     * 在活动状态变更、人数变更、支付成功时调用
     */
    private void evictActivityCache(Long activityId) {
        try {
            // 清除列表缓存（所有状态）
            redisTemplate.delete(CACHE_ACTIVITY_LIST_PREFIX + "all");
            redisTemplate.delete(CACHE_ACTIVITY_LIST_PREFIX + "1");
            redisTemplate.delete(CACHE_ACTIVITY_LIST_PREFIX + "2");
            redisTemplate.delete(CACHE_ACTIVITY_LIST_PREFIX + "3");
            // 清除详情缓存
            if (activityId != null) {
                redisTemplate.delete(CACHE_ACTIVITY_DETAIL_PREFIX + activityId);
            }
            logger.debug("【拼团缓存】已清除活动缓存，activityId={}", activityId);
        } catch (Exception e) {
            logger.warn("【拼团缓存】清除缓存失败，activityId={}", activityId, e);
        }
    }
    
    /**
     * 查询可发起的拼团活动列表
     */
    @Override
    public List<InitiableActivityVO> selectInitiableActivities() {
        // 查询状态为1（进行中）的活动
        List<GroupActivityListVO> list = groupServerMapper.selectGroupActivityList(1);
        
        List<InitiableActivityVO> result = new ArrayList<>();
        for (GroupActivityListVO vo : list) {
            InitiableActivityVO initiableVO = new InitiableActivityVO();
            initiableVO.setId(vo.getId());
            initiableVO.setTitle(vo.getTitle());
            initiableVO.setCpu(vo.getCpu());
            initiableVO.setMemory(vo.getMemory());
            initiableVO.setStorage(vo.getStorage());
            initiableVO.setBasePrice(vo.getBasePrice());
            // 设置默认人数（从活动表获取）
            initiableVO.setDefaultMinNum(vo.getGroupMinNum());
            initiableVO.setDefaultMaxNum(vo.getGroupMaxNum());
            result.add(initiableVO);
        }
        
        return result;
    }
    
    /**
     * 发起拼团（创建组团）- 使用新表 osh_group_user_initiated
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupCreateVO createGroupWork(GroupCreateDTO dto, Long userId) {
        logger.info("用户 {} 开始发起拼团，活动ID: {}", userId, dto.getActivityId());
        
        // Step 1: 参数校验
        validateCreateParams(dto);

        // 统一时间戳：整个事务内使用同一个 now，避免多次调用 LocalDateTime.now() 导致时间不一致
        LocalDateTime now = LocalDateTime.now();

        // Step 2: 先创建拼团订单（order_id 需要在发起记录插入前确定）
        String orderNo = generateOrderNo();
        OshGroupOrder order = new OshGroupOrder();
        order.setUserId(userId);
        order.setGroupActivityId(dto.getActivityId());
        order.setSourceType(GroupConstants.SOURCE_TYPE_INITIATED);
        order.setOrderNo(orderNo);
        order.setPrice(dto.getPrice());
        order.setBasePrice(dto.getPrice());
        order.setRemainingMonths(new BigDecimal(dto.getDuration()));
        order.setStatus(GroupOrderStatusEnum.PENDING.getCode()); // 待支付
        order.setPayMethod(GroupConstants.PAY_METHOD_ALIPAY);
        order.setCreateTime(now);
        order.setUpdateTime(now);

        int orderResult = groupServerMapper.insertGroupOrder(order);
        if (orderResult <= 0) {
            throw new ServiceException("创建订单失败");
        }

        logger.info("订单创建成功，订单号: {}", orderNo);

        // Step 3: 创建用户发起拼团记录（使用新表）
        OshGroupUserInitiated initiated = new OshGroupUserInitiated();
        initiated.setUserId(userId);
        initiated.setActivityId(dto.getActivityId()); // 保存活动模板ID,优化查询性能
        initiated.setOrderId(order.getId());
        initiated.setMinNum(dto.getMinNum());
        initiated.setMaxNum(dto.getMaxNum());
        initiated.setDuration(dto.getDuration());
        initiated.setCustomPrice(dto.getPrice());
        initiated.setGroupStatus(0); // 0-招募中（未成团）
        initiated.setCurrentNum(0); // 只统计已支付人数，团长支付成功后再原子加 1
        initiated.setInitiateTime(now);
        // 设置招募截止时间（默认7天后）
        initiated.setExpireTime(now.plusDays(GroupConstants.RECRUIT_EXPIRE_DAYS));
        initiated.setCreateTime(now);
        initiated.setUpdateTime(now);

        int insertResult = userInitiatedMapper.insertUserInitiated(initiated);
        if (insertResult <= 0) {
            throw new ServiceException("创建发起记录失败");
        }

        Long initiatedId = initiated.getId();
        groupServerMapper.updateOrderInitiatedId(order.getId(), initiatedId);
        logger.info("用户发起拼团记录创建成功，记录ID: {}", initiatedId);

        // Step 4: 为团长创建参团记录（osh_group_work），确保团长出现在参团用户列表中
        OshGroupWork leaderWork = new OshGroupWork();
        leaderWork.setGroupActivityId(initiatedId);
        leaderWork.setUserId(userId);
        leaderWork.setOrderId(order.getId());
        leaderWork.setActualPrice(dto.getPrice());
        leaderWork.setRemainingMonths(new BigDecimal(dto.getDuration()));
        leaderWork.setGroupStatus(0); // 待支付（支付成功后更新为1=已支付）
        leaderWork.setJoinTime(now);
        if (groupServerMapper.insertGroupWork(leaderWork) != 1 || leaderWork.getId() == null) {
            throw new ServiceException("创建团长参团记录失败");
        }
        // group_work_id 必须保存真实 work 主键，不能保存另一个表的 initiatedId。
        order.setGroupWorkId(leaderWork.getId());
        groupServerMapper.updateOrderGroupWorkId(order.getId(), leaderWork.getId());
        logger.info("团长参团记录创建成功，workId: {}", leaderWork.getId());

        // Step 5: 组装返回结果
        GroupCreateVO resultVO = new GroupCreateVO();
        resultVO.setWorkId(initiatedId);
        resultVO.setOrderNo(orderNo);
        
        logger.info("用户 {} 发起拼团成功，记录ID: {}, 订单号: {}", userId, initiatedId, orderNo);
        
        return resultVO;
    }
    
    /**
     * 校验发起拼团参数
     */
    private void validateCreateParams(GroupCreateDTO dto) {
        if (dto.getMinNum() == null || dto.getMinNum() < 2) {
            throw new ServiceException("最低拼团人数不能少于2人");
        }
        if (dto.getMaxNum() == null || dto.getMaxNum() < dto.getMinNum()) {
            throw new ServiceException("最多拼团人数不能少于最低人数");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("请设置有效的支付金额");
        }
        if (dto.getDuration() == null || dto.getDuration() < 1 || dto.getDuration() > 120) {
            throw new ServiceException("使用时长必须在1-120个月之间");
        }
    }
    
    /**
     * 生成订单号
     * 格式：GRP + yyyyMMddHHmmss + 10位随机数
     * 10位随机数提供 100 亿种可能，有敁避免高并发下的订单号冲突
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%010d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1000000000));
        return "GRP" + timestamp + random;
    }

    /**
     * 查询全量组团记录列表（管理端）
     * 性能优化：拆分为两个独立查询后 Java 层合并排序，避免 UNION ALL + ORDER BY 全表 filesort
     */
    @Override
    public List<GroupWorkListVO> selectGroupWorkList(Integer groupStatus) {
        // 1. 分别查询系统活动拼团和用户发起拼团（各自可利用索引排序）
        List<GroupWorkListVO> systemList = groupServerMapper.selectSystemGroupWorkList(groupStatus);
        List<GroupWorkListVO> initiatedList = groupServerMapper.selectInitiatedGroupWorkList(groupStatus);
        
        // 2. Java 层合并两个已排序列表（归并排序思想，O(n+m)）
        List<GroupWorkListVO> merged = new ArrayList<>(systemList.size() + initiatedList.size());
        int i = 0, j = 0;
        while (i < systemList.size() && j < initiatedList.size()) {
            LocalDateTime timeA = systemList.get(i).getJoinTime();
            LocalDateTime timeB = initiatedList.get(j).getJoinTime();
            // 按 joinTime 降序排列
            if (timeA != null && (timeB == null || timeA.isAfter(timeB))) {
                merged.add(systemList.get(i++));
            } else {
                merged.add(initiatedList.get(j++));
            }
        }
        // 追加剩余元素
        while (i < systemList.size()) {
            merged.add(systemList.get(i++));
        }
        while (j < initiatedList.size()) {
            merged.add(initiatedList.get(j++));
        }
        
        return merged;
    }
    
    /**
     * 模糊查询用户名列表（用于手动添加参团用户）
     */
    @Override
    public List<UserSearchVO> searchUsernames(String keyword, Integer limit) {
        // 默认限制20条
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        
        // 如果keyword为空，返回空列表
        if (StringUtils.isEmpty(keyword)) {
            return new ArrayList<>();
        }
        
        // 查询用户
        List<UserSearchVO> users = groupServerMapper.selectUsernamesByKeyword(keyword.trim(), limit);
        
        // 处理用户头像URL
        if (users != null && !users.isEmpty()) {
            for (UserSearchVO user : users) {
                String avatar = user.getAvatar();
                if (StringUtils.isNotEmpty(avatar)) {
                    user.setAvatar(ossUtil.getFullFilePath(avatar));
                }
            }
        }
        
        return users;
    }
    
    /**
     * 手动添加用户到拼团（管理员操作）
     * 
     * 并发安全说明：使用 CAS 原子递增（incrementCurrentNumAtomic）替代 read-then-write，
     * 避免并发场景下 current_num 超出 max_num 的超卖问题。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddUserToGroupVO addUserToGroup(Long activityId, Long userId, String remark, Long operatorId) {
        if (!adminManualJoinEnabled) {
            throw new ServiceException("支付安全升级期间已暂停管理员手工加人，请使用正常参团流程");
        }
        
        logger.info("开始手动添加用户到拼团：activityId={}, userId={}, operatorId={}", activityId, userId, operatorId);
        
        // 1. 校验拼团活动是否存在
        OshGroupUserInitiated activity = userInitiatedMapper.selectActivityDetail(activityId);
        if (activity == null) {
            logger.error("拼团活动不存在：activityId={}", activityId);
            throw new ServiceException("拼团活动不存在");
        }
        
        logger.info("查询到拼团活动详情：activityId={}, groupStatus={}", activityId, activity.getGroupStatus());
        
        // 2. 校验拼团状态（只能是招募中）
        Integer groupStatus = activity.getGroupStatus();
        if (groupStatus == null || groupStatus != 0) {
            throw new ServiceException("该拼团活动不在招募中，无法添加用户");
        }
        
        // 3. 校验是否已过期
        LocalDateTime expireTime = activity.getExpireTime();
        if (expireTime != null && LocalDateTime.now().isAfter(expireTime)) {
            throw new ServiceException("该拼团活动已过期，无法添加用户");
        }
        
        // 4. 校验是否已达到最大人数（前置校验，CAS 后再确认）
        Integer currentNum = activity.getCurrentNum();
        Integer maxNum = activity.getMaxNum();
        if (currentNum >= maxNum) {
            throw new ServiceException("该拼团活动已达到最大人数限制");
        }
        
        // 5. 校验用户是否已参与该拼团
        int joinedCount = userInitiatedMapper.checkUserAlreadyJoined(activityId, userId);
        if (joinedCount > 0) {
            throw new ServiceException("该用户已参与此拼团活动");
        }
        
        // 6. 校验用户是否存在（可选，根据业务需求）
        // 这里假设 userId 已经通过搜索接口验证过
        
        // 7. CAS 原子递增拼团活动的当前人数（防并发超卖）
        int casResult = userInitiatedMapper.incrementCurrentNumAtomic(activityId);
        if (casResult <= 0) {
            throw new ServiceException("操作过于频繁，请稍后再试");
        }
        
        // 8. CAS 成功后重新读取最新人数
        OshGroupUserInitiated latestActivity = userInitiatedMapper.selectActivityDetail(activityId);
        Integer actualCurrentNum = latestActivity.getCurrentNum();
        
        // 9. 在 osh_group_work 表中插入参团记录（用于详情接口查询）
        Integer minNum = activity.getMinNum();
        Integer duration = activity.getDuration();
        BigDecimal customPrice = activity.getCustomPrice();
        java.time.LocalDateTime serverStartTime = null;
        java.time.LocalDateTime serverExpireTime = null;
        int workGroupStatus = 2; // 管理员手动添加，直接标记为已成团（无需支付）
        
        // 判断是否达到最低成团人数
        boolean isSuccess = actualCurrentNum >= minNum;
        if (isSuccess) {
            // 已成团，设置服务器时间
            serverStartTime = java.time.LocalDateTime.now();
            serverExpireTime = serverStartTime.plusMonths(duration);
            workGroupStatus = 2; // 已成团
        }
        
        // 创建参团记录
        OshGroupWork work = new OshGroupWork();
        work.setGroupActivityId(activityId);
        work.setUserId(userId);
        work.setOrderId(null); // 手动添加，暂无订单
        work.setActualPrice(customPrice); // 使用自定义价格
        work.setRemainingMonths(new java.math.BigDecimal(duration));
        work.setGroupStatus(workGroupStatus);
        work.setJoinTime(java.time.LocalDateTime.now());
        work.setServerStartTime(serverStartTime);
        work.setServerExpireTime(serverExpireTime);
        
        int workResult = groupServerMapper.insertGroupWork(work);
        if (workResult <= 0) {
            throw new ServiceException("创建参团记录失败");
        }
        
        // 10. 检查是否已成团
        if (isSuccess) {
            // 已成团，更新状态
            userInitiatedMapper.updateGroupStatus(activityId, 1);
            
            // 批量更新所有参团记录（包括历史加入的）的状态和服务器时间
            int batchResult = userInitiatedMapper.batchUpdateWorkStatusByInitiatedId(
                    activityId, 2, serverStartTime, serverExpireTime); // 2=已成团
            logger.info("拼团活动 {} 已成团，批量更新参团记录: 影响行数={}", activityId, batchResult);
        }
        
        // 11. 构建返回结果
        AddUserToGroupVO vo = new AddUserToGroupVO();
        vo.setSuccess(true);
        vo.setActivityId(activityId);
        vo.setUserId(userId);
        vo.setCurrentNum(actualCurrentNum);
        vo.setMaxNum(maxNum);
        vo.setIsSuccess(isSuccess);
        vo.setMessage(isSuccess ? "添加成功，拼团已成团！" : "添加成功");
        vo.setRemark(remark);
        
        logger.info("管理员 {} 手动添加用户 {} 到拼团活动 {}，当前人数={}", operatorId, userId, activityId, actualCurrentNum);
        
        return vo;
    }
    
    /**
     * 根据订单号查询订单
     */
    @Override
    public OshGroupOrder selectGroupOrderByOrderNo(String orderNo) {
        return groupServerMapper.selectGroupOrderByOrderNo(orderNo);
    }
    
    /**
     * 获取服务器教程
     */
    @Override
    public ServerTutorialVO getServerTutorial(Long activityId, String sourceType) {
        if (activityId == null) {
            throw new ServiceException("拼团活动ID不能为空");
        }
        
        ServerTutorialVO tutorial = new ServerTutorialVO();
        buildTutorialVO(tutorial, requireSourceActivity(activityId, sourceType));
        
        return tutorial;
    }
    
    /**
     * 构建教程VO
     */
    private void buildTutorialVO(ServerTutorialVO tutorial, OshGroupActivity activity) {
        // 设置教程内容（从server_tutorial_url获取完整URL）
        if (StringUtils.isNotEmpty(activity.getServerTutorialUrl())) {
            tutorial.setTutorial(ossUtil.getFullFilePath(activity.getServerTutorialUrl()));
        }
        
        // 构建教程步骤
        List<ServerTutorialVO.TutorialStep> steps = new ArrayList<>();
        steps.add(new ServerTutorialVO.TutorialStep(1, "连接服务器", 
            "使用SSH工具（如PuTTY、Xshell）连接到服务器。服务器IP和端口请联系管理员获取。\n\n" +
            "SSH连接命令：\n```\nssh username@server_ip -p port\n```"));
        steps.add(new ServerTutorialVO.TutorialStep(2, "初始化配置", 
            "首次登录后，请修改默认密码并配置SSH密钥以提高安全性。\n\n" +
            "修改密码命令：\n```\npasswd\n```"));
        steps.add(new ServerTutorialVO.TutorialStep(3, "环境检查", 
            "检查服务器环境配置，确认开发工具和依赖已安装。\n\n" +
            "常用检查命令：\n```\ncat /etc/os-release  # 查看系统信息\njava -version        # 查看Java版本\npython --version     # 查看Python版本\n```"));
        steps.add(new ServerTutorialVO.TutorialStep(4, "开始使用", 
            "完成以上步骤后，即可开始使用服务器进行开发、测试或部署。\n\n" +
            "如遇问题，请联系管理员：" + (StringUtils.isNotEmpty(activity.getAdminContact()) ? activity.getAdminContact() : "联系方式见拼团详情")));
        tutorial.setSteps(steps);
        
        // 构建服务器配置信息
        ServerTutorialVO.ServerConfig serverConfig = new ServerTutorialVO.ServerConfig();
        serverConfig.setSshPort(22);
        serverConfig.setDefaultUsername("root");
        serverConfig.setPasswordResetGuide("请联系管理员重置密码");
        tutorial.setServerConfig(serverConfig);
        
        // 构建FAQ
        List<ServerTutorialVO.FaqItem> faq = new ArrayList<>();
        faq.add(new ServerTutorialVO.FaqItem("如何连接服务器？", 
            "使用SSH客户端连接，服务器IP和登录信息将在成团后通过系统消息发送给您。"));
        faq.add(new ServerTutorialVO.FaqItem("服务器到期后数据会保留多久？", 
            "服务器到期后，数据会保留7天，请在到期前及时续费或导出重要数据。"));
        faq.add(new ServerTutorialVO.FaqItem("如何续费服务器？", 
            "您可以在个人中心的'我的拼团'页面查看并续费您的服务器。"));
        faq.add(new ServerTutorialVO.FaqItem("遇到技术问题怎么办？", 
            "请联系管理员 " + (StringUtils.isNotEmpty(activity.getAdminContact()) ? activity.getAdminContact() : "获取帮助")));
        tutorial.setFaq(faq);
    }
    
    /**
     * 获取服务器SSH连接信息
     * 
     * 说明：当前实现为模拟数据，实际项目中应从服务器分配表中查询
     * 建议后续扩展：在 osh_group_work 表增加 server_ip、ssh_port、ssh_username、ssh_password 字段
     * 或创建独立的 osh_server_allocation 表存储分配给用户的服务器信息
     */
    @Override
    public ServerSshInfoVO getServerSshInfo(Long activityId, Long userId, String sourceType) {
        if (activityId == null) {
            throw new ServiceException("拼团活动ID不能为空");
        }
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        
        ServerSshInfoVO sshInfo = new ServerSshInfoVO();
        OshGroupActivity activity = requireSourceActivity(activityId, sourceType);
        buildSshInfoFromActivity(sshInfo, activity, userId);
        
        // 必须按显式来源和订单保存的 work 主键查询，禁止数值 ID 碰撞串团。
        OshGroupWork work = groupServerMapper.selectActiveGroupWorkForUser(sourceType, activityId, userId);
        
        if (work == null) {
            throw new ServiceException("您尚未参与该拼团活动");
        }
        
        // 检查拼团状态（2=已成团 才能获取服务器信息）
        if (work.getGroupStatus() == null || work.getGroupStatus() < 2) {
            throw new ServiceException("拼团尚未成功，暂无法获取服务器信息");
        }
        
        // 设置到期时间（从参团记录获取）
        if (work.getServerExpireTime() != null) {
            sshInfo.setExpireTime(work.getServerExpireTime().toString().replace("T", " "));
        }
        
        // 设置状态
        if (work.getServerExpireTime() != null && work.getServerExpireTime().isBefore(LocalDateTime.now())) {
            sshInfo.setStatus("expired");
        } else {
            sshInfo.setStatus("running");
        }
        
        return sshInfo;
    }

    private OshGroupActivity requireSourceActivity(Long sourceId, String sourceType) {
        OshGroupActivity activity;
        if (GroupConstants.SOURCE_TYPE_SYSTEM.equals(sourceType)) {
            activity = groupServerMapper.selectGroupActivityById(sourceId);
        } else if (GroupConstants.SOURCE_TYPE_INITIATED.equals(sourceType)) {
            OshGroupUserInitiated initiated = userInitiatedMapper.selectById(sourceId);
            if (initiated == null) {
                throw new ServiceException("用户发起的拼团不存在");
            }
            activity = groupServerMapper.selectGroupActivityById(initiated.getActivityId());
        } else {
            throw new ServiceException("拼团来源必须是 system 或 initiated");
        }
        if (activity == null) {
            throw new ServiceException("拼团活动不存在");
        }
        return activity;
    }
    
    /**
     * 处理拼团订单支付成功后的业务逻辑
     * 
     * 该方法由 GroupPaidHandler 调用，在支付成功后执行：
     * 1. 更新 osh_group_order 表的订单状态为已支付
     * 2. 更新 osh_group_work 表的参团记录状态
     * 3. 判断并更新 osh_group_user_initiated 表的拼团状态（成团/结束）
     * 4. 设置服务器时间（成团时）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentSuccess(String orderNo) {
        logger.info("【拼团支付回调】处理支付成功，订单号: {}", orderNo);
        
        // 1. 查询订单
        OshGroupOrder order = groupServerMapper.selectGroupOrderByOrderNo(orderNo);
        if (order == null) {
            logger.warn("【拼团支付回调】订单不存在，orderNo={}", DesensitizedUtil.maskOrderNo(orderNo));
            return false;
        }
        
        // 2. 幂等检查：已支付则跳过
        GroupOrderStatusEnum currentStatus = GroupOrderStatusEnum.fromCode(order.getStatus());
        if (currentStatus != null && currentStatus.isPaidOrSuccess()) {
            logger.info("【拼团支付回调】订单已支付，跳过处理，orderNo={}", orderNo);
            return true;
        }
        
        // 3. 更新订单状态为已支付
        LocalDateTime now = LocalDateTime.now();
        int updateOrder = groupServerMapper.updatePendingOrderToPaid(order.getId(), now);
        if (updateOrder <= 0) {
            OshGroupOrder latestOrder = groupServerMapper.selectGroupOrderByOrderNo(orderNo);
            GroupOrderStatusEnum latestStatus = latestOrder != null ? GroupOrderStatusEnum.fromCode(latestOrder.getStatus()) : null;
            if (latestStatus != null && latestStatus.isPaidOrSuccess()) {
                logger.info("【拼团支付回调】订单已由其他任务处理，跳过重复发放，orderNo={}", orderNo);
                return true;
            }
            throw new ServiceException("订单状态更新失败，请稍后重试");
        }
        logger.info("【拼团支付回调】订单状态已更新为已支付，orderNo={}", orderNo);

        // 4. 同步更新 osh_order 状态（保持双表一致）
        try {
            oshOrderMapper.updatePendingToPaid(orderNo, now);
            logger.info("【拼团支付回调】osh_order 状态已同步更新，orderNo={}", orderNo);
        } catch (Exception e) {
            // P1修复：双表同步失败时记录错误日志并标记，便于后续对账修复
            logger.error("【拼团支付回调】osh_order 状态同步失败（需人工对账），orderNo={}, userId={}", 
                    DesensitizedUtil.maskOrderNo(orderNo), order.getUserId(), e);
        }
        
        // 5. 按显式来源路由，不能再用两个独立表的自增 ID 猜来源。
        Long groupWorkId = order.getGroupWorkId();
        if (groupWorkId == null) {
            throw new ServiceException("拼团订单缺少参团记录ID");
        }

        boolean handlerResult;
        if (GroupConstants.SOURCE_TYPE_INITIATED.equals(order.getSourceType())) {
            if (order.getInitiatedId() == null) {
                throw new ServiceException("用户发起拼团订单缺少 initiatedId");
            }
            OshGroupUserInitiated initiated = userInitiatedMapper.selectById(order.getInitiatedId());
            if (initiated == null) {
                throw new ServiceException("用户发起拼团记录不存在");
            }
            handlerResult = handleUserInitiatedPayment(initiated, order, now);
        } else if (GroupConstants.SOURCE_TYPE_SYSTEM.equals(order.getSourceType())) {
            handlerResult = handleSystemActivityPayment(groupWorkId, order, now);
        } else {
            throw new ServiceException("旧拼团订单缺少明确来源，请重新发起拼团");
        }
        
        // 清除活动缓存（支付成功后状态/人数变更）
        evictActivityCache(order.getGroupActivityId());
        
        if (!handlerResult) {
            throw new ServiceException("拼团权益处理失败，事务已回滚");
        }
        
        logger.info("【拼团支付回调】处理完成，orderNo={}, userId={}, activityId={}, sourceType={}", 
                orderNo, order.getUserId(), order.getGroupActivityId(), order.getSourceType());
        return true;
    }

    /**
     * 处理用户发起拼团的支付成功逻辑
     * 
     * 修复说明：currentNum 在 join 时不再递增，统一在支付成功回调中递增。
     * 使用 CAS 原子递增防止并发超卖，递增成功后重新读取最新 currentNum 判断成团。
     * 批量更新所有已支付成员的 osh_group_work 记录，确保状态和服务器时间保持一致。
     * 
     * groupStatus 状态流：0(待支付) → 1(已支付) → 2(已成团) / 3(已结束)
     */
    private boolean handleUserInitiatedPayment(OshGroupUserInitiated initiated, OshGroupOrder order, LocalDateTime now) {
        Long initiatedId = initiated.getId();
        Integer minNum = initiated.getMinNum();
        Integer maxNum = initiated.getMaxNum();
        Integer duration = initiated.getDuration();

        OshGroupWork work = requireOrderGroupWork(order);
        if (!Integer.valueOf(0).equals(work.getGroupStatus())) {
            throw new ServiceException("参团记录状态异常，请联系客服");
        }
        
        // 1. CAS 原子递增 currentNum（防并发超卖）
        int incrementResult = userInitiatedMapper.incrementCurrentNumAtomic(initiatedId);
        if (incrementResult <= 0) {
            logger.warn("【拼团支付回调】CAS递增currentNum失败（已满员或已被删除），initiatedId={}, orderNo={}，订单已标记为已支付待人工处理",
                    initiatedId, DesensitizedUtil.maskOrderNo(order.getOrderNo()));
            return false;
        }
        logger.info("【拼团支付回调】currentNum递增成功，initiatedId={}", initiatedId);
        
        // 2. 重新读取最新记录，获取递增后的 currentNum
        OshGroupUserInitiated latest = userInitiatedMapper.selectById(initiatedId);
        Integer currentNum = latest != null ? latest.getCurrentNum() : initiated.getCurrentNum() + 1;
        
        // 3. 更新当前支付用户的 work 状态为已支付（groupStatus: 0→1）
        int workUpdated = groupServerMapper.updatePendingGroupWorkToPaid(work.getId());
        if (workUpdated != 1) {
            throw new ServiceException("用户发起拼团参团状态更新失败");
        }
        logger.info("【拼团支付回调】更新用户work状态为已支付，workId={}, userId={}", work.getId(), order.getUserId());
        
        // 4. 判断是否已满员（优先检查，避免与成团状态覆盖）
        boolean isFull = maxNum != null && currentNum != null && currentNum >= maxNum;
        boolean isFormed = currentNum != null && minNum != null && currentNum >= minNum;
        
        if (isFull) {
            // P0修复：已满员时直接标记为已结束，跳过成团中间状态
            LocalDateTime serverStartTime = now;
            LocalDateTime serverExpireTime = serverStartTime.plusMonths(duration != null ? duration : 1);
            
            userInitiatedMapper.updateGroupStatus(initiatedId, 2); // 2=已结束
            userInitiatedMapper.updateServerTime(initiatedId, serverStartTime, serverExpireTime);
            
            // 批量更新所有已支付参团记录为已成团和服务器时间
            int batchResult = userInitiatedMapper.batchUpdateWorkStatusByInitiatedId(
                    initiatedId, 2, serverStartTime, serverExpireTime);
            logger.info("【拼团支付回调】用户发起拼团已满员结束，initiatedId={}, currentNum={}, maxNum={}, batchUpdated={}",
                    initiatedId, currentNum, maxNum, batchResult);
        } else if (isFormed) {
            // 达到最低成团人数但未达满员，标记为已成团
            LocalDateTime serverStartTime = now;
            LocalDateTime serverExpireTime = serverStartTime.plusMonths(duration != null ? duration : 1);
            
            // 更新用户发起拼团记录状态为已成团
            userInitiatedMapper.updateGroupStatus(initiatedId, 1);
            userInitiatedMapper.updateServerTime(initiatedId, serverStartTime, serverExpireTime);
            logger.info("【拼团支付回调】用户发起拼团已成团，initiatedId={}, currentNum={}, minNum={}", 
                    initiatedId, currentNum, minNum);
            
            // 批量更新所有已支付参团记录（group_status=1）的状态为已成团（2）和服务器时间
            int batchResult = userInitiatedMapper.batchUpdateWorkStatusByInitiatedId(
                    initiatedId, 2, serverStartTime, serverExpireTime);
            logger.info("【拼团支付回调】批量更新已支付参团记录为已成团: initiatedId={}, 影响行数={}", 
                    initiatedId, batchResult);
        } else {
            logger.info("【拼团支付回调】用户发起拼团尚未成团，initiatedId={}, currentNum={}, minNum={}",
                    initiatedId, currentNum, minNum);
        }
        
        return true;
    }

    /**
     * 处理系统活动模板拼团的支付成功逻辑
     *
     * 修复说明：currentNum 在 join 时不再递增，统一在支付成功回调中通过 CAS 原子递增。
     * 递增成功后判断成团/满员状态，更新 work 记录和清除缓存。
     * 
     * groupStatus 状态流：0(待支付) → 1(已支付) → 2(已成团) / 3(已结束)
     */
    private boolean handleSystemActivityPayment(Long groupWorkId, OshGroupOrder order, LocalDateTime now) {
        logger.info("【拼团支付回调】处理系统活动模板支付，groupWorkId={}", groupWorkId);
        Long activityId = order.getGroupActivityId();

        OshGroupWork work = requireOrderGroupWork(order);
        if (!Integer.valueOf(0).equals(work.getGroupStatus())) {
            throw new ServiceException("参团记录状态异常，请联系客服");
        }

        // 1. CAS 原子递增 currentNum（防并发超卖）
        int incrementResult = groupServerMapper.atomicIncrementCurrentNum(activityId);
        if (incrementResult <= 0) {
            logger.warn("【拼团支付回调】CAS递增currentNum失败（已满员或已被删除），activityId={}, orderNo={}，订单已标记为已支付待人工处理",
                    activityId, DesensitizedUtil.maskOrderNo(order.getOrderNo()));
            return false;
        }
        logger.info("【拼团支付回调】系统活动currentNum递增成功，activityId={}", activityId);

        // 2. 查找当前支付用户的参团记录
        // 3. 更新当前支付用户的 work 状态为已支付（groupStatus: 0→1）
        int workUpdated = groupServerMapper.updatePendingGroupWorkToPaid(work.getId());
        if (workUpdated != 1) {
            throw new ServiceException("系统拼团参团状态更新失败");
        }
        logger.info("【拼团支付回调】更新用户work状态为已支付，workId={}, userId={}", work.getId(), order.getUserId());

        // 4. 重新读取活动最新状态，判断成团/满员
        OshGroupActivity activity = groupServerMapper.selectGroupActivityById(activityId);
        if (activity == null) {
            throw new ServiceException("系统拼团活动不存在");
        }

        int currentNum = activity.getCurrentNum();
        int minNum = activity.getGroupMinNum();
        int maxNum = activity.getGroupMaxNum();

        // 5. 判断成团/满员状态（P0修复：优先检查满员，避免状态覆盖）
        boolean isFull = currentNum >= maxNum;
        boolean isFormed = currentNum >= minNum;
        
        if (isFull) {
            // 已满员：直接标记为已结束，跳过成团中间状态
            LocalDateTime serverStartTime = activity.getServerStartTime() != null ? activity.getServerStartTime() : now;
            LocalDateTime serverExpireTime = activity.getServerEndTime() != null
                    ? activity.getServerEndTime()
                    : serverStartTime.plusMonths(activity.getTotalDuration());
            
            // 批量更新所有已支付参团记录为已成团
            int batchUpdated = groupServerMapper.batchUpdateWorkStatusByActivity(
                    activityId, 2, serverStartTime, serverExpireTime);
            
            // 更新活动状态为已结束
            if (activity.getStatus() != null && activity.getStatus() < 3) {
                groupServerMapper.updateGroupActivityStatus(activityId, 3, now);
            }
            logger.info("【拼团支付回调】系统活动已满员结束，activityId={}, currentNum={}, maxNum={}, batchUpdated={}",
                    activityId, currentNum, maxNum, batchUpdated);
        } else if (isFormed) {
            // 达到最低成团人数但未达满员
            LocalDateTime serverStartTime = activity.getServerStartTime() != null ? activity.getServerStartTime() : now;
            LocalDateTime serverExpireTime = activity.getServerEndTime() != null
                    ? activity.getServerEndTime()
                    : serverStartTime.plusMonths(activity.getTotalDuration());
            
            // 批量更新所有已支付参团记录（group_status=1）为已成团（2）
            int batchUpdated = groupServerMapper.batchUpdateWorkStatusByActivity(
                    activityId, 2, serverStartTime, serverExpireTime);
            logger.info("【拼团支付回调】系统活动已成团，批量更新已支付work: activityId={}, 影响行数={}, currentNum={}",
                    activityId, batchUpdated, currentNum);
            
            // 更新活动状态为拼团成功
            if (activity.getStatus() != null && activity.getStatus() < 2) {
                groupServerMapper.updateGroupActivityStatus(activityId, 2, now);
            }
        } else {
            logger.info("【拼团支付回调】系统活动尚未成团，workId={}, currentNum={}, minNum={}",
                    work.getId(), currentNum, minNum);
        }

        // 7. 清除活动缓存
        evictActivityCache(activityId);
        return true;
    }

    private OshGroupWork requireOrderGroupWork(OshGroupOrder order) {
        if (order.getGroupWorkId() == null) {
            throw new ServiceException("拼团订单缺少参团记录ID");
        }
        OshGroupWork work = groupServerMapper.selectGroupWorkById(order.getGroupWorkId());
        if (work == null
                || !order.getId().equals(work.getOrderId())
                || !order.getUserId().equals(work.getUserId())) {
            throw new ServiceException("拼团订单关联的参团记录不一致");
        }
        return work;
    }
    
    /**
     * 根据活动信息构建SSH信息（真实数据存储）
     * 
     * 安全说明：密码字段经过脱敏处理，只返回首尾字符 + 星号，
     * 防止 API 接口泄露完整密码。用户应通过安全渠道（如站内信）获取完整密码。
     */
    private void buildSshInfoFromActivity(ServerSshInfoVO sshInfo, OshGroupActivity activity, Long userId) {
        // 从 OshGroupWork 表中获取真实的服务器分配信息
        OshGroupWork work = groupServerMapper.selectGroupWorkByActivityAndUser(activity.getId(), userId);
        
        if (work != null && work.getServerIp() != null) {
            // 真实数据：使用数据库中存储的服务器信息
            sshInfo.setIp(work.getServerIp());
            sshInfo.setPort(work.getSshPort() != null ? work.getSshPort() : 22);
            sshInfo.setUsername(work.getSshUsername() != null ? work.getSshUsername() : "root");
            // 密码脱敏：只返回首尾字符 + 星号，防止 API 泄露完整密码
            sshInfo.setPassword(maskSshPassword(work.getSshPassword()));
        } else {
            // 无数据时的默认提示
            sshInfo.setIp("待分配");
            sshInfo.setPort(22);
            sshInfo.setUsername("root");
            sshInfo.setPassword("拼团成功后自动分配");
        }
        
        // 协议
        sshInfo.setProtocol("SSH");
        
        // 连接指南
        StringBuilder guide = new StringBuilder();
        if (work != null && work.getServerIp() != null) {
            guide.append("【SSH连接指南】\n");
            guide.append("1. 使用SSH客户端（如PuTTY、Xshell）\n");
            guide.append("2. 主机地址：").append(work.getServerIp()).append("\n");
            guide.append("3. 端口：").append(work.getSshPort() != null ? work.getSshPort() : 22).append("\n");
            guide.append("4. 用户名：").append(work.getSshUsername() != null ? work.getSshUsername() : "root").append("\n");
            guide.append("5. 密码：").append(work.getSshPassword() != null ? "（已分配）" : "待分配").append("\n\n");
            guide.append("【首次连接建议】\n");
            guide.append("- 首次登录后请立即修改密码\n");
            guide.append("- 建议使用密钥认证替代密码登录\n");
            guide.append("- 详细教程请查看服务器教程页面");
        } else {
            guide.append("【服务器分配中】\n");
            guide.append("您的服务器正在准备中，请稍后查看。\n");
            guide.append("成团后服务器信息将自动显示在此处。");
        }
        sshInfo.setConnectGuide(guide.toString());
        
        // 备注
        sshInfo.setRemark("本服务器由拼团活动 '" + activity.getTitle() + "' 分配");
    }
    
    /**
     * 创建拼团订单的支付流水记录
     * 
     * 在调用支付接口前，必须先创建支付流水记录，以便支付回调时能够被正确处理。
     * 支付流水用于记录支付状态，与 osh_group_order 订单表配合使用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createGroupPayment(String orderNo,
                                     Long orderId,
                                     String amount,
                                     String clientIp,
                                     String provider,
                                     Integer channel) {
        OshGroupOrder lockedOrder = groupServerMapper.selectGroupOrderByOrderNoForUpdate(orderNo);
        if (lockedOrder == null || !lockedOrder.getId().equals(orderId)) {
            throw new ServiceException("拼团订单不存在");
        }
        if (!GroupOrderStatusEnum.PENDING.getCode().equals(lockedOrder.getStatus())) {
            throw new ServiceException("拼团订单状态不允许支付");
        }

        // 生成支付流水号（与订单号一致，易支付使用 out_trade_no 作为支付流水号）
        String paymentNo = orderNo;

        // 幂等校验：若同一 payment_no 的未删除流水已存在，直接跳过插入，
        // 避免重复点击/重新发起支付时产生多条 osh_payment 记录，
        // 进而触发 selectGroupOrderByOrderNo 的 TooManyResultsException。
        int existed = groupServerMapper.existsPaymentByPaymentNo(paymentNo);
        if (existed > 0) {
            logger.info("【拼团支付】支付流水已存在，跳过创建（幂等），orderNo={}, paymentNo={}, existed={}",
                    orderNo, paymentNo, existed);
            com.backstage.system.domain.order.OshPayment existingPayment =
                    oshPaymentMapper.selectByPaymentNo(paymentNo);
            if (existingPayment == null || StringUtils.isEmpty(existingPayment.getProvider())) {
                throw new ServiceException("拼团支付流水缺少支付提供方");
            }
            BigDecimal expectedAmount = new BigDecimal(amount).setScale(2, RoundingMode.UNNECESSARY);
            if (existingPayment.getAmount() == null
                    || existingPayment.getAmount().compareTo(expectedAmount) != 0
                    || !orderNo.equals(existingPayment.getOrderNo())) {
                throw new ServiceException("拼团支付流水与订单信息不一致");
            }
            return existingPayment.getProvider();
        }

        // 将金额字符串转换为 BigDecimal
        BigDecimal amountDecimal = new BigDecimal(amount);

        // 同步在 osh_order 写入一条对应记录（productType=group），确保支付回调链路：
        //   /notify/pay → handlePayNotify → Kafka → PaymentSuccessConsumer
        // 能根据 osh_order.product_type 正确路由到 GroupPaidHandler，
        // 进而触发 handlePaymentSuccess 把 osh_group_order.status 推进到 paid。
        com.backstage.system.domain.order.OshOrder unifiedOrder =
                ensureOshOrderRecord(orderNo, amountDecimal);

        // 插入支付流水记录
        // 安全说明：使用 fastjson2 JSONObject 构建 JSON，避免手动拼接导致的注入风险
        JSONObject payloadObj = new JSONObject();
        payloadObj.put("clientIp", clientIp);
        payloadObj.put("source", "group");
        String requestPayload = payloadObj.toJSONString();
        int result = groupServerMapper.insertGroupPayment(
                paymentNo, orderNo, unifiedOrder.getId(), amountDecimal, provider, channel, requestPayload);
        if (result <= 0) {
            throw new ServiceException("创建拼团支付流水失败");
        }
        logger.info("【拼团支付】创建支付流水成功，orderNo={}, paymentNo={}, provider={}, amount={}",
                orderNo, paymentNo, provider, amount);
        return provider;
    }

    /**
     * 在 osh_order 表中幂等写入拼团订单的通用订单记录。
     * 仅在 osh_order 不存在该 orderNo 时插入，已存在则跳过。
     *
     * @param orderNo        拼团订单号（与 osh_payment.payment_no 一致）
     * @param payableAmount  应付金额
     */
    private com.backstage.system.domain.order.OshOrder ensureOshOrderRecord(String orderNo,
                                                                            BigDecimal payableAmount) {
        com.backstage.system.domain.order.OshOrder existedOrder = oshOrderMapper.selectByOrderNo(orderNo);
        if (existedOrder != null) {
            logger.info("【拼团支付】osh_order 已存在，跳过写入，orderNo={}, existedId={}", orderNo, existedOrder.getId());
            return existedOrder;
        }

        OshGroupOrder groupOrder = groupServerMapper.selectGroupOrderByOrderNo(orderNo);
        if (groupOrder == null) {
            throw new ServiceException("未找到拼团订单，无法创建统一订单");
        }

        com.backstage.system.domain.order.OshOrder oshOrder = new com.backstage.system.domain.order.OshOrder();
        oshOrder.setUserId(groupOrder.getUserId());
        oshOrder.setOrderNo(orderNo);
        oshOrder.setStatus(OrderStatusEnum.PENDING.getCode());
        oshOrder.setProductType(ProductTypeEnum.GROUP.getCode());
        // product_id 优先取参团记录ID（group_work_id），缺失时退化为活动ID
        Long productId = groupOrder.getGroupWorkId() != null
                ? groupOrder.getGroupWorkId()
                : groupOrder.getGroupActivityId();
        oshOrder.setProductId(productId);
        oshOrder.setProductName("拼团订单-" + orderNo);
        oshOrder.setActivityId(groupOrder.getGroupActivityId());
        BigDecimal originalAmount = groupOrder.getBasePrice() != null ? groupOrder.getBasePrice() : payableAmount;
        oshOrder.setOriginalAmount(originalAmount);
        oshOrder.setDiscountAmount(BigDecimal.ZERO);
        oshOrder.setPayableAmount(payableAmount);
        LocalDateTime now = LocalDateTime.now();
        oshOrder.setCreatedTime(now);
        oshOrder.setUpdatedTime(now);
        oshOrder.setDeleteFlag(0);

        int inserted = oshOrderMapper.insertOshOrder(oshOrder);
        if (inserted > 0) {
            logger.info("【拼团支付】写入 osh_order 成功，orderNo={}, oshOrderId={}, userId={}, productType={}",
                    orderNo, oshOrder.getId(), oshOrder.getUserId(), oshOrder.getProductType());
        } else {
            throw new ServiceException("创建拼团统一订单失败");
        }
        return oshOrder;
    }

    // ==================== 定时任务业务方法实现 ====================

    /**
     * 取消超时未支付的 pending 订单（每5分钟执行）
     * 仅释放名额，不退款。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelExpiredOrders() {
        // 禁止旧 Quartz 入口批量只改本地状态。该做法无法先查单/关单，二维码仍可能付款。
        // 安全扫描由 GroupOrderTimeoutCancelTask 逐笔调用 OrderService.cancelPayment 完成。
        logger.warn("忽略旧拼团批量取消入口，请使用安全支付对账任务");
        return 0;
    }

    /**
     * 关闭已过招募期的用户发起拼团（每天凌晨2点执行）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireInitiatedGroups() {
        LocalDateTime expireBefore = LocalDateTime.now();
        List<OshGroupUserInitiated> expiredList = userInitiatedMapper.selectExpiredInitiatedGroups(expireBefore);

        if (expiredList == null || expiredList.isEmpty()) {
            return 0;
        }

        List<Long> ids = new ArrayList<>();
        for (OshGroupUserInitiated initiated : expiredList) {
            ids.add(initiated.getId());
        }

        // 批量更新为已结束（group_status = 2）
        int updatedCount = userInitiatedMapper.batchUpdateGroupStatus(ids, 2);
        logger.info("【拼团定时任务】过期用户发起拼团 {} 条", updatedCount);
        return updatedCount;
    }

    /**
     * 自动更新拼团活动状态（每天凌晨2:30执行）
     * 1进行中 → 2拼团成功 → 3已结束
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoUpdateActivityStatus() {
        List<OshGroupActivity> activeList = groupServerMapper.selectAllActiveActivities();
        if (activeList == null || activeList.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (OshGroupActivity activity : activeList) {
            int currentNum = activity.getCurrentNum();
            int minNum = activity.getGroupMinNum();
            int maxNum = activity.getGroupMaxNum();

            // 判断是否需要更新状态
            int newStatus = activity.getStatus();
            if (currentNum >= maxNum) {
                newStatus = 3; // 已结束
            } else if (currentNum >= minNum) {
                newStatus = 2; // 拼团成功
            }

            if (newStatus != activity.getStatus()) {
                groupServerMapper.updateGroupActivityStatus(activity.getId(), newStatus, now);
                evictActivityCache(activity.getId());
                updatedCount++;
                logger.info("【拼团定时任务】活动 {} 状态更新: {} → {}", activity.getId(), activity.getStatus(), newStatus);
            }
        }
        logger.info("【拼团定时任务】自动更新活动状态 {} 条", updatedCount);
        return updatedCount;
    }

    /**
     * 同步活动当前参与人数（每10分钟执行）
     * 基于 osh_group_work 表刷新 osh_group_activity.current_num
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshActivityCurrentNum() {
        List<OshGroupActivity> activeList = groupServerMapper.selectAllActiveActivities();
        if (activeList == null || activeList.isEmpty()) {
            return 0;
        }

        int syncCount = 0;
        for (OshGroupActivity activity : activeList) {
            int actualNum = groupServerMapper.countGroupWorkByActivityId(activity.getId());
            if (actualNum != activity.getCurrentNum()) {
                groupServerMapper.updateActivityCurrentNum(activity.getId(), actualNum);
                evictActivityCache(activity.getId());
                syncCount++;
                logger.info("【拼团定时任务】活动 {} 人数同步: {} → {}", activity.getId(), activity.getCurrentNum(), actualNum);
            }
        }
        logger.info("【拼团定时任务】同步活动人数 {} 条", syncCount);
        return syncCount;
    }

    /**
     * 服务器到期前提醒（每天9点执行）
     * 根据 osh.group.expire.notify_days 配置（默认7天），在到期前N天提醒用户
     */
    @Override
    public int notifyExpiringServers() {
        // 默认提前7天提醒
        int notifyDays = 7;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startExpire = now;
        LocalDateTime endExpire = now.plusDays(notifyDays);

        List<OshGroupWork> expiringWorks = groupServerMapper.selectExpiringWorks(startExpire, endExpire);
        if (expiringWorks == null || expiringWorks.isEmpty()) {
            logger.info("【拼团定时任务】无即将到期的服务器");
            return 0;
        }

        int notifyCount = 0;
        for (OshGroupWork work : expiringWorks) {
            // TODO: 接入站内信/微信模板消息推送
            logger.info("【拼团定时任务】服务器即将到期 - userId={}, activityId={}, expireTime={}",
                    work.getUserId(), work.getGroupActivityId(), work.getServerExpireTime());
            notifyCount++;
        }
        logger.info("【拼团定时任务】到期提醒 {} 条", notifyCount);
        return notifyCount;
    }

    /**
     * 清理长期未成团且已过期的组团（每天凌晨3点执行）
     * 清理创建超过30天且未成团的活动
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredGroups() {
        LocalDateTime expireBefore = LocalDateTime.now().minusDays(30);
        List<OshGroupActivity> expiredList = groupServerMapper.selectExpiredUnformedActivities(expireBefore);

        if (expiredList == null || expiredList.isEmpty()) {
            return 0;
        }

        int cleanedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (OshGroupActivity activity : expiredList) {
            // 标记为已结束
            groupServerMapper.updateGroupActivityStatus(activity.getId(), 3, now);
            evictActivityCache(activity.getId());
            cleanedCount++;
            logger.info("【拼团定时任务】清理过期活动: id={}, title={}", activity.getId(), activity.getTitle());
        }

        // 同时清理过期的用户发起拼团
        int expiredInitiated = expireInitiatedGroups();

        logger.info("【拼团定时任务】清理过期组团: 活动 {} 条, 用户发起 {} 条", cleanedCount, expiredInitiated);
        return cleanedCount;
    }

    /**
     * 每日数据快照（每天凌晨4点执行）
     * 生成前一天拼团数据快照
     */
    @Override
    public void dailySnapshot() {
        int totalActivities = groupServerMapper.countActivities(null);
        int activeActivities = groupServerMapper.countActivities(1);
        int successActivities = groupServerMapper.countActivities(2);
        int endedActivities = groupServerMapper.countActivities(3);

        int pendingOrders = groupServerMapper.countOrdersByStatus(GroupOrderStatusEnum.PENDING.getCode());
        int paidOrders = groupServerMapper.countOrdersByStatus(GroupOrderStatusEnum.PAID.getCode());
        int successOrders = groupServerMapper.countOrdersByStatus(GroupOrderStatusEnum.SUCCESS.getCode());
        int cancelledOrders = groupServerMapper.countOrdersByStatus(GroupOrderStatusEnum.CANCELLED.getCode());

        int activeInitiated = userInitiatedMapper.countActiveInitiatedGroups();

        logger.info("【拼团每日快照】活动总数={}, 进行中={}, 成功={}, 已结束={}",
                totalActivities, activeActivities, successActivities, endedActivities);
        logger.info("【拼团每日快照】订单: pending={}, paid={}, success={}, cancelled={}",
                pendingOrders, paidOrders, successOrders, cancelledOrders);
        logger.info("【拼团每日快照】用户发起拼团(进行中)={}", activeInitiated);

        // TODO: 将快照数据持久化到 osh_group_stats_snapshot 表（如需要BI报表支撑）
    }

    // ==================== 安全校验方法 ====================

    /**
     * 校验用户是否为某个拼团的成员
     */
    @Override
    public boolean isUserInGroup(Long userId, Long activityId, String sourceType) {
        if (userId == null || activityId == null) {
            return false;
        }
        if (!(GroupConstants.SOURCE_TYPE_SYSTEM.equals(sourceType) || GroupConstants.SOURCE_TYPE_INITIATED.equals(sourceType))) {
            return false;
        }
        return groupServerMapper.selectActiveGroupWorkForUser(sourceType, activityId, userId) != null;
    }

    /**
     * 校验用户是否为指定订单的所有者
     */
    @Override
    public boolean checkOrderOwnership(String orderNo, Long userId) {
        if (orderNo == null || userId == null) {
            return false;
        }
        return groupServerMapper.checkOrderOwnership(orderNo, userId) > 0;
    }

    // ==================== 分享海报功能 ====================

    /** 前端分享链接前缀（可通过配置注入） */
    private static final String SHARE_URL_PREFIX = "https://osh.net.cn/group/detail";

    /**
     * 获取分享海报数据（系统活动）
     */
    @Override
    public SharePosterVO getSharePosterData(Long activityId, Long userId) {
        OshGroupActivity activity = groupServerMapper.selectGroupActivityById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }

        SharePosterVO poster = new SharePosterVO();
        poster.setActivityId(activityId);
        poster.setActivityType(GroupConstants.SOURCE_TYPE_SYSTEM);
        poster.setTitle(activity.getTitle());
        poster.setDescription(activity.getTitle()); // 使用title作为描述
        poster.setServerSpec(buildServerSpec(activity.getCpu(), activity.getMemory(), activity.getStorage()));
        poster.setOriginalPrice(activity.getBasePrice());

        // 计算当前价格
        if (activity.getStatus() != null && activity.getStatus() == 2) {
            // 已成团：计算动态价格
            GroupPriceCalculator.PriceResult priceResult = GroupPriceCalculator.calculateFormedGroupPrice(
                    activity.getBasePrice(), activity.getServerEndTime(), activity.getTotalDuration());
            poster.setCurrentPrice(priceResult.getCurrentPrice());
        } else {
            poster.setCurrentPrice(activity.getBasePrice());
        }

        poster.setCurrentNum(activity.getCurrentNum());
        poster.setGroupMinNum(activity.getGroupMinNum());

        // 计算进度百分比
        int progress = activity.getGroupMinNum() > 0
                ? Math.min(100, (activity.getCurrentNum() * 100) / activity.getGroupMinNum())
                : 0;
        poster.setProgressPercent(progress);

        // 封面URL
        if (StringUtils.isNotEmpty(activity.getCover())) {
            poster.setCoverUrl(ossUtil.getFullFilePath(activity.getCover()));
        }

        // 生成分享链接
        poster.setShareUrl(SHARE_URL_PREFIX + "?id=" + activityId + "&type=system&inviter=" + userId);
        poster.setInviteCode(String.valueOf(userId));

        return poster;
    }

    /**
     * 获取分享海报数据（用户发起拼团）
     */
    @Override
    public SharePosterVO getSharePosterDataByInitiated(Long initiatedId, Long userId) {
        OshGroupUserInitiated initiated = userInitiatedMapper.selectById(initiatedId);
        if (initiated == null) {
            throw new ServiceException("拼团不存在");
        }

        OshGroupActivity activity = groupServerMapper.selectGroupActivityById(initiated.getActivityId());
        if (activity == null) {
            throw new ServiceException("关联活动不存在");
        }

        SharePosterVO poster = new SharePosterVO();
        poster.setActivityId(initiatedId);
        poster.setActivityType(GroupConstants.SOURCE_TYPE_INITIATED);
        poster.setTitle(activity.getTitle());
        poster.setDescription("发起人拼团 - " + activity.getTitle());
        poster.setServerSpec(buildServerSpec(activity.getCpu(), activity.getMemory(), activity.getStorage()));
        poster.setOriginalPrice(activity.getBasePrice());
        poster.setCurrentPrice(initiated.getCustomPrice());
        poster.setCurrentNum(initiated.getCurrentNum());
        poster.setGroupMinNum(initiated.getMinNum());

        // 计算进度百分比
        int progress = initiated.getMinNum() > 0
                ? Math.min(100, (initiated.getCurrentNum() * 100) / initiated.getMinNum())
                : 0;
        poster.setProgressPercent(progress);

        // 封面URL
        if (StringUtils.isNotEmpty(activity.getCover())) {
            poster.setCoverUrl(ossUtil.getFullFilePath(activity.getCover()));
        }

        // 生成分享链接
        poster.setShareUrl(SHARE_URL_PREFIX + "?id=" + initiatedId + "&type=initiated&inviter=" + userId);
        poster.setInviteCode(String.valueOf(userId));

        return poster;
    }

    /**
     * 构建服务器配置摘要
     */
    private String buildServerSpec(String cpu, String memory, String storage) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(cpu)) {
            sb.append(cpu);
        }
        if (StringUtils.isNotEmpty(memory)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(memory);
        }
        if (StringUtils.isNotEmpty(storage)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(storage);
        }
        return sb.length() > 0 ? sb.toString() : "待定";
    }
}
