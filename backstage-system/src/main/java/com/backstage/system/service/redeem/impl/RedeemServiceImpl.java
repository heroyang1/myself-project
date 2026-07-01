package com.backstage.system.service.redeem.impl;

import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.UserBookRelation;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.PayChannelEnum;
import com.backstage.system.domain.order.enums.PaymentStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.order.enums.PurchaseModeEnum;
import com.backstage.system.domain.redeem.OshRedeemChannel;
import com.backstage.system.domain.redeem.OshRedeemCode;
import com.backstage.system.domain.redeem.OshRedeemInviteLink;
import com.backstage.system.domain.redeem.dto.RedeemChannelCreateRequest;
import com.backstage.system.domain.redeem.dto.RedeemCodeBatchCreateRequest;
import com.backstage.system.domain.redeem.dto.RedeemCodeCreateRequest;
import com.backstage.system.domain.redeem.dto.RedeemCodePageQuery;
import com.backstage.system.domain.redeem.dto.RedeemResourceSearchQuery;
import com.backstage.system.domain.redeem.vo.RedeemCodeBatchItemVO;
import com.backstage.system.domain.redeem.vo.RedeemCodeBatchResultVO;
import com.backstage.system.domain.redeem.vo.RedeemCodeVO;
import com.backstage.system.domain.redeem.vo.RedeemInviteLinkVO;
import com.backstage.system.domain.redeem.vo.RedeemResourceVO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.enums.behavior.ContributionResourceType;
import com.backstage.system.mapper.book.BookMapper;
import com.backstage.system.mapper.book.UserBookRelationMapper;
import com.backstage.system.mapper.course.CourseBuyMapper;
import com.backstage.system.mapper.course.OshCourseMapper;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.mapper.redeem.OshRedeemChannelMapper;
import com.backstage.system.mapper.redeem.OshRedeemCodeMapper;
import com.backstage.system.mapper.redeem.OshRedeemInviteLinkMapper;
import com.backstage.system.mapper.redeem.OshRedeemResourceMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.behavior.ContributionService;
import com.backstage.system.service.order.OrderNoGenerator;
import com.backstage.system.service.redeem.RedeemService;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RedeemServiceImpl implements RedeemService {
    private static final int CHANNEL_ENABLED = 1;
    private static final int REDEEM_PENDING = 0;
    private static final int REDEEM_USED = 1;
    private static final int REDEEM_CANCELLED = 2;
    private static final int LINK_ENABLED = 1;
    private static final String DEFAULT_CHANNEL_CODE = "wechat_transfer";
    private static final String DEFAULT_CHANNEL_NAME = "微信转账";
    private static final String SYSTEM_OPERATOR = "redeem";
    private static final int DEFAULT_RESOURCE_PAGE_SIZE = 10;
    private static final int MAX_RESOURCE_PAGE_SIZE = 50;
    private static final int MAX_BATCH_CREATE_SIZE = 200;
    private static final int VIP_REDEEMABLE_LEVEL = 2;

    @Resource
    private OshRedeemChannelMapper redeemChannelMapper;

    @Resource
    private OshRedeemCodeMapper redeemCodeMapper;

    @Resource
    private OshRedeemInviteLinkMapper redeemInviteLinkMapper;

    @Resource
    private OshRedeemResourceMapper redeemResourceMapper;

    @Resource
    private OshUserMapper oshUserMapper;

    @Resource
    private OshCourseMapper oshCourseMapper;

    @Resource
    private BookMapper bookMapper;

    @Resource
    private CourseBuyMapper courseBuyMapper;

    @Resource
    private UserBookRelationMapper userBookRelationMapper;

    @Resource
    private OshOrderMapper orderMapper;

    @Resource
    private OshPaymentMapper paymentMapper;

    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Resource
    private ContributionService contributionService;

    @Override
    public List<OshRedeemChannel> listChannels() {
        return redeemChannelMapper.selectList(new LambdaQueryWrapper<OshRedeemChannel>()
                .eq(OshRedeemChannel::getDeleteFlag, (byte) 0)
                .orderByDesc(OshRedeemChannel::getStatus)
                .orderByAsc(OshRedeemChannel::getSort)
                .orderByAsc(OshRedeemChannel::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OshRedeemChannel createChannel(RedeemChannelCreateRequest request) {
        if (request == null || StringUtils.isBlank(request.getChannelName())) {
            throw new ServiceException("渠道名称不能为空");
        }
        String channelName = request.getChannelName().trim();
        String channelCode = StringUtils.isBlank(request.getChannelCode())
                ? toChannelCode(channelName)
                : request.getChannelCode().trim();
        if (StringUtils.isBlank(channelCode)) {
            throw new ServiceException("渠道编码不能为空");
        }
        Long operatorId = currentOperatorId();
        OshRedeemChannel channel = new OshRedeemChannel();
        channel.setChannelName(channelName);
        channel.setChannelCode(channelCode);
        channel.setStatus(request.getStatus() == null ? CHANNEL_ENABLED : request.getStatus());
        channel.setSort(request.getSort() == null ? 0 : request.getSort());
        channel.setRemark(request.getRemark());
        channel.setCreateBy(operatorId);
        channel.setUpdateBy(operatorId);
        channel.setDeleteFlag((byte) 0);
        try {
            redeemChannelMapper.insert(channel);
        } catch (Exception ex) {
            throw new ServiceException("渠道编码已存在或渠道保存失败");
        }
        return channel;
    }

    @Override
    public RedeemResourceVO resolveResource(String resourceNo) {
        String no = normalizeResourceNo(resourceNo);
        RedeemResourceVO resolved = resolveByPreferredType(no);
        if (resolved != null) {
            return resolved;
        }
        resolved = resolveByFallbackScan(no);
        if (resolved == null) {
            throw new ServiceException("资源不存在或不支持核销");
        }
        return resolved;
    }

    @Override
    public PageInfo<RedeemResourceVO> searchResources(RedeemResourceSearchQuery query) {
        RedeemResourceSearchQuery normalized = normalizeResourceSearch(query);
        PageHelper.startPage(normalized.getPageNum(), normalized.getPageSize());
        return new PageInfo<>(redeemResourceMapper.searchResources(normalized.getResourceType(), normalized.getKeyword()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OshRedeemCode createCode(RedeemCodeCreateRequest request) {
        validateCreateRequest(request);
        RedeemResourceVO resource = resolveResource(request.getResourceNo());
        OshRedeemChannel channel = resolveChannel(request.getChannelId());

        Long operatorId = currentOperatorId();
        OshRedeemCode code = new OshRedeemCode();
        code.setCode(generateUniqueCode());
        code.setResourceNo(resource.getResourceNo());
        code.setResourceType(resource.getResourceType());
        code.setProductType(resource.getProductType());
        code.setProductId(resource.getProductId());
        code.setProductName(resource.getProductName());
        code.setAmount(money(request.getAmount() == null ? resource.getPrice() : request.getAmount()));
        code.setChannelId(channel.getId());
        code.setChannelName(channel.getChannelName());
        code.setStatus(REDEEM_PENDING);
        code.setRemark(request.getRemark());
        code.setCreateBy(operatorId);
        code.setUpdateBy(operatorId);
        code.setDeleteFlag((byte) 0);
        redeemCodeMapper.insert(code);
        ensureInviteLink(code, operatorId);
        return code;
    }

    @Override
    public RedeemCodeBatchResultVO batchCreateCodes(RedeemCodeBatchCreateRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ServiceException("请至少提交一条核销码生成记录");
        }
        if (request.getItems().size() > MAX_BATCH_CREATE_SIZE) {
            throw new ServiceException("单次最多生成" + MAX_BATCH_CREATE_SIZE + "条核销码");
        }

        RedeemCodeBatchResultVO result = new RedeemCodeBatchResultVO();
        List<RedeemCodeBatchItemVO> items = new ArrayList<>();
        int successCount = 0;

        for (RedeemCodeCreateRequest itemRequest : request.getItems()) {
            RedeemCodeBatchItemVO item = new RedeemCodeBatchItemVO();
            if (itemRequest != null) {
                item.setResourceNo(StringUtils.trimToEmpty(itemRequest.getResourceNo()));
            }
            try {
                OshRedeemCode code = createCode(itemRequest);
                item.setResourceNo(code.getResourceNo());
                item.setProductName(code.getProductName());
                item.setCode(code.getCode());
                OshRedeemInviteLink link = ensureInviteLink(code, currentOperatorId());
                item.setInviteLinkToken(link.getLinkToken());
                item.setStatus(code.getStatus());
                item.setSuccess(true);
                item.setMessage("生成成功");
                successCount++;
            } catch (ServiceException ex) {
                item.setSuccess(false);
                item.setMessage(StringUtils.defaultIfBlank(ex.getMessage(), "生成失败"));
            } catch (Exception ex) {
                item.setSuccess(false);
                item.setMessage("生成失败");
            }
            items.add(item);
        }

        result.setTotal(items.size());
        result.setSuccessCount(successCount);
        result.setFailCount(items.size() - successCount);
        result.setItems(items);
        return result;
    }

    @Override
    public PageInfo<RedeemCodeVO> pageAdminCodes(RedeemCodePageQuery query) {
        RedeemCodePageQuery normalized = normalizePage(query);
        normalizeEmailQuery(normalized);
        PageHelper.startPage(normalized.getPageNum(), normalized.getPageSize());
        return new PageInfo<>(redeemCodeMapper.selectAdminPage(normalized));
    }

    @Override
    public PageInfo<RedeemCodeVO> pageUserCodes(Long userId, RedeemCodePageQuery query) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        RedeemCodePageQuery normalized = normalizePage(query);
        PageHelper.startPage(normalized.getPageNum(), normalized.getPageSize());
        return new PageInfo<>(redeemCodeMapper.selectUserPage(userId, normalized));
    }

    @Override
    public RedeemInviteLinkVO getInviteLink(String token, boolean includeCode) {
        String normalized = normalizeLinkToken(token);
        RedeemInviteLinkVO vo = redeemInviteLinkMapper.selectByToken(normalized);
        if (vo == null) {
            throw new ServiceException("核销邀请链接不存在");
        }
        if (!Integer.valueOf(LINK_ENABLED).equals(vo.getLinkStatus())) {
            throw new ServiceException("核销邀请链接已停用");
        }
        if (vo.getExpireTime() != null && vo.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException("核销邀请链接已过期");
        }
        if (!includeCode) {
            vo.setCode(null);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OshRedeemCode redeem(Long userId, Long id) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        if (id == null) {
            throw new ServiceException("请选择要核销的核销码");
        }
        OshRedeemCode code = redeemCodeMapper.selectByIdForUpdate(id);
        return redeemLockedCode(userId, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OshRedeemCode redeemByCode(Long userId, String codeText) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        if (StringUtils.isBlank(codeText)) {
            throw new ServiceException("核销码不能为空");
        }
        OshRedeemCode code = redeemCodeMapper.selectByCodeForUpdate(codeText.trim().toUpperCase(Locale.ROOT));
        return redeemLockedCode(userId, code);
    }

    private OshRedeemCode redeemLockedCode(Long userId, OshRedeemCode code) {
        if (code == null) {
            throw new ServiceException("核销码不存在");
        }
        if (Integer.valueOf(REDEEM_USED).equals(code.getStatus())) {
            throw new ServiceException("核销码已使用");
        }
        if (Integer.valueOf(REDEEM_CANCELLED).equals(code.getStatus())) {
            throw new ServiceException("核销码已作废");
        }
        if (!Integer.valueOf(REDEEM_PENDING).equals(code.getStatus())) {
            throw new ServiceException("核销码状态异常");
        }
        assertCodeResourceRedeemable(code);

        String orderNo = orderNoGenerator.nextOrderNo(ProductTypeEnum.fromCode(code.getProductType()).getName());
        String paymentNo = orderNoGenerator.nextPaymentNo();
        LocalDateTime now = LocalDateTime.now();
        int updated = redeemCodeMapper.markRedeemed(code.getId(), orderNo, paymentNo, now, userId);
        if (updated <= 0) {
            throw new ServiceException("核销码已使用");
        }
        code.setUserId(userId);
        OshOrder order = createPaidOrder(code, orderNo, now);
        createSuccessPayment(code, order, paymentNo, now);
        grantResource(code, orderNo);
        recordContribution(order);
        code.setOrderNo(orderNo);
        code.setPaymentNo(paymentNo);
        code.setStatus(REDEEM_USED);
        code.setRedeemTime(now);
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelCode(Long id) {
        if (id == null) {
            throw new ServiceException("核销码ID不能为空");
        }
        OshRedeemCode code = redeemCodeMapper.selectById(id);
        if (code == null || !Byte.valueOf((byte) 0).equals(code.getDeleteFlag())) {
            throw new ServiceException("核销码不存在");
        }
        if (!Integer.valueOf(REDEEM_PENDING).equals(code.getStatus())) {
            throw new ServiceException("仅待核销的核销码可以作废");
        }
        int updated = redeemCodeMapper.cancelPending(id, LocalDateTime.now(), currentOperatorId());
        if (updated <= 0) {
            throw new ServiceException("作废失败");
        }
    }

    private void validateCreateRequest(RedeemCodeCreateRequest request) {
        if (request == null) {
            throw new ServiceException("创建参数不能为空");
        }
        if (StringUtils.isBlank(request.getResourceNo())) {
            throw new ServiceException("资源编号不能为空");
        }
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("金额不能小于0");
        }
    }

    private OshRedeemInviteLink ensureInviteLink(OshRedeemCode code, Long operatorId) {
        if (code == null || code.getId() == null) {
            throw new ServiceException("核销码不存在");
        }
        OshRedeemInviteLink existing = redeemInviteLinkMapper.selectOne(new LambdaQueryWrapper<OshRedeemInviteLink>()
                .eq(OshRedeemInviteLink::getRedeemCodeId, code.getId())
                .eq(OshRedeemInviteLink::getDeleteFlag, (byte) 0)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        OshUser operator = operatorId == null || operatorId <= 0 ? null : oshUserMapper.selectById(operatorId);
        OshRedeemInviteLink link = new OshRedeemInviteLink();
        link.setRedeemCodeId(code.getId());
        link.setRedeemCode(code.getCode());
        link.setLinkToken(generateUniqueLinkToken());
        link.setInviteCode(operator == null ? null : StringUtils.trimToNull(operator.getInviteCode()));
        link.setInviterId(operator == null ? null : operator.getId());
        link.setStatus(LINK_ENABLED);
        link.setCreateBy(operatorId == null ? 0L : operatorId);
        link.setUpdateBy(operatorId == null ? 0L : operatorId);
        link.setDeleteFlag((byte) 0);
        redeemInviteLinkMapper.insert(link);
        return link;
    }

    private String normalizeLinkToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new ServiceException("核销邀请链接不能为空");
        }
        return token.trim();
    }

    private OshRedeemChannel resolveChannel(Long channelId) {
        OshRedeemChannel channel = channelId == null ? null : redeemChannelMapper.selectById(channelId);
        if (channel == null) {
            channel = redeemChannelMapper.selectOne(new LambdaQueryWrapper<OshRedeemChannel>()
                    .eq(OshRedeemChannel::getChannelCode, DEFAULT_CHANNEL_CODE)
                    .eq(OshRedeemChannel::getDeleteFlag, (byte) 0)
                    .last("limit 1"));
        }
        if (channel == null) {
            throw new ServiceException("默认核销渠道不存在，请先执行数据库迁移");
        }
        if (!Integer.valueOf(CHANNEL_ENABLED).equals(channel.getStatus())) {
            throw new ServiceException("支付渠道已停用");
        }
        return channel;
    }

    private RedeemResourceVO resolveByPreferredType(String no) {
        if (no.length() < 2) {
            return null;
        }
        String prefix = no.substring(0, 2).toLowerCase(Locale.ROOT);
        switch (prefix) {
            case "co":
                return resolveCourse(no);
            case "bk":
                return resolveBook(no);
            default:
                return null;
        }
    }

    private RedeemResourceVO resolveByFallbackScan(String no) {
        RedeemResourceVO resource = resolveCourse(no);
        if (resource != null) {
            return resource;
        }
        resource = resolveBook(no);
        if (resource != null) {
            return resource;
        }
        return null;
    }

    private RedeemResourceVO resolveCourse(String no) {
        OshCourse course = oshCourseMapper.selectCourseByNo(no);
        if (course == null) {
            return null;
        }
        assertVipLevel(course.getLevel());
        return buildResource(no, ResourceTypeEnum.COURSE.getType(), ProductTypeEnum.COURSE,
                course.getId(), course.getTitle(), course.getPrice(), course.getTPrice());
    }

    private RedeemResourceVO resolveBook(String no) {
        BookDO book = bookMapper.selectReadableByNo(no);
        if (book == null) {
            return null;
        }
        assertVipLevel(book.getLevel());
        return buildResource(no, ResourceTypeEnum.BOOK.getType(), ProductTypeEnum.BOOK,
                book.getId(), book.getTitle(), book.getPrice(), book.getOriginalPrice());
    }

    private void assertCodeResourceRedeemable(OshRedeemCode code) {
        ProductTypeEnum productType = ProductTypeEnum.fromCode(code.getProductType());
        if (productType == null) {
            throw new ServiceException("不支持的资源类型");
        }
        switch (productType) {
            case COURSE:
                OshCourse course = oshCourseMapper.selectCourseById(code.getProductId());
                if (course == null) {
                    throw new ServiceException("资源不存在或不支持核销");
                }
                assertVipLevel(course.getLevel());
                break;
            case BOOK:
                BookDO book = bookMapper.selectReadableById(code.getProductId());
                if (book == null) {
                    throw new ServiceException("资源不存在或不支持核销");
                }
                assertVipLevel(book.getLevel());
                break;
            default:
                throw new ServiceException("当前资源类型暂不支持核销");
        }
    }

    private void assertVipLevel(Integer level) {
        if (!Integer.valueOf(VIP_REDEEMABLE_LEVEL).equals(level)) {
            throw new ServiceException("仅VIP付费资源可以核销");
        }
    }

    private RedeemResourceVO buildResource(String resourceNo, String resourceType, ProductTypeEnum productType,
                                           Long productId, String productName, BigDecimal price,
                                           BigDecimal originalPrice) {
        RedeemResourceVO vo = new RedeemResourceVO();
        vo.setResourceNo(resourceNo);
        vo.setResourceType(resourceType);
        vo.setProductType(productType.getCode());
        vo.setProductId(productId);
        vo.setProductName(productName);
        vo.setPrice(money(defaultMoney(price)));
        vo.setOriginalPrice(money(defaultMoney(originalPrice == null ? price : originalPrice)));
        return vo;
    }

    private OshOrder createPaidOrder(OshRedeemCode code, String orderNo, LocalDateTime now) {
        OshOrder order = new OshOrder();
        order.setUserId(code.getUserId());
        order.setOrderNo(orderNo);
        order.setStatus(OrderStatusEnum.PAID.getCode());
        order.setProductType(code.getProductType());
        order.setProductId(code.getProductId());
        order.setProductName(code.getProductName());
        order.setPurchaseMode(PurchaseModeEnum.NORMAL.getCode());
        order.setOriginalAmount(code.getAmount());
        order.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setPayableAmount(code.getAmount());
        order.setPointsAmount(0L);
        order.setPointsDeductAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setCreatedTime(now);
        order.setPaidTime(now);
        order.setUpdatedTime(now);
        order.setDeleteFlag(0);
        orderMapper.insertOshOrder(order);
        return order;
    }

    private void createSuccessPayment(OshRedeemCode code, OshOrder order, String paymentNo, LocalDateTime now) {
        OshPayment payment = new OshPayment();
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setPaymentNo(paymentNo);
        payment.setChannel(PayChannelEnum.OFFLINE.getCode());
        payment.setAmount(code.getAmount());
        payment.setStatus(PaymentStatusEnum.SUCCESS.getCode());
        payment.setPlatformTradeNo(code.getCode());
        payment.setRequestPayload("{\"redeemCode\":\"" + code.getCode() + "\",\"channel\":\"" + code.getChannelName() + "\"}");
        payment.setResponsePayload("{\"status\":\"success\"}");
        payment.setPaidTime(now);
        payment.setCreatedTime(now);
        payment.setUpdatedTime(now);
        payment.setDeleteFlag(0);
        paymentMapper.insertOshPayment(payment);
    }

    private void grantResource(OshRedeemCode code, String orderNo) {
        ProductTypeEnum productType = ProductTypeEnum.fromCode(code.getProductType());
        if (productType == null) {
            throw new ServiceException("不支持的资源类型");
        }
        switch (productType) {
            case COURSE:
                grantCourse(code, orderNo);
                break;
            case BOOK:
                grantBook(code, orderNo);
                break;
            default:
                throw new ServiceException("当前资源类型暂不支持核销");
        }
    }

    private void grantCourse(OshRedeemCode code, String orderNo) {
        if (courseBuyMapper.countPaidByUserAndCourse(code.getUserId(), code.getProductId()) > 0) {
            throw new ServiceException("用户已购买该课程");
        }
        courseBuyMapper.upsertPendingOrder(code.getUserId(), code.getProductId(), orderNo,
                PayChannelEnum.OFFLINE.getValue(), code.getAmount(), code.getAmount());
        courseBuyMapper.markPaidByOrderNoAndUserId(orderNo, code.getUserId());
    }

    private void grantBook(OshRedeemCode code, String orderNo) {
        UserBookRelation relation = userBookRelationMapper.selectByUserIdAndBookId(code.getUserId(), code.getProductId());
        if (relation != null && Integer.valueOf(1).equals(relation.getPurchased())) {
            throw new ServiceException("用户已购买该电子书");
        }
        userBookRelationMapper.upsertPurchased(code.getUserId(), code.getProductId(), code.getAmount(), orderNo, SYSTEM_OPERATOR);
    }

    private void recordContribution(OshOrder order) {
        if (order == null) {
            return;
        }
        ProductTypeEnum productType = ProductTypeEnum.fromCode(order.getProductType());
        if (productType == null || !ContributionResourceType.contributionTracked(productType.getName())) {
            return;
        }
        try {
            contributionService.recordRevenue(productType.getName(), order.getProductId(), order.getId(),
                    order.getOrderNo(), order.getUserId(), order.getPayableAmount(), order.getPointsAmount(),
                    productType.getName());
        } catch (Exception ignored) {
            // 核销本身已经成功，贡献统计失败不回滚用户权益。
        }
    }

    private RedeemCodePageQuery normalizePage(RedeemCodePageQuery query) {
        RedeemCodePageQuery normalized = query == null ? new RedeemCodePageQuery() : query;
        if (normalized.getPageNum() == null || normalized.getPageNum() < 1) {
            normalized.setPageNum(1);
        }
        if (normalized.getPageSize() == null || normalized.getPageSize() < 1) {
            normalized.setPageSize(20);
        }
        if (normalized.getPageSize() > 200) {
            normalized.setPageSize(200);
        }
        return normalized;
    }

    private RedeemResourceSearchQuery normalizeResourceSearch(RedeemResourceSearchQuery query) {
        RedeemResourceSearchQuery normalized = query == null ? new RedeemResourceSearchQuery() : query;
        if (normalized.getPageNum() == null || normalized.getPageNum() < 1) {
            normalized.setPageNum(1);
        }
        if (normalized.getPageSize() == null || normalized.getPageSize() < 1) {
            normalized.setPageSize(DEFAULT_RESOURCE_PAGE_SIZE);
        }
        if (normalized.getPageSize() > MAX_RESOURCE_PAGE_SIZE) {
            normalized.setPageSize(MAX_RESOURCE_PAGE_SIZE);
        }
        if (StringUtils.isBlank(normalized.getResourceType())) {
            throw new ServiceException("资源类型不能为空");
        }
        String resourceType = normalized.getResourceType().trim();
        if (!ResourceTypeEnum.COURSE.getType().equals(resourceType)
                && !ResourceTypeEnum.BOOK.getType().equals(resourceType)) {
            throw new ServiceException("当前资源类型暂不支持核销");
        }
        normalized.setResourceType(resourceType);
        if (StringUtils.isNotBlank(normalized.getKeyword())) {
            normalized.setKeyword(normalized.getKeyword().trim());
        }
        return normalized;
    }

    private void normalizeEmailQuery(RedeemCodePageQuery query) {
        if (query == null || StringUtils.isBlank(query.getUserEmail())) {
            return;
        }
        query.setUserEmail(query.getUserEmail().trim());
    }

    private String normalizeResourceNo(String resourceNo) {
        if (StringUtils.isBlank(resourceNo)) {
            throw new ServiceException("资源编号不能为空");
        }
        return resourceNo.trim();
    }

    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal money(BigDecimal amount) {
        return defaultMoney(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private Long currentOperatorId() {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        return userId == null ? 0L : userId;
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 5; i++) {
            String code = "HX" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT)
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
            Long count = redeemCodeMapper.selectCount(new LambdaQueryWrapper<OshRedeemCode>()
                    .eq(OshRedeemCode::getCode, code));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new ServiceException("核销码生成失败，请重试");
    }

    private String generateUniqueLinkToken() {
        for (int i = 0; i < 10; i++) {
            String token = UUID.randomUUID().toString().replace("-", "");
            Long count = redeemInviteLinkMapper.selectCount(new LambdaQueryWrapper<OshRedeemInviteLink>()
                    .eq(OshRedeemInviteLink::getLinkToken, token));
            if (count == null || count == 0) {
                return token;
            }
        }
        throw new ServiceException("核销邀请链接生成失败，请重试");
    }

    private String toChannelCode(String channelName) {
        String code = channelName.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9_\\-]", "");
        if (StringUtils.isBlank(code)) {
            code = "channel_" + Long.toString(System.currentTimeMillis(), 36);
        }
        return code.toLowerCase(Locale.ROOT);
    }
}
