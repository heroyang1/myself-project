package com.backstage.system.service.bundle.impl;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.UserBookRelation;
import com.backstage.system.domain.bundle.OshBundleItem;
import com.backstage.system.domain.bundle.OshProductBundle;
import com.backstage.system.domain.bundle.dto.BundleItemRequest;
import com.backstage.system.domain.bundle.dto.BundlePageQuery;
import com.backstage.system.domain.bundle.dto.BundleResourceSearchQuery;
import com.backstage.system.domain.bundle.dto.BundleSaveRequest;
import com.backstage.system.domain.bundle.dto.BundleStatusRequest;
import com.backstage.system.domain.bundle.vo.BundleDetailVO;
import com.backstage.system.domain.bundle.vo.BundleItemVO;
import com.backstage.system.domain.bundle.vo.BundleListVO;
import com.backstage.system.domain.bundle.vo.BundleResourceVO;
import com.backstage.system.domain.order.OrderStatusResult;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.domain.order.enums.PayChannelEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.enums.CourseResourceEnum;
import com.backstage.system.mapper.book.UserBookRelationMapper;
import com.backstage.system.mapper.course.CourseBuyMapper;
import com.backstage.system.mapper.course.OshCourseMapper;
import com.backstage.system.mapper.bundle.OshBundleItemMapper;
import com.backstage.system.mapper.bundle.OshBundleResourceMapper;
import com.backstage.system.mapper.bundle.OshProductBundleMapper;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.bundle.ProductBundleService;
import com.backstage.system.service.impl.course.CourseBundlePayCheckoutService;
import com.backstage.system.service.order.OrderService;
import com.backstage.system.utils.OssUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductBundleServiceImpl implements ProductBundleService {
    public static final String SALE_MODE_PRIMARY_GIFT = "PRIMARY_GIFT";
    public static final String SALE_MODE_FLAT_BUNDLE = "FLAT_BUNDLE";

    @Resource
    private OshProductBundleMapper bundleMapper;
    @Resource
    private OshBundleItemMapper bundleItemMapper;
    @Resource
    private OshBundleResourceMapper bundleResourceMapper;
    @Resource
    private OssUtil ossUtil;
    @Resource
    private OssService ossService;
    @Resource
    private CourseBundlePayCheckoutService courseBundlePayCheckoutService;
    @Resource
    private OrderService orderService;
    @Resource
    private OshOrderMapper orderMapper;
    @Resource
    private OshPaymentMapper paymentMapper;
    @Resource
    private CourseBuyMapper courseBuyMapper;
    @Resource
    private OshCourseMapper oshCourseMapper;
    @Resource
    private UserBookRelationMapper userBookRelationMapper;

    private static final int MAX_BUNDLE_RESOURCE_PAGE_SIZE = 200;

    @Override
    public PageInfo<BundleListVO> pageAdminBundles(BundlePageQuery query) {
        BundlePageQuery normalized = query == null ? new BundlePageQuery() : query;
        int pageNum = normalized.getPageNum() == null || normalized.getPageNum() < 1 ? 1 : normalized.getPageNum();
        int pageSize = normalized.getPageSize() == null || normalized.getPageSize() < 1 ? 10 : normalized.getPageSize();

        LambdaQueryWrapper<OshProductBundle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(normalized.getKeyword())) {
            wrapper.and(w -> w.like(OshProductBundle::getName, normalized.getKeyword().trim())
                    .or().like(OshProductBundle::getBundleNo, normalized.getKeyword().trim()));
        }
        if (StringUtils.isNotBlank(normalized.getSaleMode())) {
            wrapper.eq(OshProductBundle::getSaleMode, normalized.getSaleMode().trim());
        }
        if (normalized.getStatus() != null) {
            wrapper.eq(OshProductBundle::getStatus, normalized.getStatus());
        }
        wrapper.orderByDesc(OshProductBundle::getSortOrder).orderByDesc(OshProductBundle::getUpdateTime);

        PageHelper.startPage(pageNum, pageSize);
        List<OshProductBundle> bundles = bundleMapper.selectList(wrapper);
        PageInfo<OshProductBundle> raw = new PageInfo<>(bundles);

        List<BundleListVO> rows = new ArrayList<>();
        for (OshProductBundle bundle : bundles) {
            BundleListVO vo = new BundleListVO();
            BeanUtils.copyProperties(bundle, vo);
            vo.setItemCount(countItems(bundle.getId()));
            rows.add(vo);
        }

        PageInfo<BundleListVO> pageInfo = new PageInfo<>();
        pageInfo.setList(rows);
        pageInfo.setTotal(raw.getTotal());
        pageInfo.setPageNum(raw.getPageNum());
        pageInfo.setPageSize(raw.getPageSize());
        pageInfo.setPages(raw.getPages());
        return pageInfo;
    }

    @Override
    public BundleDetailVO getDetail(Long id) {
        OshProductBundle bundle = requireBundle(id);
        return toDetailVO(bundle, listItems(bundle.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BundleDetailVO save(BundleSaveRequest request) {
        if (request == null || StringUtils.isBlank(request.getName())) {
            throw new ServiceException("套餐名称不能为空");
        }
        if (StringUtils.isBlank(request.getSaleMode())) {
            throw new ServiceException("请选择售卖模式");
        }
        if (request.getBundlePrice() == null || request.getBundlePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("套餐售价不合法");
        }
        List<BundleItemRequest> items = request.getItems() == null ? new ArrayList<>() : request.getItems();
        validateItems(request.getSaleMode(), items);

        OshProductBundle bundle;
        if (request.getId() == null) {
            bundle = new OshProductBundle();
            bundle.setBundleNo(generateBundleNo());
            bundle.setStatus(0);
            bundle.setSalesCount(0);
        } else {
            bundle = requireBundle(request.getId());
        }

        bundle.setName(request.getName().trim());
        bundle.setCoverUrl(StringUtils.trimToNull(normalizeCoverStoragePath(request.getCoverUrl())));
        bundle.setDescription(StringUtils.trimToNull(request.getDescription()));
        bundle.setSaleMode(request.getSaleMode().trim());
        bundle.setBundlePrice(request.getBundlePrice());
        bundle.setOriginalPrice(resolveOriginalPrice(request.getOriginalPrice(), items));
        bundle.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());

        if (request.getId() == null) {
            bundleMapper.insert(bundle);
        } else {
            bundleMapper.updateById(bundle);
            softDeleteItems(bundle.getId());
        }

        persistItems(bundle, request.getSaleMode(), items);
        return getDetail(bundle.getId());
    }

    @Override
    public void updateStatus(BundleStatusRequest request) {
        if (request == null || request.getId() == null || request.getStatus() == null) {
            throw new ServiceException("参数不完整");
        }
        if (request.getStatus() < 0 || request.getStatus() > 2) {
            throw new ServiceException("状态不合法");
        }
        OshProductBundle bundle = requireBundle(request.getId());
        if (request.getStatus() == 1 && countItems(bundle.getId()) < 1) {
            throw new ServiceException("套餐内至少包含一个商品才能上架");
        }
        bundle.setStatus(request.getStatus());
        bundleMapper.updateById(bundle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        OshProductBundle bundle = requireBundle(id);
        if (bundle.getStatus() != null && bundle.getStatus() == 1) {
            throw new ServiceException("请先下架后再删除");
        }
        bundleMapper.deleteById(id);
        softDeleteItems(id);
    }

    private void validateItems(String saleMode, List<BundleItemRequest> items) {
        if (items.isEmpty()) {
            throw new ServiceException("请至少添加一个商品");
        }
        Set<String> unique = new HashSet<>();
        int primaryCount = 0;
        int secondaryCount = 0;
        for (BundleItemRequest item : items) {
            if (item == null || StringUtils.isBlank(item.getResourceType()) || item.getResourceId() == null) {
                throw new ServiceException("套餐商品信息不完整");
            }
            String key = item.getResourceType().trim() + ":" + item.getResourceId();
            if (!unique.add(key)) {
                throw new ServiceException("同一套餐内不能重复添加相同商品");
            }
            String role = normalizeRole(saleMode, item.getItemRole());
            if ("primary".equals(role)) {
                primaryCount++;
            }
            if ("secondary".equals(role)) {
                secondaryCount++;
            }
        }
        if (SALE_MODE_PRIMARY_GIFT.equals(saleMode)) {
            if (primaryCount != 1) {
                throw new ServiceException("买主送副模式需要且只能有一个主商品");
            }
            if (secondaryCount < 1) {
                throw new ServiceException("买主送副模式至少需要一个副商品/赠品");
            }
        } else if (SALE_MODE_FLAT_BUNDLE.equals(saleMode)) {
            if (items.size() < 2) {
                throw new ServiceException("打包优惠模式至少需要两个商品");
            }
        } else {
            throw new ServiceException("未知的售卖模式");
        }
    }

    private void persistItems(OshProductBundle bundle, String saleMode, List<BundleItemRequest> items) {
        int index = 0;
        for (BundleItemRequest item : items) {
            OshBundleItem row = new OshBundleItem();
            row.setBundleId(bundle.getId());
            row.setResourceType(item.getResourceType().trim());
            row.setResourceId(item.getResourceId());
            row.setResourceName(StringUtils.trimToNull(item.getResourceName()));
            row.setUnitPrice(item.getUnitPrice());
            String role = normalizeRole(saleMode, item.getItemRole());
            row.setItemRole(role);
            row.setIsGift("secondary".equals(role) ? 1 : 0);
            row.setSortOrder(item.getSortOrder() == null ? index : item.getSortOrder());
            bundleItemMapper.insert(row);
            index++;
        }
    }

    private String normalizeRole(String saleMode, String itemRole) {
        if (SALE_MODE_FLAT_BUNDLE.equals(saleMode)) {
            return "member";
        }
        if (StringUtils.isBlank(itemRole)) {
            throw new ServiceException("买主送副模式请标记主/副商品");
        }
        String role = itemRole.trim().toLowerCase();
        if (!"primary".equals(role) && !"secondary".equals(role)) {
            throw new ServiceException("商品角色只能是 primary 或 secondary");
        }
        return role;
    }

    @Override
    public PageInfo<BundleResourceVO> searchResources(BundleResourceSearchQuery query) {
        BundleResourceSearchQuery normalized = normalizeResourceSearch(query);
        PageHelper.startPage(normalized.getPageNum(), normalized.getPageSize());
        List<BundleResourceVO> rows = bundleResourceMapper.searchResources(
                normalized.getResourceType(),
                normalized.getKeyword());
        return new PageInfo<>(rows);
    }

    @Override
    public List<BundleDetailVO> recommendByResource(String resourceType, Long resourceId) {
        String normalizedType = normalizeResourceType(resourceType);
        if (resourceId == null || resourceId < 1) {
            throw new ServiceException("资源ID不能为空");
        }

        List<OshBundleItem> matchedItems = bundleItemMapper.selectList(new LambdaQueryWrapper<OshBundleItem>()
                .eq(OshBundleItem::getResourceType, normalizedType)
                .eq(OshBundleItem::getResourceId, resourceId));
        if (matchedItems.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> bundleIds = matchedItems.stream()
                .map(OshBundleItem::getBundleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<OshProductBundle> bundles = bundleMapper.selectList(new LambdaQueryWrapper<OshProductBundle>()
                .in(OshProductBundle::getId, bundleIds)
                .eq(OshProductBundle::getStatus, 1)
                .orderByDesc(OshProductBundle::getSortOrder)
                .orderByDesc(OshProductBundle::getUpdateTime));

        return bundles.stream()
                .limit(6)
                .map(bundle -> toRecommendDetailVO(bundle, listItems(bundle.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public OrderCheckoutRespVO createBundlePay(Long bundleId, String payType, String clientIp, Long userId) {
        if (bundleId == null || bundleId < 1) {
            throw new ServiceException("套餐ID不能为空");
        }
        if (userId == null || userId < 1) {
            throw new ServiceException("请先登录后再支付");
        }
        OshProductBundle bundle = requireBundle(bundleId);
        if (!Integer.valueOf(1).equals(bundle.getStatus())) {
            throw new ServiceException("套餐未上架，暂不可购买");
        }
        if (countItems(bundleId) < 1) {
            throw new ServiceException("套餐内容为空，暂不可购买");
        }
        ensureBundleItemsPayable(bundleId);
        BigDecimal price = bundle.getBundlePrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("套餐价格无效");
        }

        return courseBundlePayCheckoutService.checkout(
                userId,
                ProductTypeEnum.BUNDLE,
                bundle.getId(),
                bundle.getName() == null ? ("套餐#" + bundle.getId()) : bundle.getName(),
                bundle.getOriginalPrice() == null ? price : bundle.getOriginalPrice(),
                price,
                normalizePayType(payType),
                normalizeClientIp(clientIp)
        );
    }

    private void ensureBundleItemsPayable(Long bundleId) {
        List<OshBundleItem> items = listItems(bundleId);
        for (OshBundleItem item : items) {
            if (!"course".equals(item.getResourceType())) {
                continue;
            }
            OshCourse course = oshCourseMapper.selectCourseById(item.getResourceId());
            if (course == null) {
                throw new ServiceException("套餐包含不存在的课程，暂不可购买");
            }
            if (!isCoursePayableInBundle(course)) {
                throw new ServiceException("套餐包含小班专属/内部专属课程，暂不支持购买");
            }
        }
    }

    private boolean isCoursePayableInBundle(OshCourse course) {
        CourseResourceEnum resourceEnum = CourseResourceEnum.fromCode(course.getResourceType());
        if (resourceEnum == CourseResourceEnum.VIP
                || resourceEnum == CourseResourceEnum.CASH_ONLY
                || resourceEnum == CourseResourceEnum.CASH_POINT) {
            return true;
        }
        return resourceEnum == null && Integer.valueOf(2).equals(course.getLevel());
    }

    @Override
    public Map<String, Object> getBundlePayStatus(String orderNo, Long userId) {
        if (StringUtils.isBlank(orderNo) || userId == null || userId < 1) {
            throw new ServiceException("参数不完整");
        }
        OrderStatusResult status = orderService.getOrderStatusForUser(orderNo, userId);
        if (status != null && status.isPayStatus()) {
            grantBundleAccess(orderNo);
        }
        OshOrder order = orderMapper.selectByOrderNo(orderNo);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paid", status != null && status.isPayStatus());
        data.put("orderNo", orderNo);
        data.put("bundleId", order == null ? null : order.getProductId());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantBundleAccess(String orderNo) {
        OshOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ServiceException("套餐订单不存在: " + orderNo);
        }
        if (!Integer.valueOf(ProductTypeEnum.BUNDLE.getCode()).equals(order.getProductType())) {
            throw new ServiceException("订单商品类型不是套餐: " + orderNo);
        }
        List<OshBundleItem> items = listItems(order.getProductId());
        if (items.isEmpty()) {
            throw new ServiceException("套餐内容为空: " + order.getProductId());
        }
        for (OshBundleItem item : items) {
            if ("course".equals(item.getResourceType())) {
                grantCourseItem(order, item);
            } else if ("book".equals(item.getResourceType())) {
                grantBookItem(order, item);
            }
        }
    }

    private void grantCourseItem(OshOrder order, OshBundleItem item) {
        if (courseBuyMapper.countPaidByUserAndCourse(order.getUserId(), item.getResourceId()) > 0) {
            return;
        }
        BigDecimal itemPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
        courseBuyMapper.upsertPendingOrder(
                order.getUserId(),
                item.getResourceId(),
                order.getOrderNo(),
                resolvePayMethod(order),
                itemPrice,
                itemPrice
        );
        courseBuyMapper.markPaidByOrderNoAndUserId(order.getOrderNo(), order.getUserId());
    }

    private void grantBookItem(OshOrder order, OshBundleItem item) {
        UserBookRelation relation = userBookRelationMapper.selectByUserIdAndBookId(order.getUserId(), item.getResourceId());
        if (relation != null && Integer.valueOf(1).equals(relation.getPurchased())) {
            return;
        }
        BigDecimal itemPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
        userBookRelationMapper.upsertPurchased(order.getUserId(), item.getResourceId(), itemPrice, order.getOrderNo(), "system");
    }

    private String normalizePayType(String payType) {
        String channel = StringUtils.defaultIfBlank(payType, PayChannelEnum.WXPAY.getValue()).trim().toLowerCase();
        if (PayChannelEnum.WXPAY.getValue().equals(channel) || PayChannelEnum.ALIPAY.getValue().equals(channel)) {
            return channel;
        }
        throw new ServiceException("不支持的支付方式: " + payType);
    }

    private String normalizeClientIp(String clientIp) {
        if (StringUtils.isBlank(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp.trim())) {
            return "127.0.0.1";
        }
        String ip = clientIp.trim();
        int commaIndex = ip.indexOf(',');
        if (commaIndex >= 0) {
            ip = ip.substring(0, commaIndex).trim();
        }
        return StringUtils.defaultIfBlank(ip, "127.0.0.1");
    }

    private String resolvePayMethod(OshOrder order) {
        OshPayment payment = paymentMapper.selectByOrderNo(order.getOrderNo());
        if (payment != null && payment.getChannel() != null) {
            PayChannelEnum channel = PayChannelEnum.fromCode(payment.getChannel());
            if (channel != null) {
                return channel.getValue();
            }
        }
        return PayChannelEnum.WXPAY.getValue();
    }

    private BundleResourceSearchQuery normalizeResourceSearch(BundleResourceSearchQuery query) {
        BundleResourceSearchQuery normalized = query == null ? new BundleResourceSearchQuery() : query;
        if (normalized.getPageNum() == null || normalized.getPageNum() < 1) {
            normalized.setPageNum(1);
        }
        if (normalized.getPageSize() == null || normalized.getPageSize() < 1) {
            normalized.setPageSize(50);
        }
        if (normalized.getPageSize() > MAX_BUNDLE_RESOURCE_PAGE_SIZE) {
            normalized.setPageSize(MAX_BUNDLE_RESOURCE_PAGE_SIZE);
        }
        if (StringUtils.isBlank(normalized.getResourceType())) {
            throw new ServiceException("资源类型不能为空");
        }
        String resourceType = normalized.getResourceType().trim();
        if (!"course".equals(resourceType)
                && !"book".equals(resourceType)
                && !"tool".equals(resourceType)
                && !"exam".equals(resourceType)) {
            throw new ServiceException("不支持的资源类型");
        }
        normalized.setResourceType(resourceType);
        if (StringUtils.isNotBlank(normalized.getKeyword())) {
            normalized.setKeyword(normalized.getKeyword().trim());
        } else {
            normalized.setKeyword(null);
        }
        return normalized;
    }

    private String normalizeResourceType(String resourceType) {
        if (StringUtils.isBlank(resourceType)) {
            throw new ServiceException("资源类型不能为空");
        }
        String normalizedType = resourceType.trim();
        if (!"course".equals(normalizedType)
                && !"book".equals(normalizedType)
                && !"tool".equals(normalizedType)
                && !"exam".equals(normalizedType)) {
            throw new ServiceException("不支持的资源类型");
        }
        return normalizedType;
    }

    private String resolveCoverUrl(String cover) {
        String storagePath = normalizeCoverStoragePath(cover);
        if (StringUtils.isBlank(storagePath)) {
            return null;
        }
        try {
            return ossService.getLimitedUrl(storagePath, 1440);
        } catch (Exception ignored) {
            return ossUtil.getFullFilePath(storagePath);
        }
    }

    private String normalizeCoverStoragePath(String cover) {
        if (StringUtils.isBlank(cover)) {
            return null;
        }
        String normalized = cover.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            try {
                normalized = URLDecoder.decode(URI.create(normalized).getPath(), StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {
                return null;
            }
        }
        normalized = normalized.replaceFirst("^/+", "");
        String bucketName = ossUtil.getOssProperties().getBucketName();
        if (StringUtils.isNotBlank(bucketName) && normalized.startsWith(bucketName + "/")) {
            normalized = normalized.substring(bucketName.length() + 1);
        }
        return StringUtils.trimToNull(normalized);
    }

    private BigDecimal resolveOriginalPrice(BigDecimal originalPrice, List<BundleItemRequest> items) {
        if (originalPrice != null) {
            return originalPrice;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BundleItemRequest item : items) {
            if (item.getUnitPrice() != null) {
                sum = sum.add(item.getUnitPrice());
            }
        }
        return sum.compareTo(BigDecimal.ZERO) > 0 ? sum : null;
    }

    private int countItems(Long bundleId) {
        return bundleItemMapper.selectCount(new LambdaQueryWrapper<OshBundleItem>()
                .eq(OshBundleItem::getBundleId, bundleId)).intValue();
    }

    private List<OshBundleItem> listItems(Long bundleId) {
        return bundleItemMapper.selectList(new LambdaQueryWrapper<OshBundleItem>()
                .eq(OshBundleItem::getBundleId, bundleId)
                .orderByAsc(OshBundleItem::getSortOrder)
                .orderByAsc(OshBundleItem::getId));
    }

    private void softDeleteItems(Long bundleId) {
        bundleItemMapper.delete(new LambdaQueryWrapper<OshBundleItem>().eq(OshBundleItem::getBundleId, bundleId));
    }

    private OshProductBundle requireBundle(Long id) {
        OshProductBundle bundle = bundleMapper.selectById(id);
        if (bundle == null) {
            throw new ServiceException("套餐不存在");
        }
        return bundle;
    }

    private BundleDetailVO toDetailVO(OshProductBundle bundle, List<OshBundleItem> items) {
        BundleDetailVO vo = new BundleDetailVO();
        BeanUtils.copyProperties(bundle, vo);
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    private BundleDetailVO toRecommendDetailVO(OshProductBundle bundle, List<OshBundleItem> items) {
        BundleDetailVO vo = toDetailVO(bundle, items);
        vo.setCoverUrl(resolveCoverUrl(bundle.getCoverUrl()));
        return vo;
    }

    private BundleItemVO toItemVO(OshBundleItem item) {
        BundleItemVO vo = new BundleItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private String generateBundleNo() {
        return "BND" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (int) (Math.random() * 900 + 100);
    }
}
