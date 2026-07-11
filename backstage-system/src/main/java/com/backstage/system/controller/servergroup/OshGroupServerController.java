package com.backstage.system.controller.servergroup;

import com.backstage.common.annotation.Log;
import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.RateLimiter;
import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.enums.BusinessType;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.common.utils.DesensitizedUtil;
import com.backstage.common.utils.SecurityUtils;
import com.backstage.system.domain.dto.GroupCreateDTO;
import com.backstage.system.domain.dto.AddUserToGroupDTO;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.servergroup.OshGroupOrder;
import com.backstage.system.domain.servergroup.enums.GroupConstants;
import com.backstage.system.domain.servergroup.enums.GroupOrderStatusEnum;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.domain.vo.GroupActivityListVO;
import com.backstage.system.domain.vo.GroupCreateVO;
import com.backstage.system.domain.vo.GroupDetailVO;
import com.backstage.system.domain.vo.GroupWorkListVO;
import com.backstage.system.domain.vo.InitiableActivityVO;
import com.backstage.system.domain.vo.MyGroupListVO;
import com.backstage.system.domain.vo.UserInitiatedActivityListVO;
import com.backstage.system.domain.vo.UserSearchVO;
import com.backstage.system.domain.vo.group.AddUserToGroupVO;
import com.backstage.system.domain.vo.group.GroupOrderStatusVO;
import com.backstage.system.domain.vo.group.JoinGroupVO;
import com.backstage.system.domain.vo.group.ServerSshInfoVO;
import com.backstage.system.domain.vo.group.ServerTutorialVO;
import com.backstage.system.domain.vo.group.SharePosterVO;
import com.backstage.system.domain.vo.order.PayResponse;
import com.backstage.system.service.servergroup.IOshGroupServerService;
import com.backstage.system.service.order.PayService;
import com.backstage.system.utils.IpUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 服务器拼团C端Controller
 * 
 * @author system
 * @date 2026-04-18
 */
@Api(tags = "服务器拼团C端接口")
@RestController
@RequestMapping("/pc/group")
public class OshGroupServerController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(OshGroupServerController.class);
    
    @Autowired
    private IOshGroupServerService groupServerService;
    
    @Autowired
    private PayService payService;

    @Autowired
    private OshOrderMapper oshOrderMapper;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    
    /**
     * 查询拼团列表
     * 
     * GET /pc/group/activity/list
     * 
     * @param status 状态筛选（可选）
     * @return 拼团活动列表
     */
    @Anonymous
    @ApiOperation("查询拼团列表")
    @GetMapping("/activity/list")
    public TableDataInfo list(
            @ApiParam("状态筛选：1-进行中 2-拼团成功 3-已结束") 
            @RequestParam(value = "status", required = false) Integer status) {
        startPage();
        List<GroupActivityListVO> list = groupServerService.selectGroupActivityList(status);
        return getDataTable(list);
    }
    
    /**
     * 查询用户发起拼团列表
     * 
     * GET /pc/group/activity/initiated/list
     * 
     * @param status 状态筛选（可选）
     * @param type 类型筛选（可选）
     * @return 用户发起拼团活动列表
     */
    @ApiOperation("查询用户发起拼团列表")
    @GetMapping("/activity/initiated/list")
    public TableDataInfo initiatedList(
            @ApiParam("状态筛选：1-进行中 2-拼团成功 3-已结束") 
            @RequestParam(value = "status", required = false) Integer status,
            @ApiParam("类型筛选（可选）")
            @RequestParam(value = "type", required = false) String type) {
        startPage();
        List<UserInitiatedActivityListVO> list = groupServerService.selectUserInitiatedActivityList(status, type);
        return getDataTable(list);
    }
    
    /**
     * 接口2：查询我的拼团列表
     * 权限要求：登录用户都可查看自己的拼团记录
     * 
     * GET /pc/group/activity/mylist
     * 
     * @param groupStatus 组团状态筛选（可选）
     * @return 我的拼团列表
     */
    @ApiOperation("查询我的拼团列表")
    @GetMapping("/activity/mylist")
    public R<List<MyGroupListVO>> myList(
            @ApiParam("组团状态：0-进行中 1-已成团 2-已结束")
            @RequestParam(value = "groupStatus", required = false) Integer groupStatus) {
        // 从 ThreadLocal 获取网校用户ID（OshAuthenticationFilter 已写入）
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (userId == null) {
            return R.fail("请先登录");
        }
        
        // 查看我的拼团无需额外权限检查，登录用户均可查看自己的记录
        List<MyGroupListVO> list = groupServerService.selectMyGroupList(userId, groupStatus);
        return R.ok(list, "查询成功");
    }
    
    /**
     * 接口3：拼团详情
     * 支持两种查询方式：
     * 1. 从用户发起列表进入：传入 initiatedId（osh_group_user_initiated表ID）
     * 2. 从活动模板进入：传入 activityId（osh_group_activity表ID）
     * 
     * GET /pc/group/work/detail
     *
     * @param activityId 拼团记录ID（可选）
     * @return 拼团详情
     */
    @Anonymous
    @ApiOperation("拼团详情")
    @GetMapping("/work/detail")
    public R<GroupDetailVO> detail(
            @ApiParam("拼团发起记录ID（可选）") @RequestParam(value = "activityId", required = false) Long activityId) {
        // 尝试获取当前用户ID（如果已登录）
        Long userId = null;
        try {
            userId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 未登录，userId为null
            logger.debug("用户未登录，将以匿名身份查看拼团详情");
        }
        
        GroupDetailVO detail;
        if (activityId != null) {
            // 双数据源策略：先查用户发起记录，未找到则回退到系统活动模板
            detail = groupServerService.selectGroupDetailWithFallback(activityId, userId);
        }  else {
            return R.fail("拼团id为空");
        }
        return R.ok(detail);
    }
    
    /**
     * 接口4：参与拼团
     * 支持两种参团方式：
     * 1. 参与用户发起的拼团：传入 activityId（实际为 osh_group_user_initiated 表ID）
     * 2. 参与系统活动模板拼团：传入 activityId（osh_group_activity 表ID）
     * 权限要求：登录用户都可参与拼团
     * 
     * POST /pc/group/work/join
     * 
     * @param activityId 拼团记录ID或活动模板ID
     * @param payMethod 支付方式：wechat-微信支付 alipay-支付宝
     * @return 参团结果（包含订单号、支付状态等）
     */
    @Log(title = "拼团", businessType = BusinessType.INSERT)
    @ApiOperation("参与拼团")
    @RateLimiter(count = 5, time = 60)
    @PostMapping("/work/join")
    public R<JoinGroupVO> join(
            @ApiParam("拼团记录ID或活动模板ID") @RequestParam(value = "activityId") Long activityId,
            @ApiParam("支付方式：wechat-微信支付 alipay-支付宝") 
            @RequestParam(value = "payMethod", defaultValue = "wechat") String payMethod) {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (userId == null) {
            return R.fail("请先登录");
        }
        // P0-6: 分布式锁防重复提交
        String lockKey = GroupConstants.LOCK_JOIN_PREFIX + activityId + ":" + userId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", GroupConstants.LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return R.fail("操作正在处理中，请勿重复提交");
        }
        try {
            JoinGroupVO result = groupServerService.joinGroup(activityId, userId, payMethod);
            return R.ok(result, result.getMessage());
        } catch (DuplicateKeyException e) {
            return R.fail("您已参团，请勿重复操作");
        } catch (DataIntegrityViolationException e) {
            logger.error("{} 参团数据冲突, activityId={}, userId={}", GroupConstants.LOG_JOIN, activityId, userId, e);
            return R.fail("系统繁忙，请稍后重试");
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    /**
     * 接口5：获取可发起的拼团活动列表
     * 权限要求：group:activity:initiable
     * GET /pc/group/activity/initiable
     * @return 可发起的拼团活动列表
     */
    @ApiOperation("获取可发起的拼团活动列表")
    @PreAuthorize("hasAuthority('group:activity:initiable')")
    @GetMapping("/activity/initiable")
    public R<List<InitiableActivityVO>> initiableActivities() {
        List<InitiableActivityVO> list = groupServerService.selectInitiableActivities();
        return R.ok(list, "查询成功");
    }
    
    /**
     * 接口6：发起拼团（创建组团）
     * 权限要求：只有管理员（level <= 2）可发起拼团
     * 
     * POST /pc/group/work/create
     * 
     * @param dto 发起拼团请求参数
     * @return 发起结果
     */
    @Log(title = "拼团", businessType = BusinessType.INSERT)
    @ApiOperation("发起拼团")
    @PreAuthorize("hasAuthority('group:work:create')")
    @PostMapping("/work/create")
    public R<GroupCreateVO> create(
            @ApiParam("发起拼团请求参数") @Validated @RequestBody GroupCreateDTO dto) {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (userId == null) {
            return R.fail("请先登录");
        }
        // P0-6: 分布式锁防重复提交
        String lockKey = GroupConstants.LOCK_CREATE_PREFIX + dto.getActivityId() + ":" + userId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", GroupConstants.LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return R.fail("操作正在处理中，请勿重复提交");
        }
        try {
            GroupCreateVO result = groupServerService.createGroupWork(dto, userId);
            return R.ok(result, "发起成功");
        } catch (DuplicateKeyException e) {
            return R.fail("您已发起过该拼团，请勿重复操作");
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 接口7：查询全量组团记录列表（管理端）
     *
     * GET /pc/group/work/list
     *
     * @param groupStatus 组团状态筛选（可选）：0-进行中 1-已成团 2-已结束
     * @return 组团记录列表（分页）
     */
    @PreAuthorize("hasAuthority('group:work:query')")
    @ApiOperation("查询全量组团记录列表")
    @GetMapping("/work/list")
    public TableDataInfo workList(
            @ApiParam("组团状态：0-进行中 1-已成团 2-已结束")
            @RequestParam(value = "groupStatus", required = false) Integer groupStatus) {
        startPage();
        List<GroupWorkListVO> list = groupServerService.selectGroupWorkList(groupStatus);
        return getDataTable(list);
    }
    
    /**
     * 接口8：模糊查询用户名列表（用于手动添加参团用户）
     *
     * GET /pc/group/user/search
     *
     * @param keyword 搜索关键词（支持用户名、昵称模糊匹配）
     * @param limit 返回数量限制（可选，默认20）
     * @return 用户信息列表
     */
    @RateLimiter(time = 60, count = 10)
    @PreAuthorize("hasAuthority('group:user:search')")
    @ApiOperation("模糊查询用户名列表")
    @GetMapping("/user/search")
    public R<List<UserSearchVO>> searchUsers(
            @ApiParam("搜索关键词（支持用户名、昵称模糊匹配）")
            @RequestParam(value = "keyword", required = true) String keyword,
            @ApiParam("返回数量限制（可选，默认20）")
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        List<UserSearchVO> users = groupServerService.searchUsernames(keyword, limit);
        return R.ok(users, "查询成功");
    }
    
    /**
     * 接口9：手动添加用户到拼团（管理员操作）
     * 权限要求：只有管理员（level <= 2）可手动添加用户到拼团
     *
     * POST /pc/group/user/add
     *
     * @param dto 添加用户请求参数
     * @return 添加结果
     */
    @Log(title = "拼团", businessType = BusinessType.INSERT)
    @ApiOperation("手动添加用户到拼团")
    @PreAuthorize("hasAuthority('group:user:add')")
    @PostMapping("/user/add")
    public R<AddUserToGroupVO> addUserToGroup(
            @ApiParam("添加用户请求参数") @Validated @RequestBody AddUserToGroupDTO dto) {
        Long operatorId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (operatorId == null) {
            return R.fail("请先登录");
        }
        AddUserToGroupVO result = groupServerService.addUserToGroup(
            dto.getActivityId(), 
            dto.getUserId(), 
            dto.getRemark(), 
            operatorId
        );
        return R.ok(result, result.getMessage());
    }
    
    /**
     * 接口10：拼团订单支付
     * 权限要求：登录用户可支付自己的拼团订单
     * 
     * POST /pc/group/work/pay
     */
    @Log(title = "拼团", businessType = BusinessType.UPDATE)
    @ApiOperation("拼团订单支付")
    @RateLimiter(count = 5, time = 60)
    @PostMapping("/work/pay")
    public R<PayResponse> pay(
            @ApiParam("订单号") @RequestParam(value = "orderNo") String orderNo,
            @ApiParam("支付方式：wechat-微信支付 alipay-支付宝") @RequestParam(value = "payMethod", defaultValue = "wechat") String payMethod,
            @ApiParam("商品名称") @RequestParam(value = "name") String name,
            HttpServletRequest request) {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (userId == null) {
            return R.fail("请先登录");
        }
        // P0-6: 分布式锁防重复支付
        String lockKey = GroupConstants.LOCK_PAY_PREFIX + orderNo;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", GroupConstants.LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return R.fail("支付请求正在处理中，请勿重复提交");
        }
        try {
            // 1. 查询订单
            OshGroupOrder order = groupServerService.selectGroupOrderByOrderNo(orderNo);
            if (order == null) {
                return R.fail("订单不存在");
            }
            // 2. 校验订单归属（防止越权支付他人订单）
            if (!userId.equals(order.getUserId())) {
                logger.warn("{} 越权支付尝试: userId={}, orderUserId={}, orderNo={}",
                        GroupConstants.LOG_SECURITY, userId, order.getUserId(), DesensitizedUtil.maskOrderNo(orderNo));
                return R.fail("订单不存在");
            }
            // 3. 校验订单状态（必须是待支付）
            if (!GroupOrderStatusEnum.PENDING.getCode().equals(order.getStatus())) {
                return R.fail("订单状态不允许支付");
            }
            // 4. 获取客户端IP + 支付金额
            String clientIp = IpUtils.getClientIp(request);
            String money = order.getPrice() != null ? order.getPrice().toString() : "0";
            // 5. 创建支付流水 + 调用支付服务
            groupServerService.createGroupPayment(orderNo, order.getId(), money, clientIp);
            PayResponse response = payService.createPay(orderNo, name, money, clientIp, "wxpay");
            if (response.getCode() == 1) {
                return R.ok(response, "获取支付链接成功");
            } else {
                return R.fail(response.getMsg());
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    /**
     * 接口11：查询拼团订单状态
     * 权限要求：登录用户可查询自己的订单状态
     * 
     * GET /pc/group/order/status
     */
    @ApiOperation("查询拼团订单状态")
    @GetMapping("/order/status")
    public R<GroupOrderStatusVO> getOrderStatus(
            @ApiParam("订单号") @RequestParam(value = "orderNo") String orderNo) {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (userId == null) {
            return R.fail("请先登录");
        }
        // 查询 osh_order 表
        OshOrder order = oshOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return R.fail("订单不存在");
        }
        // 校验订单归属（防止越权查看他人订单）
        if (!userId.equals(order.getUserId())) {
            logger.warn("{} 越权访问订单: userId={}, orderUserId={}, orderNo={}",
                    GroupConstants.LOG_SECURITY, userId, order.getUserId(), DesensitizedUtil.maskOrderNo(orderNo));
            return R.fail("订单不存在");
        }
        // 构造返回结果（强类型VO）
        GroupOrderStatusVO result = new GroupOrderStatusVO();
        result.setOrderNo(order.getOrderNo());
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(order.getStatus());
        result.setStatus(statusEnum != null ? statusEnum.getName() : "未知");
        result.setPaidTime(order.getPaidTime());
        result.setPayableAmount(order.getPayableAmount());
        result.setProductName(order.getProductName());
        // 支付倒计时（待支付订单：创建后30分钟内需完成支付）
        if (statusEnum == OrderStatusEnum.PENDING && order.getCreatedTime() != null) {
            long elapsedSeconds = java.time.Duration.between(order.getCreatedTime(), java.time.LocalDateTime.now()).getSeconds();
            result.setCountdownSeconds(Math.max(GroupConstants.PAY_TIMEOUT_MINUTES * 60L - elapsedSeconds, 0));
        }
        return R.ok(result, "查询成功");
    }
    
    /**
     * 接口12：获取服务器教程
     * 权限要求：匿名可访问
     * 
     * GET /pc/group/server/tutorial
     */
    @Anonymous
    @ApiOperation("获取服务器教程")
    @GetMapping("/server/tutorial")
    public R<ServerTutorialVO> getServerTutorial(
            @ApiParam("拼团活动ID") @RequestParam(value = "activityId") Long activityId) {
        ServerTutorialVO tutorial = groupServerService.getServerTutorial(activityId);
        return R.ok(tutorial, "查询成功");
    }
    
    /**
     * 接口13：获取服务器SSH信息
     * 权限要求：需用户登录 + 拼团成员校验
     * 
     * GET /pc/group/server/ssh-info
     */
    @ApiOperation("获取服务器SSH信息")
    @GetMapping("/server/ssh-info")
    public R<ServerSshInfoVO> getServerSshInfo(
            @ApiParam("拼团活动ID") @RequestParam(value = "activityId") Long activityId) {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (userId == null) {
            return R.fail("请先登录");
        }
        // 校验用户是否为该拼团的成员（防止越权获取服务器凭证）
        boolean isMember = groupServerService.isUserInGroup(userId, activityId);
        if (!isMember) {
            logger.warn("{} 越权访问SSH: userId={}, activityId={}", GroupConstants.LOG_SECURITY, userId, activityId);
            return R.fail("无权访问");
        }
        ServerSshInfoVO sshInfo = groupServerService.getServerSshInfo(activityId, userId);
        return R.ok(sshInfo, "查询成功");
    }

    // ==================== 分享海报接口 ====================

    /**
     * 接口14：获取系统活动分享海报数据
     */
    @GetMapping("/share/poster")
    public R<SharePosterVO> getSharePoster(@RequestParam("activityId") Long activityId) {
        Long userId = getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        SharePosterVO poster = groupServerService.getSharePosterData(activityId, userId);
        return R.ok(poster, "获取成功");
    }

    /**
     * 接口15：获取用户发起拼团分享海报数据
     */
    @GetMapping("/share/poster/initiated")
    public R<SharePosterVO> getSharePosterInitiated(@RequestParam("initiatedId") Long initiatedId) {
        Long userId = getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        SharePosterVO poster = groupServerService.getSharePosterDataByInitiated(initiatedId, userId);
        return R.ok(poster, "获取成功");
    }
}
