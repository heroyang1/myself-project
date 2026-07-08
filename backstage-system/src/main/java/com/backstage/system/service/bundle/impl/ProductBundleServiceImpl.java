package com.backstage.system.service.bundle.impl;

import com.backstage.common.exception.ServiceException;
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
import com.backstage.system.mapper.bundle.OshBundleItemMapper;
import com.backstage.system.mapper.bundle.OshBundleResourceMapper;
import com.backstage.system.mapper.bundle.OshProductBundleMapper;
import com.backstage.system.service.bundle.ProductBundleService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        bundle.setCoverUrl(StringUtils.trimToNull(request.getCoverUrl()));
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

    private String resolveCoverUrl(String cover) {
        if (StringUtils.isBlank(cover)) {
            return null;
        }
        if (cover.startsWith("http://") || cover.startsWith("https://")) {
            return cover;
        }
        return ossUtil.getFullFilePath(cover);
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
