package com.backstage.system.controller.servergroup;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.RateLimiter;
import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.common.utils.DesensitizedUtil;
import com.backstage.common.utils.SecurityUtils;
import com.backstage.system.domain.dto.GroupCreateDTO;
import com.backstage.system.domain.dto.AddUserToGroupDTO;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.PayChannelEnum;
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
import com.backstage.system.domain.vo.order.PayResponse;
import com.backstage.system.service.servergroup.IOshGroupServerService;
import com.backstage.system.service.order.OrderService;
import com.backstage.system.service.order.PayService;
import com.backstage.system.utils.UserContextUtil;
import com.backstage.system.utils.IpUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

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
    private OrderService orderService;

    @Autowired
    private OshOrderMapper oshOrderMapper;
    
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
            @ApiParam("拼团发起记录ID（可选）") @RequestParam(value = "activityId", required = false) Long activityId,
            @ApiParam("来源：system/initiated") @RequestParam(value = "sourceType") String sourceType) {
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
            detail = groupServerService.selectGroupDetailWithFallback(activityId, userId, sourceType);
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
     * @param payMethod 支付方式：当前统一为 alipay
     * @return 参团结果（包含订单号、支付状态等）
     */
    @ApiOperation("参与拼团")
    @RateLimiter(count = 5, time = 60)
    @PostMapping("/work/join")
    public R<com.backstage.system.domain.vo.group.JoinGroupVO> join(
            @ApiParam("拼团记录ID或活动模板ID") @RequestParam(value = "activityId") Long activityId,
            @ApiParam("支付方式：当前统一为支付宝") 
            @RequestParam(value = "payMethod", defaultValue = GroupConstants.PAY_METHOD_ALIPAY) String payMethod,
            @ApiParam("来源：system/initiated") @RequestParam(value = "sourceType") String sourceType) {
        // 从 ThreadLocal 获取网校用户ID（OshAuthenticationFilter 已写入）
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (userId == null) {
            return R.fail("请先登录");
        }
        
        try {
            // 参与拼团无需额外权限检查，登录用户均可参与
            com.backstage.system.domain.vo.group.JoinGroupVO result =
                    groupServerService.joinGroup(activityId, userId, payMethod, sourceType);
            return R.ok(result, result.getMessage());
        } catch (com.backstage.common.exception.ServiceException e) {
            return R.fail(e.getMessage()); // 业务异常保留原始消息
        } catch (DuplicateKeyException e) {
            return R.fail("您已参团，请勿重复操作");
        } catch (DataIntegrityViolationException e) {
            logger.error("参与拼团数据冲突", e);
            return R.fail("系统繁忙，请稍后重试");
        } catch (Exception e) {
            logger.error("参与拼团失败", e);
            return R.fail("系统繁忙，请稍后重试"); // 避免暴露内部异常
        }
    }
    
    /**
     * 接口5：获取可发起的拼团活动列表
     * 权限要求：只有管理员（level <= 2）可查看可发起的拼团活动
     * GET /pc/group/activity/initiable
     * @return 可发起的拼团活动列表
     */
    @ApiOperation("获取可发起的拼团活动列表")
    @GetMapping("/activity/initiable")
    public R<List<InitiableActivityVO>> initiableActivities() {
        // 权限验证：只有管理员（level <= 2）可查看可发起的拼团活动
        Integer userLevel = UserContextUtil.getCurrentLevel();
        if (userLevel == null || userLevel > 2) {
            return R.fail("权限不足，只有管理员可以查看可发起的拼团活动");
        }
        
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
    @ApiOperation("发起拼团")
    @PreAuthorize("hasAuthority('group:work:create')")
    @PostMapping("/work/create")
    public R<GroupCreateVO> create(
            @ApiParam("发起拼团请求参数") @Validated @RequestBody GroupCreateDTO dto) {
        // 从 ThreadLocal 获取网校用户ID（OshAuthenticationFilter 已写入）
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (userId == null) {
            return R.fail("请先登录");
        }
        
        try {
            GroupCreateVO result = groupServerService.createGroupWork(dto, userId);
            return R.ok(result, "发起成功");
        } catch (com.backstage.common.exception.ServiceException e) {
            return R.fail(e.getMessage());
        } catch (DuplicateKeyException e) {
            return R.fail("您已发起过该拼团，请勿重复操作");
        } catch (Exception e) {
            logger.error("发起拼团失败", e);
            return R.fail("系统繁忙，请稍后重试");
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
    @ApiOperation("手动添加用户到拼团")
    @PreAuthorize("hasAuthority('group:user:add')")
    @PostMapping("/user/add")
    public R<AddUserToGroupVO> addUserToGroup(
            @ApiParam("添加用户请求参数") @Validated @RequestBody AddUserToGroupDTO dto) {
        // 从 ThreadLocal 获取管理员ID
        Long operatorId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (operatorId == null) {
            return R.fail("请先登录");
        }
        
        try {
            AddUserToGroupVO result = groupServerService.addUserToGroup(
                dto.getActivityId(), 
                dto.getUserId(), 
                dto.getRemark(), 
                operatorId
            );
            return R.ok(result, result.getMessage());
        } catch (com.backstage.common.exception.ServiceException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            logger.error("手动添加用户到拼团失败", e);
            return R.fail("系统繁忙，请稍后重试");
        }
    }
    
    /**
     * 接口10：拼团订单支付
     * 权限要求：登录用户可支付自己的拼团订单
     * 
     * POST /pc/group/work/pay
     * 
     * @param orderNo 订单号
     * @param payMethod 支付方式：当前统一为 alipay
     * @param name 商品名称
     * @return 支付二维码链接
     */
    @ApiOperation("拼团订单支付")
    @RateLimiter(count = 5, time = 60)
    @PostMapping("/work/pay")
    public R<PayResponse> pay(
            @ApiParam("订单号") @RequestParam(value = "orderNo") String orderNo,
            @ApiParam("支付方式：当前统一为支付宝") @RequestParam(value = "payMethod", defaultValue = GroupConstants.PAY_METHOD_ALIPAY) String payMethod,
            @ApiParam("商品名称（保留参数，实际从数据库订单读取）") @RequestParam(value = "name", required = false, defaultValue = "") String name,
            HttpServletRequest request) {
        // 从 ThreadLocal 获取网校用户ID（验证登录状态）
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (userId == null) {
            return R.fail("请先登录");
        }
        
        try {
            // 1. 从订单表查询实际金额
            com.backstage.system.domain.servergroup.OshGroupOrder order = groupServerService.selectGroupOrderByOrderNo(orderNo);
            if (order == null) {
                return R.fail("订单不存在");
            }

            // 2. 校验订单归属（防止越权支付他人订单）
            if (!userId.equals(order.getUserId())) {
                logger.warn("【拼团支付】越权支付尝试: userId={}, orderUserId={}, orderNo={}",
                        userId, order.getUserId(), DesensitizedUtil.maskOrderNo(orderNo));
                return R.fail("订单不存在");
            }
            
            // 3. 校验订单状态（必须是待支付）
            if (!GroupOrderStatusEnum.PENDING.getCode().equals(order.getStatus())) {
                return R.fail("订单状态不允许支付");
            }
            if (!(GroupConstants.SOURCE_TYPE_SYSTEM.equals(order.getSourceType()) || GroupConstants.SOURCE_TYPE_INITIATED.equals(order.getSourceType()))) {
                return R.fail("该旧拼团订单缺少来源信息，请重新发起拼团后支付");
            }
            
            // 4. 获取客户端IP
            String clientIp = IpUtils.getClientIp(request);
            
            // 5. 获取实际支付金额
            String money = order.getPrice() != null ? order.getPrice().toString() : "0";
            String channel = PayChannelEnum.ALIPAY.getValue();

            // 6. 创建支付流水记录（内部固化 provider 到 osh_payment，并同步写入 osh_order）
            groupServerService.createGroupPayment(
                    orderNo,
                    order.getId(),
                    money,
                    clientIp,
                    payService.currentProvider(),
                    PayChannelEnum.fromValue(channel).getCode()
            );

            // 7. 复用统一支付创建逻辑，完整保存二维码/平台响应，并正确处理确定失败。
            PayResponse response = orderService.createPaymentForExistingOrder(
                    orderNo, orderNo, clientIp, channel);
            if (response.getCode() == 1) {
                return R.ok(response, "获取支付链接成功");
            } else {
                return R.fail(response.getMsg());
            }
        } catch (Exception e) {
            logger.error("拼团订单支付失败", e);
            return R.fail("支付请求失败，请稍后重试");
        }
    }
    
    /**
     * 接口11：查询拼团订单状态
     * 权限要求：登录用户可查询自己的订单状态
     * 
     * GET /pc/group/order/status
     * 
     * @param orderNo 订单号
     * @return 订单状态信息
     */
    @ApiOperation("查询拼团订单状态")
    @GetMapping("/order/status")
    public R<GroupOrderStatusVO> getOrderStatus(
            @ApiParam("订单号") @RequestParam(value = "orderNo") String orderNo) {
        // 从 ThreadLocal 获取网校用户ID（验证登录状态）
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        if (userId == null) {
            return R.fail("请先登录");
        }
        
        try {
            // 统一状态查询会在本地 pending 时主动向原支付平台查单补单。
            orderService.getOrderStatusForUser(orderNo, userId);
            OshOrder order = oshOrderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                return R.fail("订单不存在");
            }

            // 校验订单归属（防止越权查看他人订单）
            if (!userId.equals(order.getUserId())) {
                logger.warn("【拼团订单】越权访问尝试: userId={}, orderUserId={}, orderNo={}",
                        userId, order.getUserId(), DesensitizedUtil.maskOrderNo(orderNo));
                return R.fail("订单不存在");
            }
            
            // 构造返回结果
            GroupOrderStatusVO vo = new GroupOrderStatusVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setStatus(OrderStatusEnum.fromCode(order.getStatus()).getName());
            vo.setPaidTime(order.getPaidTime());
            vo.setPayableAmount(order.getPayableAmount());
            vo.setProductName(order.getProductName());
            
            // 支付倒计时（待支付订单：创建后30分钟内需完成支付）
            if (order.getStatus() != null && order.getStatus() == OrderStatusEnum.PENDING.getCode() && order.getCreatedTime() != null) {
                long elapsedSeconds = java.time.Duration.between(order.getCreatedTime(), java.time.LocalDateTime.now()).getSeconds();
                long countdownSeconds = Math.max(30 * 60 - elapsedSeconds, 0);
                vo.setCountdownSeconds(countdownSeconds);
            }
            
            return R.ok(vo, "查询成功");
        } catch (Exception e) {
            logger.error("查询订单状态失败", e);
            return R.fail("查询失败，请稍后重试");
        }
    }
    
    /**
     * 接口11：获取服务器教程
     * 权限要求：匿名可访问
     * 
     * GET /api/group/server/tutorial
     * 
     * @param activityId 拼团活动ID
     * @return 服务器使用教程
     */
    @Anonymous
    @ApiOperation("获取服务器教程")
    @GetMapping("/server/tutorial")
    public R<com.backstage.system.domain.vo.group.ServerTutorialVO> getServerTutorial(
            @ApiParam("拼团活动ID") @RequestParam(value = "activityId") Long activityId,
            @ApiParam("来源：system/initiated") @RequestParam(value = "sourceType") String sourceType) {
        try {
            com.backstage.system.domain.vo.group.ServerTutorialVO tutorial = 
                groupServerService.getServerTutorial(activityId, sourceType);
            return R.ok(tutorial, "查询成功");
        } catch (com.backstage.common.exception.ServiceException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            logger.error("获取服务器教程失败", e);
            return R.fail("获取教程失败，请稍后重试");
        }
    }
    
    /**
     * 接口12：获取服务器SSH信息
     * 权限要求：需用户登录
     * 
     * GET /api/group/server/ssh-info
     * 
     * @param activityId 拼团活动ID
     * @return 服务器SSH连接信息
     */
    @ApiOperation("获取服务器SSH信息")
    @GetMapping("/server/ssh-info")
    public R<com.backstage.system.domain.vo.group.ServerSshInfoVO> getServerSshInfo(
            @ApiParam("拼团活动ID") @RequestParam(value = "activityId") Long activityId,
            @ApiParam("来源：system/initiated") @RequestParam(value = "sourceType") String sourceType) {
        try {
            // 从 ThreadLocal 获取网校用户ID
            Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
            if (userId == null) {
                return R.fail("请先登录");
            }

            // 校验用户是否为该拼团的成员（防止越权获取服务器凭证）
            boolean isMember = groupServerService.isUserInGroup(userId, activityId, sourceType);
            if (!isMember) {
                logger.warn("【拼团SSH】越权访问尝试: userId={}, activityId={}", userId, activityId);
                return R.fail("无权访问");
            }
            
            com.backstage.system.domain.vo.group.ServerSshInfoVO sshInfo = 
                groupServerService.getServerSshInfo(activityId, userId, sourceType);
            return R.ok(sshInfo, "查询成功");
        } catch (Exception e) {
            logger.error("获取服务器SSH信息失败", e);
            return R.fail("获取服务器信息失败，请稍后重试");
        }
    }

    // ==================== 分享海报接口 ====================

    /**
     * 获取系统活动分享海报数据
     */
    @GetMapping("/share/poster")
    public R<com.backstage.system.domain.vo.group.SharePosterVO> getSharePoster(@RequestParam("activityId") Long activityId) {
        try {
            Long userId = getUserId();
            if (userId == null) {
                return R.fail("请先登录");
            }
            com.backstage.system.domain.vo.group.SharePosterVO poster =
                groupServerService.getSharePosterData(activityId, userId);
            return R.ok(poster, "获取成功");
        } catch (Exception e) {
            logger.error("获取分享海报数据失败, activityId={}", activityId, e);
            return R.fail("获取分享海报数据失败");
        }
    }

    /**
     * 获取用户发起拼团分享海报数据
     */
    @GetMapping("/share/poster/initiated")
    public R<com.backstage.system.domain.vo.group.SharePosterVO> getSharePosterInitiated(@RequestParam("initiatedId") Long initiatedId) {
        try {
            Long userId = getUserId();
            if (userId == null) {
                return R.fail("请先登录");
            }
            com.backstage.system.domain.vo.group.SharePosterVO poster =
                groupServerService.getSharePosterDataByInitiated(initiatedId, userId);
            return R.ok(poster, "获取成功");
        } catch (Exception e) {
            logger.error("获取分享海报数据失败, initiatedId={}", initiatedId, e);
            return R.fail("获取分享海报数据失败");
        }
    }
}
