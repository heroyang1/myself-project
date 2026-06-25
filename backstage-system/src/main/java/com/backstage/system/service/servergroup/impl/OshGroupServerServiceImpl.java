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
import com.backstage.system.domain.vo.GroupWorkListVO;
import com.backstage.system.domain.vo.InitiableActivityVO;
import com.backstage.system.domain.vo.ServerGroupUserVo;
import com.backstage.system.domain.vo.MyGroupListVO;
import com.backstage.system.domain.vo.UserInitiatedActivityListVO;
import com.backstage.system.domain.vo.UserSearchVO;
import com.backstage.system.domain.vo.group.JoinGroupVO;
import com.backstage.system.domain.vo.group.ServerTutorialVO;
import com.backstage.system.domain.vo.group.ServerSshInfoVO;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.mapper.servergroup.OshGroupServerMapper;
import com.backstage.system.mapper.servergroup.OshGroupUserInitiatedMapper;
import com.backstage.system.service.servergroup.IOshGroupServerService;
import com.backstage.system.utils.OssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final String CACHE_ACTIVITY_LIST_PREFIX = "group:activity:list:";
    private static final String CACHE_ACTIVITY_DETAIL_PREFIX = "group:activity:detail:";
    private static final long CACHE_ACTIVITY_LIST_TTL = 5;   // 活动列表缓存 5 分钟
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
    private OssUtil ossUtil;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    
    /**
     * 查询拼团活动列表
     */
    @Override
    public List<GroupActivityListVO> selectGroupActivityList(Integer status) {
        // 1. 尝试从 Redis 缓存获取
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

        // 2. 查询数据库
        List<GroupActivityListVO> list = groupServerMapper.selectGroupActivityList(status);
        
        // 3. 处理计算字段
        for (GroupActivityListVO vo : list) {
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
     */
    @Override
    public List<MyGroupListVO> selectMyGroupList(Long userId, Integer groupStatus) {
        // 查询数据
        List<MyGroupListVO> list = groupServerMapper.selectMyGroupList(userId, groupStatus);
        
        // 处理敏感信息脱敏
        for (MyGroupListVO vo : list) {
            // 脱敏管理员联系方式
            if (StringUtils.isNotEmpty(vo.getAdminContact())) {
                vo.setAdminContact(desensitizeContact(vo.getAdminContact()));
            }
            
            // 设置订单状态文字
            vo.setOrderStatusText(getOrderStatusText(vo.getOrderStatus()));
        }
        
        return list;
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
        vo.setRemainNum(activity.getGroupMaxNum() - activity.getCurrentNum());
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
        // 转换用户头像URL
        if (users != null && !users.isEmpty()) {
            for (ServerGroupUserVo user : users) {
                if (StringUtils.isNotEmpty(user.getAvatar())) {
                    user.setAvatar(ossUtil.getFullFilePath(user.getAvatar()));
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
        vo.setRemainNum(initiated.getMaxNum() - initiated.getCurrentNum());
        
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
        vo.setServerStartTime(initiated.getServerStartTime());
        vo.setServerExpireTime(initiated.getServerExpireTime());
        vo.setAdminContact(activity.getAdminContact());
        vo.setServerTutorialUrl(activity.getServerTutorialUrl());
        
        // 设置封面完整URL
        String cover = activity.getCover();
        if (StringUtils.isNotEmpty(cover)) {
            vo.setCover(ossUtil.getFullFilePath(cover));
        }
        
        // 计算价格和剩余时间
        if (initiated.getGroupStatus() == 1) {
            // 已成团：按实际剩余时间计算
            if (initiated.getServerExpireTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                long days = ChronoUnit.DAYS.between(now, initiated.getServerExpireTime());
                long remainingMonthsLong = (days + 29) / 30; // 向上取整
                BigDecimal remaining = new BigDecimal(remainingMonthsLong);
                vo.setRemainingMonths(remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining);
                
                // 计算当前价格
                BigDecimal currentPrice = vo.getBasePrice()
                        .multiply(vo.getRemainingMonths())
                        .divide(new BigDecimal(vo.getTotalDuration()), 2, RoundingMode.HALF_UP);
                vo.setCurrentPrice(currentPrice);
                
                // 会员价格（八折）
                vo.setMemberPrice(currentPrice.multiply(new BigDecimal("0.8")));
            } else {
                // 已成团但无到期时间，使用基础价格
                vo.setRemainingMonths(new BigDecimal(initiated.getDuration()));
                vo.setCurrentPrice(vo.getBasePrice());
                vo.setMemberPrice(vo.getBasePrice().multiply(new BigDecimal("0.8")));
            }
        } else {
            // 未成团：显示基础价格
            vo.setRemainingMonths(null);
            vo.setCurrentPrice(vo.getBasePrice());
            vo.setMemberPrice(vo.getBasePrice().multiply(new BigDecimal("0.8")));
        }
        
        // 兜底：如果 currentPrice 仍为空，使用 basePrice
        if (vo.getCurrentPrice() == null && vo.getBasePrice() != null) {
            vo.setCurrentPrice(vo.getBasePrice());
            vo.setMemberPrice(vo.getBasePrice().multiply(new BigDecimal("0.8")));
        }
        
        // 查询参团用户列表（根据发起记录ID查询）
        List<ServerGroupUserVo> users = groupServerMapper.selectGroupUsersByInitiatedId(initiatedId);
        // 转换用户头像URL
        if (users != null && !users.isEmpty()) {
            for (ServerGroupUserVo user : users) {
                if (StringUtils.isNotEmpty(user.getAvatar())) {
                    user.setAvatar(ossUtil.getFullFilePath(user.getAvatar()));
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
     * 参与拼团（支持双数据源）
     * 1. 参与用户发起的拼团：activityId 实际为 osh_group_user_initiated 表ID
     * 2. 参与系统活动模板拼团：activityId 为 osh_group_activity 表ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JoinGroupVO joinGroup(Long activityId, Long userId, String payMethod) {
        // 参数校验
        if (activityId == null) {
            throw new ServiceException("拼团ID不能为空");
        }
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        
        // 尝试从用户发起记录表查询
        OshGroupUserInitiated initiated = userInitiatedMapper.selectById(activityId);
        
        if (initiated != null) {
            // 数据源1：用户发起的拼团记录
            logger.info("用户 {} 参与用户发起的拼团，记录ID: {}", userId, activityId);
            return joinUserInitiatedGroup(initiated, userId, payMethod);
        } else {
            // 数据源2：系统活动模板
            logger.info("拼团记录不存在，活动ID: {}",  activityId);
            return joinSystemActivity(activityId, userId, payMethod);
        }
    }
    
    /**
     * 参与用户发起的拼团
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
        
        // 3. 校验人数上限
        if (initiated.getCurrentNum() >= initiated.getMaxNum()) {
            throw new ServiceException("该拼团人数已满");
        }
        
        // 4. 校验用户是否已参团
        int joinedCount = groupServerMapper.checkUserJoinedByInitiatedId(initiatedId, userId);
        if (joinedCount > 0) {
            throw new ServiceException("您已参与该拼团活动");
        }
        
        // 5. 计算实际价格和剩余时间
        BigDecimal actualPrice;
        BigDecimal remainingMonths;
        LocalDateTime serverStartTime = null;
        LocalDateTime serverExpireTime = null;
        int newCurrentNum = initiated.getCurrentNum() + 1;
        
        // 判断是否达到最低人数（触发成团）
        boolean willSuccess = newCurrentNum >= initiated.getMinNum();
        
        if (willSuccess) {
            // 成团：计算服务器使用时间
            serverStartTime = LocalDateTime.now();
            serverExpireTime = serverStartTime.plusMonths(initiated.getDuration());
            remainingMonths = new BigDecimal(initiated.getDuration());
        } else {
            // 未成团：按预计时间计算
            remainingMonths = new BigDecimal(initiated.getDuration());
        }
        
        // 计算实际价格
        actualPrice = initiated.getCustomPrice()
                .multiply(remainingMonths)
                .divide(new BigDecimal(initiated.getDuration()), 2, RoundingMode.HALF_UP);
        
        // 6. 生成订单号
        String orderNo = "GP" + DateUtils.dateTime() + IdUtils.fastSimpleUUID().substring(0, 8);
        
        // 7. 创建订单
        OshGroupOrder order = new OshGroupOrder();
        order.setUserId(userId);
        order.setGroupActivityId(initiated.getActivityId()); // 关联活动模板ID
        order.setOrderNo(orderNo);
        order.setPrice(actualPrice);
        order.setBasePrice(initiated.getCustomPrice());
        order.setRemainingMonths(remainingMonths);
        order.setStatus("pending"); // 待支付
        order.setPayMethod(StringUtils.isEmpty(payMethod) ? "wechat" : payMethod);
        
        int orderResult = groupServerMapper.insertGroupOrder(order);
        if (orderResult <= 0) {
            throw new ServiceException("创建订单失败");
        }
        
        // 8. 创建参团记录
        OshGroupWork work = new OshGroupWork();
        work.setGroupActivityId(initiatedId); // 关联用户发起记录ID
        work.setUserId(userId);
        work.setOrderId(order.getId());
        work.setActualPrice(actualPrice);
        work.setRemainingMonths(remainingMonths);
        work.setJoinTime(LocalDateTime.now());
        
        if (willSuccess) {
            work.setGroupStatus(1); // 已成团
            work.setServerStartTime(serverStartTime);
            work.setServerExpireTime(serverExpireTime);
        } else {
            work.setGroupStatus(0); // 进行中
        }
        
        int workResult = groupServerMapper.insertGroupWork(work);
        if (workResult <= 0) {
            throw new ServiceException("创建参团记录失败");
        }
        
        // 9. 更新订单关联的参团ID
        order.setGroupWorkId(work.getId());
        
        // 10. 更新用户发起拼团记录的人数
        int updateResult = userInitiatedMapper.updateCurrentNum(initiatedId, newCurrentNum);
        if (updateResult <= 0) {
            throw new ServiceException("更新拼团记录失败，可能是并发冲突");
        }
        
        // 11. 如果达到人数，更新状态为已成团
        if (willSuccess) {
            userInitiatedMapper.updateGroupStatus(initiatedId, 1);
            logger.info("用户发起的拼团 {} 已成团，触发后续业务流程", initiatedId);
            
            // 更新 osh_group_user_initiated 表的服务器开始、到期时间
            userInitiatedMapper.updateServerTime(initiatedId, serverStartTime, serverExpireTime);
            logger.info("更新用户发起拼团 {} 的服务器时间: startTime={}, expireTime={}", 
                    initiatedId, serverStartTime, serverExpireTime);
            
            // 更新本次创建的 osh_group_work 记录的服务器时间
            // 注意：由于插入时已经设置了时间，这里主要是确保数据一致性
            if (work.getId() != null) {
                groupServerMapper.updateGroupWorkServerTime(work.getId(), serverStartTime, serverExpireTime);
                logger.info("更新本次参团记录 {} 的服务器时间: startTime={}, expireTime={}", 
                        work.getId(), serverStartTime, serverExpireTime);
            }
        }
        
        // 12. 如果达到人数上限，更新状态为已结束
        if (newCurrentNum >= initiated.getMaxNum()) {
            userInitiatedMapper.updateGroupStatus(initiatedId, 2);
        }
        
        logger.info("用户{}成功参与用户发起的拼团{}，订单号：{}", userId, initiatedId, orderNo);
        
        // 返回待支付状态
        return JoinGroupVO.pendingPayment(orderNo, actualPrice);
    }
    
    /**
     * 参与系统活动模板拼团
     */
    private JoinGroupVO joinSystemActivity(Long activityId, Long userId, String payMethod) {
        // 查询拼团活动
        OshGroupActivity activity = groupServerMapper.selectGroupActivityById(activityId);
        if (activity == null) {
            throw new ServiceException("拼团活动不存在");
        }

        // 校验拼团状态
        if (activity.getStatus() == 3) {
            throw new ServiceException("该拼团已结束，请关注新一期拼团活动");
        }
        
        // 校验人数上限
        if (activity.getCurrentNum() >= activity.getGroupMaxNum()) {
            throw new ServiceException("该拼团人数已满");
        }
        
        // 校验用户是否已参团
        int joinedCount = groupServerMapper.checkUserJoined(activityId, userId);
        if (joinedCount > 0) {
            throw new ServiceException("您已参与该拼团活动");
        }
        
        // 计算实际价格和剩余时间（CAS 模式下价格按基础价计算，成团后按实际剩余时间调整）
        BigDecimal remainingMonths = new BigDecimal(activity.getTotalDuration());
        BigDecimal actualPrice = activity.getBasePrice();
        
        // 生成订单号
        String orderNo = "GP" + DateUtils.dateTime() + IdUtils.fastSimpleUUID().substring(0, 8);
        
        // 创建订单
        OshGroupOrder order = new OshGroupOrder();
        order.setUserId(userId);
        order.setGroupActivityId(activityId);
        order.setOrderNo(orderNo);
        order.setPrice(actualPrice);
        order.setBasePrice(activity.getBasePrice());
        order.setRemainingMonths(remainingMonths);
        order.setStatus("pending"); // 待支付
        order.setPayMethod(StringUtils.isEmpty(payMethod) ? "wechat" : payMethod);
        
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
        work.setGroupStatus(0); // 默认进行中，成团后由后续逻辑更新
        
        int workResult = groupServerMapper.insertGroupWork(work);
        if (workResult <= 0) {
            throw new ServiceException("创建参团记录失败");
        }
        
        // 更新订单关联的参团ID
        order.setGroupWorkId(work.getId());
        
        // 使用 CAS 原子递增更新拼团活动人数（防并发超卖）
        int updateResult = groupServerMapper.atomicIncrementCurrentNum(activityId);
        if (updateResult <= 0) {
            throw new ServiceException("该拼团人数已满，请选择其他活动");
        }
        
        // 原子递增成功后，重新读取活动判断是否成团/满员
        OshGroupActivity latestActivity = groupServerMapper.selectGroupActivityById(activityId);
        if (latestActivity != null) {
            int latestNum = latestActivity.getCurrentNum();
            
            if (latestNum >= activity.getGroupMaxNum()) {
                // 达到人数上限：更新状态为已结束
                groupServerMapper.updateGroupActivityStatusEnded(activityId, LocalDateTime.now());
            } else if (latestNum >= activity.getGroupMinNum()) {
                // 达到最低成团人数：更新状态为拼团成功
                LocalDateTime srvStartTime = LocalDateTime.now();
                LocalDateTime srvExpireTime = srvStartTime.plusMonths(activity.getTotalDuration());
                groupServerMapper.updateGroupActivityWithLock(
                        activityId, latestNum, 2,
                        srvStartTime, srvExpireTime, LocalDateTime.now());
                // 更新本次参团记录的服务器时间
                groupServerMapper.updateGroupWorkServerTime(work.getId(), srvStartTime, srvExpireTime);
                // 同时更新参团记录状态为已成团
                groupServerMapper.updateGroupWorkStatus(work.getId(), 1);
            }
        }
        
        // 清除活动缓存（人数/状态已变更）
        evictActivityCache(activityId);
        
        logger.info("用户{}成功参与系统活动拼团{}，订单号：{}", userId, activityId, orderNo);
        
        // 返回待支付状态
        return JoinGroupVO.pendingPayment(orderNo, actualPrice);
    }
    
    /**
     * 计算价格和剩余时间（列表页）
     */
    private void calculatePriceAndTime(GroupActivityListVO vo) {
        if (vo.getIsSuccess()) {
            // 已成团：按实际剩余时间计算
            if (vo.getServerEndTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                long days = ChronoUnit.DAYS.between(now, vo.getServerEndTime());
                
                // 向上取整：不足一个月按一个月计算（即使剩余1天也算1个月）
                long remainingMonthsLong = (days + 29) / 30; // 向上取整算法
                BigDecimal remaining = new BigDecimal(remainingMonthsLong);
                
                // 如果已过期，剩余月数为0
                vo.setRemainingMonths(remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining);
                
                // 计算价格
                vo.setCurrentPrice(vo.getBasePrice()
                        .multiply(vo.getRemainingMonths())
                        .divide(new BigDecimal(vo.getTotalDuration()), 2, RoundingMode.HALF_UP));
            }
        } else {
            // 未成团：显示基础价格
            vo.setRemainingMonths(null);
            vo.setCurrentPrice(vo.getBasePrice());
        }
    }
    
    /**
     * 计算价格和剩余时间（用户发起拼团列表页）
     */
    private void calculatePriceAndTime(UserInitiatedActivityListVO vo) {
        if (vo.getIsSuccess()) {
            // 已成团：按实际剩余时间计算
            if (vo.getServerEndTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                long days = ChronoUnit.DAYS.between(now, vo.getServerEndTime());
                
                // 向上取整：不足一个月按一个月计算（即使剩余1天也算1个月）
                long remainingMonthsLong = (days + 29) / 30; // 向上取整算法
                BigDecimal remaining = new BigDecimal(remainingMonthsLong);
                
                // 如果已过期，剩余月数为0
                vo.setRemainingMonths(remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining);
                
                // 计算价格
                vo.setCurrentPrice(vo.getBasePrice()
                        .multiply(vo.getRemainingMonths())
                        .divide(new BigDecimal(vo.getTotalDuration()), 2, RoundingMode.HALF_UP));
            }
        } else {
            // 未成团：显示基础价格
            vo.setRemainingMonths(null);
            vo.setCurrentPrice(vo.getBasePrice());
        }
    }
    
    /**
     * 计算价格和剩余时间（详情页）
     */
    private void calculatePriceAndTimeForDetail(GroupDetailVO vo, OshGroupActivity activity) {
        if (vo.getGroupStatus() == 1) {
            // 已成团
            if (vo.getServerExpireTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                long days = ChronoUnit.DAYS.between(now, vo.getServerExpireTime());
                
                // 向上取整：不足一个月按一个月计算（即使剩余1天也算1个月）
                long remainingMonthsLong = (days + 29) / 30; // 向上取整算法
                BigDecimal remaining = new BigDecimal(remainingMonthsLong);
                
                vo.setRemainingMonths(remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining);
                
                // 计算当前价格
                BigDecimal currentPrice = vo.getBasePrice()
                        .multiply(vo.getRemainingMonths())
                        .divide(new BigDecimal(vo.getTotalDuration()), 2, RoundingMode.HALF_UP);
                vo.setCurrentPrice(currentPrice);
                
                // 计算会员价格（当前价格的八折）
                BigDecimal memberPrice = currentPrice.multiply(new BigDecimal("0.8"))
                        .setScale(2, RoundingMode.HALF_UP);
                vo.setMemberPrice(memberPrice);
            } else {
                // 已成团但无到期时间，使用基础价格
                vo.setRemainingMonths(new BigDecimal(activity.getTotalDuration()));
                vo.setCurrentPrice(vo.getBasePrice());
                vo.setMemberPrice(vo.getBasePrice().multiply(new BigDecimal("0.8")).setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            // 未成团
            vo.setRemainingMonths(null);
            vo.setCurrentPrice(vo.getBasePrice());
            
            // 计算会员价格（当前价格的八折）
            BigDecimal memberPrice = vo.getBasePrice().multiply(new BigDecimal("0.8"))
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setMemberPrice(memberPrice);
        }
        
        // 兜底：如果 currentPrice 仍为空，使用 basePrice
        if (vo.getCurrentPrice() == null && vo.getBasePrice() != null) {
            vo.setCurrentPrice(vo.getBasePrice());
            vo.setMemberPrice(vo.getBasePrice().multiply(new BigDecimal("0.8")).setScale(2, RoundingMode.HALF_UP));
        }
    }
    
    /**
     * 获取组团状态文字
     */
    private String getGroupStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "进行中";
            case 1:
                return "已成团";
            case 2:
                return "已结束";
            default:
                return "未知";
        }
    }
    
    /**
     * 获取订单状态文字
     */
    private String getOrderStatusText(String status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case "pending":
                return "待支付";
            case "paid":
                return "已支付";
            case "success":
                return "拼团成功";
            case "refunded":
                return "已退款";
            case "cancelled":
                return "已取消";
            default:
                return "未知";
        }
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

        // Step 2: 先创建拼团订单（order_id 需要在发起记录插入前确定）
        String orderNo = generateOrderNo();
        OshGroupOrder order = new OshGroupOrder();
        order.setUserId(userId);
        order.setGroupActivityId(dto.getActivityId());
        order.setOrderNo(orderNo);
        order.setPrice(dto.getPrice());
        order.setBasePrice(dto.getPrice());
        order.setRemainingMonths(new BigDecimal(dto.getDuration()));
        order.setStatus("pending"); // 待支付
        order.setPayMethod("wechat"); // 默认微信支付
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

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
        initiated.setCurrentNum(1); // 发起人自己算1人
        initiated.setInitiateTime(LocalDateTime.now());
        // 设置招募截止时间（默认7天后）
        initiated.setExpireTime(LocalDateTime.now().plusDays(7));
        initiated.setCreateTime(LocalDateTime.now());
        initiated.setUpdateTime(LocalDateTime.now());

        int insertResult = userInitiatedMapper.insertUserInitiated(initiated);
        if (insertResult <= 0) {
            throw new ServiceException("创建发起记录失败");
        }

        Long initiatedId = initiated.getId();
        logger.info("用户发起拼团记录创建成功，记录ID: {}", initiatedId);

        // Step 4: 回写 order 的 group_work_id（这里存储发起记录ID）
        order.setGroupWorkId(initiatedId);
        
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
     * 格式：GRP + yyyyMMddHHmmss + 6位随机数
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", (int)(Math.random() * 1000000));
        return "GRP" + timestamp + random;
    }

    /**
     * 查询全量组团记录列表（管理端）
     */
    @Override
    public List<GroupWorkListVO> selectGroupWorkList(Integer groupStatus) {
        return groupServerMapper.selectGroupWorkList(groupStatus);
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
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addUserToGroup(Long activityId, Long userId, String remark, Long operatorId) {
        Map<String, Object> result = new java.util.HashMap<>();
        
        logger.info("开始手动添加用户到拼团：activityId={}, userId={}, operatorId={}", activityId, userId, operatorId);
        
        // 1. 校验拼团活动是否存在
        Map<String, Object> activity = userInitiatedMapper.selectActivityDetail(activityId);
        if (activity == null) {
            logger.error("拼团活动不存在：activityId={}", activityId);
            throw new ServiceException("拼团活动不存在");
        }
        
        logger.info("查询到拼团活动详情：{}", activity);
        
        // 2. 校验拼团状态（只能是招募中）
        Integer groupStatus = (Integer) activity.get("group_status");
        if (groupStatus == null || groupStatus != 0) {
            throw new ServiceException("该拼团活动不在招募中，无法添加用户");
        }
        
        // 3. 校验是否已过期
        java.time.LocalDateTime expireTime = (java.time.LocalDateTime) activity.get("expire_time");
        if (expireTime != null && java.time.LocalDateTime.now().isAfter(expireTime)) {
            throw new ServiceException("该拼团活动已过期，无法添加用户");
        }
        
        // 4. 校验是否已达到最大人数
        Integer currentNum = (Integer) activity.get("current_num");
        Integer maxNum = (Integer) activity.get("max_num");
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
        
        // 7. 更新拼团活动的当前人数
        int newCurrentNum = currentNum + 1;
        userInitiatedMapper.updateCurrentNum(activityId, newCurrentNum);
        
        // 8. 在 osh_group_work 表中插入参团记录（用于详情接口查询）
        // 获取拼团活动的详细信息
        Integer minNum = (Integer) activity.get("min_num");
        Integer duration = (Integer) activity.get("duration");
        java.math.BigDecimal customPrice = (java.math.BigDecimal) activity.get("custom_price");
        Long leaderUserId = (Long) activity.get("user_id"); // 团长是发起人
        java.time.LocalDateTime serverStartTime = null;
        java.time.LocalDateTime serverExpireTime = null;
        int workGroupStatus = 0; // 默认进行中
        
        // 判断是否达到最低成团人数
        boolean isSuccess = newCurrentNum >= minNum;
        if (isSuccess) {
            // 已成团，设置服务器时间
            serverStartTime = java.time.LocalDateTime.now();
            serverExpireTime = serverStartTime.plusMonths(duration);
            workGroupStatus = 1; // 已成团
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
        
        // 9. 检查是否已成团
        if (isSuccess) {
            // 已成团，更新状态
            userInitiatedMapper.updateGroupStatus(activityId, 1);
            
            // 更新当前组团记录的状态为已成团
            // TODO: 触发成团后的业务逻辑
            // - 分配服务器资源
            // - 发送成团通知
            // - 创建订单等
            logger.info("拼团活动 {} 已成团，触发后续业务流程", activityId);
        }
        
        // 10. 构建返回结果
        result.put("success", true);
        result.put("activityId", activityId);
        result.put("userId", userId);
        result.put("currentNum", newCurrentNum);
        result.put("maxNum", maxNum);
        result.put("isSuccess", isSuccess);
        result.put("message", isSuccess ? "添加成功，拼团已成团！" : "添加成功");
        result.put("remark", remark);
        
        logger.info("管理员 {} 手动添加用户 {} 到拼团活动 {}", operatorId, userId, activityId);
        
        return result;
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
    public ServerTutorialVO getServerTutorial(Long activityId) {
        if (activityId == null) {
            throw new ServiceException("拼团活动ID不能为空");
        }
        
        ServerTutorialVO tutorial = new ServerTutorialVO();
        
        // 尝试从用户发起记录表查询
        OshGroupUserInitiated initiated = userInitiatedMapper.selectById(activityId);
        
        if (initiated != null) {
            // 数据源1：用户发起的拼团记录
            // 获取关联的活动模板信息
            OshGroupActivity activity = groupServerMapper.selectGroupActivityById(initiated.getActivityId());
            if (activity != null) {
                buildTutorialVO(tutorial, activity);
            }
        } else {
            // 数据源2：系统活动模板
            OshGroupActivity activity = groupServerMapper.selectGroupActivityById(activityId);
            if (activity != null) {
                buildTutorialVO(tutorial, activity);
            } else {
                throw new ServiceException("拼团活动不存在");
            }
        }
        
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
    public ServerSshInfoVO getServerSshInfo(Long activityId, Long userId) {
        if (activityId == null) {
            throw new ServiceException("拼团活动ID不能为空");
        }
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        
        ServerSshInfoVO sshInfo = new ServerSshInfoVO();
        
        // 优先从用户发起记录查询
        OshGroupUserInitiated initiated = userInitiatedMapper.selectById(activityId);
        
        if (initiated != null) {
            // 用户发起的拼团记录
            Long actualActivityId = initiated.getActivityId();
            OshGroupActivity activity = groupServerMapper.selectGroupActivityById(actualActivityId);
            
            if (activity != null) {
                buildSshInfoFromActivity(sshInfo, activity, userId);
            }
        } else {
            // 直接查询拼团活动
            OshGroupActivity activity = groupServerMapper.selectGroupActivityById(activityId);
            if (activity != null) {
                buildSshInfoFromActivity(sshInfo, activity, userId);
            }
        }
        
        // 检查用户是否参与了该拼团
        OshGroupWork work = groupServerMapper.selectGroupWorkByActivityAndUser(activityId, userId);
        
        if (work == null) {
            throw new ServiceException("您尚未参与该拼团活动");
        }
        
        // 检查拼团状态
        if (work.getGroupStatus() != null && work.getGroupStatus() != 1) {
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
        if ("paid".equals(order.getStatus()) || "success".equals(order.getStatus())) {
            logger.info("【拼团支付回调】订单已支付，跳过处理，orderNo={}", orderNo);
            return true;
        }
        
        // 3. 更新订单状态为已支付
        LocalDateTime now = LocalDateTime.now();
        int updateOrder = groupServerMapper.updateOrderStatus(order.getId(), "paid", now);
        if (updateOrder <= 0) {
            logger.error("【拼团支付回调】更新订单状态失败，orderNo={}", DesensitizedUtil.maskOrderNo(orderNo));
            return false;
        }
        logger.info("【拼团支付回调】订单状态已更新为已支付，orderNo={}", orderNo);

        // 4. 同步更新 osh_order 状态（保持双表一致）
        try {
            oshOrderMapper.updatePendingToPaid(orderNo, now);
            logger.info("【拼团支付回调】osh_order 状态已同步更新，orderNo={}", orderNo);
        } catch (Exception e) {
            logger.warn("【拼团支付回调】osh_order 状态同步失败（非致命），orderNo={}", DesensitizedUtil.maskOrderNo(orderNo), e);
        }
        
        // 5. 获取参团记录（group_work_id 存储的是 osh_group_user_initiated 表ID 或 osh_group_work 表ID）
        Long groupWorkId = order.getGroupWorkId();
        if (groupWorkId == null) {
            logger.warn("【拼团支付回调】参团记录ID为空，orderNo={}", orderNo);
            return true; // 非参团订单（如发起拼团订单），直接返回成功
        }
        
        // 6. 判断数据源：优先查询用户发起拼团记录
        OshGroupUserInitiated initiated = userInitiatedMapper.selectById(groupWorkId);
        if (initiated != null) {
            // 数据源1：用户发起的拼团
            handleUserInitiatedPayment(initiated, order, now);
        } else {
            // 数据源2：系统活动模板
            handleSystemActivityPayment(groupWorkId, order, now);
        }
        
        // 清除活动缓存（支付成功后状态/人数变更）
        evictActivityCache(order.getGroupActivityId());
        
        logger.info("【拼团支付回调】处理完成，orderNo={}", orderNo);
        return true;
    }

    /**
     * 处理用户发起拼团的支付成功逻辑
     */
    private void handleUserInitiatedPayment(OshGroupUserInitiated initiated, OshGroupOrder order, LocalDateTime now) {
        Long groupWorkId = initiated.getId();
        Integer currentNum = initiated.getCurrentNum();
        Integer minNum = initiated.getMinNum();
        Integer duration = initiated.getDuration();
        
        if (currentNum != null && minNum != null && currentNum >= minNum) {
            // 已达最低成团人数，更新状态
            LocalDateTime serverStartTime = now;
            LocalDateTime serverExpireTime = serverStartTime.plusMonths(duration != null ? duration : 1);
            
            // 更新用户发起拼团记录状态为已成团
            userInitiatedMapper.updateGroupStatus(groupWorkId, 1);
            userInitiatedMapper.updateServerTime(groupWorkId, serverStartTime, serverExpireTime);
            logger.info("【拼团支付回调】用户发起拼团已成团，initiatedId={}, currentNum={}, minNum={}", 
                    groupWorkId, currentNum, minNum);
            
            // 查找并更新对应的 osh_group_work 记录
            OshGroupWork work = groupServerMapper.selectGroupWorkByActivityAndUser(groupWorkId, order.getUserId());
            if (work != null) {
                groupServerMapper.updateGroupWorkStatus(work.getId(), 1);
                groupServerMapper.updateGroupWorkServerTime(work.getId(), serverStartTime, serverExpireTime);
                logger.info("【拼团支付回调】参团记录已更新，workId={}", work.getId());
            }
        }
    }

    /**
     * 处理系统活动模板拼团的支付成功逻辑
     *
     * 当 group_work_id 不在 osh_group_user_initiated 中时，说明是系统活动模板的参团记录，
     * 需要查询 osh_group_work 并更新状态。
     */
    private void handleSystemActivityPayment(Long groupWorkId, OshGroupOrder order, LocalDateTime now) {
        logger.info("【拼团支付回调】处理系统活动模板支付，groupWorkId={}", groupWorkId);

        // 尝试通过 group_work_id 和 userId 查找参团记录
        // 注意：groupWorkId 在系统活动模板场景下可能是 osh_group_work 的 ID
        OshGroupWork work = groupServerMapper.selectGroupWorkByActivityAndUser(
                order.getGroupActivityId(), order.getUserId());

        if (work == null) {
            logger.warn("【拼团支付回调】系统活动参团记录不存在，groupActivityId={}, userId={}",
                    order.getGroupActivityId(), order.getUserId());
            return;
        }

        // 更新 osh_group_work 状态为已成团
        groupServerMapper.updateGroupWorkStatus(work.getId(), 1);
        logger.info("【拼团支付回调】系统活动参团记录已更新，workId={}", work.getId());

        // 查询活动信息，判断是否达到成团条件
        OshGroupActivity activity = groupServerMapper.selectGroupActivityById(order.getGroupActivityId());
        if (activity == null) {
            logger.warn("【拼团支付回调】活动不存在，activityId={}", order.getGroupActivityId());
            return;
        }

        // 使用原子递增更新 current_num（防并发超卖）
        int updatedRows = groupServerMapper.atomicIncrementCurrentNum(activity.getId());
        if (updatedRows > 0) {
            int newNum = activity.getCurrentNum() + 1;
            // 检查是否成团
            if (newNum >= activity.getGroupMinNum()) {
                LocalDateTime serverStartTime = now;
                LocalDateTime serverExpireTime = serverStartTime.plusMonths(activity.getTotalDuration());

                // 更新活动状态为拼团成功
                groupServerMapper.updateGroupActivityWithLock(
                        activity.getId(), newNum, 2,
                        serverStartTime, serverExpireTime, now);

                // 更新本次参团记录的服务器时间
                groupServerMapper.updateGroupWorkServerTime(work.getId(), serverStartTime, serverExpireTime);

                logger.info("【拼团支付回调】系统活动拼团已成团！activityId={}, newNum={}, minNum={}",
                        activity.getId(), newNum, activity.getGroupMinNum());
            }
        } else {
            logger.warn("【拼团支付回调】原子递增失败（可能已满员），activityId={}", activity.getId());
        }
    }
    
    /**
     * 根据活动信息构建SSH信息（真实数据存储）
     */
    private void buildSshInfoFromActivity(ServerSshInfoVO sshInfo, OshGroupActivity activity, Long userId) {
        // 从 OshGroupWork 表中获取真实的服务器分配信息
        OshGroupWork work = groupServerMapper.selectGroupWorkByActivityAndUser(activity.getId(), userId);
        
        if (work != null && work.getServerIp() != null) {
            // 真实数据：使用数据库中存储的服务器信息
            sshInfo.setIp(work.getServerIp());
            sshInfo.setPort(work.getSshPort() != null ? work.getSshPort() : 22);
            sshInfo.setUsername(work.getSshUsername() != null ? work.getSshUsername() : "root");
            sshInfo.setPassword(work.getSshPassword() != null ? work.getSshPassword() : "");
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
    public void createGroupPayment(String orderNo, Long orderId, String amount, String clientIp) {
        // 生成支付流水号（与订单号一致，易支付使用 out_trade_no 作为支付流水号）
        String paymentNo = orderNo;

        // 幂等校验：若同一 payment_no 的未删除流水已存在，直接跳过插入，
        // 避免重复点击/重新发起支付时产生多条 osh_payment 记录，
        // 进而触发 selectGroupOrderByOrderNo 的 TooManyResultsException。
        int existed = groupServerMapper.existsPaymentByPaymentNo(paymentNo);
        if (existed > 0) {
            logger.info("【拼团支付】支付流水已存在，跳过创建（幂等），orderNo={}, paymentNo={}, existed={}",
                    orderNo, paymentNo, existed);
            return;
        }

        // 将金额字符串转换为 BigDecimal
        BigDecimal amountDecimal = new BigDecimal(amount);

        // 同步在 osh_order 写入一条对应记录（productType=group），确保支付回调链路：
        //   /notify/pay → handlePayNotify → Kafka → PaymentSuccessConsumer
        // 能根据 osh_order.product_type 正确路由到 GroupPaidHandler，
        // 进而触发 handlePaymentSuccess 把 osh_group_order.status 推进到 paid。
        ensureOshOrderRecord(orderNo, amountDecimal);

        // 插入支付流水记录
        int result = groupServerMapper.insertGroupPayment(paymentNo, orderNo, orderId, amountDecimal, clientIp);
        if (result <= 0) {
            logger.warn("【拼团支付】创建支付流水失败，orderNo={}, paymentNo={}", DesensitizedUtil.maskOrderNo(orderNo), DesensitizedUtil.maskOrderNo(paymentNo));
        } else {
            logger.info("【拼团支付】创建支付流水成功，orderNo={}, paymentNo={}, amount={}", orderNo, paymentNo, amount);
        }
    }

    /**
     * 在 osh_order 表中幂等写入拼团订单的通用订单记录。
     * 仅在 osh_order 不存在该 orderNo 时插入，已存在则跳过。
     *
     * @param orderNo        拼团订单号（与 osh_payment.payment_no 一致）
     * @param payableAmount  应付金额
     */
    private void ensureOshOrderRecord(String orderNo, BigDecimal payableAmount) {
        com.backstage.system.domain.order.OshOrder existedOrder = oshOrderMapper.selectByOrderNo(orderNo);
        if (existedOrder != null) {
            logger.info("【拼团支付】osh_order 已存在，跳过写入，orderNo={}, existedId={}", orderNo, existedOrder.getId());
            return;
        }

        OshGroupOrder groupOrder = groupServerMapper.selectGroupOrderByOrderNo(orderNo);
        if (groupOrder == null) {
            logger.warn("【拼团支付】未找到拼团订单，跳过 osh_order 写入，orderNo={}", orderNo);
            return;
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
            logger.warn("【拼团支付】写入 osh_order 影响行数为 0，orderNo={}", orderNo);
        }
    }

    // ==================== 定时任务业务方法实现 ====================

    /**
     * 取消超时未支付的 pending 订单（每5分钟执行）
     * 仅释放名额，不退款。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelExpiredOrders() {
        // 超时阈值：默认30分钟
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(30);
        List<OshGroupOrder> timeoutOrders = groupServerMapper.selectTimeoutOrders(timeoutTime);

        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
            return 0;
        }

        // 提取订单ID列表
        List<Long> orderIds = new ArrayList<>();
        for (OshGroupOrder order : timeoutOrders) {
            orderIds.add(order.getId());
        }

        // 批量更新订单状态为 cancelled
        int cancelledCount = groupServerMapper.batchUpdateOrderStatus(orderIds, "cancelled", LocalDateTime.now());
        logger.info("【拼团定时任务】取消超时订单 {} 条（共扫描 {} 条）", cancelledCount, timeoutOrders.size());

        // TODO: 释放对应拼团名额（current_num - 1），后续可配合 osh_group_work 状态联动
        return cancelledCount;
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

        int pendingOrders = groupServerMapper.countOrdersByStatus("pending");
        int paidOrders = groupServerMapper.countOrdersByStatus("paid");
        int successOrders = groupServerMapper.countOrdersByStatus("success");
        int cancelledOrders = groupServerMapper.countOrdersByStatus("cancelled");

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
    public boolean isUserInGroup(Long userId, Long activityId) {
        if (userId == null || activityId == null) {
            return false;
        }
        return groupServerMapper.isUserInGroup(userId, activityId) > 0;
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
}
