package com.backstage.system.service.impl.homepage;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.homepage.es.HomepageMemberPlanEsDocument;
import com.backstage.system.domain.member.OshMemberBenefit;
import com.backstage.system.domain.member.OshMemberPlan;
import com.backstage.system.domain.member.vo.MemberPlanPriceTierVO;
import com.backstage.system.mapper.homepage.HomepageMemberPlanEsMapper;
import com.backstage.system.mapper.member.OshMemberBenefitMapper;
import com.backstage.system.mapper.member.OshMemberPlanMapper;
import com.backstage.system.service.homepage.IOshHomePageMemberPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OshHomePageMemberPlanServiceImpl implements IOshHomePageMemberPlanService, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(OshHomePageMemberPlanServiceImpl.class);
    private static final int PLAN_STATUS_ENABLED = 1;
    private static final int MAX_BENEFIT_SIZE = 3;
    private static final BigDecimal DEFAULT_GROWTH_COEFFICIENT = new BigDecimal("0.90");
    private static final BigDecimal DEFAULT_CAP_RATIO = new BigDecimal("0.95");
    private static final BigDecimal MONTH_GROWTH_COEFFICIENT = new BigDecimal("0.78");
    private static final BigDecimal SMALL_CLASS_GROWTH_COEFFICIENT = new BigDecimal("0.95");
    private static final String MEMBER_TYPE_SMALL_CLASS = "small_class";

    @Resource
    private HomepageMemberPlanEsMapper homepageMemberPlanEsMapper;

    @Resource
    private OshMemberPlanMapper memberPlanMapper;

    @Resource
    private OshMemberBenefitMapper memberBenefitMapper;

    @Override
    public List<OshMemberPlan> getPlans() {
        try {
            List<HomepageMemberPlanEsDocument> documents = homepageMemberPlanEsMapper.searchHomepagePlans();
            List<OshMemberPlan> plans = new ArrayList<>(documents.size());
            for (HomepageMemberPlanEsDocument document : documents) {
                plans.add(toPlan(document));
            }
            attachHomepagePriceTiers(plans);
            return plans;
        } catch (IOException | IllegalStateException exception) {
            throw buildServiceException("查询首页会员套餐失败", exception);
        }
    }

    @Override
    public int syncAllToEs() {
        List<OshMemberPlan> plans = queryAllEnabledPlans();
        if (plans.isEmpty()) {
            try {
                return homepageMemberPlanEsMapper.deleteAll();
            } catch (IOException | IllegalStateException exception) {
                throw buildServiceException("清空首页会员套餐ES数据失败", exception);
            }
        }

        attachEnabledBenefits(plans);
        attachHomepagePriceTiers(plans);
        List<HomepageMemberPlanEsDocument> documents = new ArrayList<>(plans.size());
        for (OshMemberPlan plan : plans) {
            documents.add(toDocument(plan));
        }

        try {
            homepageMemberPlanEsMapper.deleteAll();
            return homepageMemberPlanEsMapper.bulkUpsert(documents);
        } catch (IOException | IllegalStateException exception) {
            throw buildServiceException("全量同步首页会员套餐到ES失败", exception);
        }
    }

    @Override
    public void upsertPlanById(Long planId) {
        if (planId == null) {
            return;
        }

        OshMemberPlan plan = memberPlanMapper.selectById(planId);
        if (plan == null || shouldDeleteFromEs(plan)) {
            deletePlanQuietly(planId);
            return;
        }

        attachEnabledBenefits(Collections.singletonList(plan));
        try {
            homepageMemberPlanEsMapper.upsert(toDocument(plan));
        } catch (IOException | IllegalStateException exception) {
            throw buildServiceException("同步首页会员套餐到ES失败，planId=" + planId, exception);
        }
    }

    @Override
    public void deletePlanById(Long planId) {
        if (planId == null) {
            return;
        }
        try {
            homepageMemberPlanEsMapper.deleteById(planId);
        } catch (IOException exception) {
            throw buildServiceException("删除首页会员套餐ES文档失败，planId=" + planId, exception);
        }
    }

    @Override
    public boolean indexExists() {
        try {
            return homepageMemberPlanEsMapper.indexExists();
        } catch (IOException exception) {
            throw buildServiceException("检查首页会员套餐ES索引失败", exception);
        }
    }

    @Override
    public void recreateIndex(String indexDefinitionJson) {
        try {
            homepageMemberPlanEsMapper.recreateIndex(indexDefinitionJson);
        } catch (IOException exception) {
            throw buildServiceException("重建首页会员套餐ES索引失败", exception);
        }
    }

    @Override
    public void afterPropertiesSet() {
        try {
            if (!homepageMemberPlanEsMapper.indexExists()) {
                log.info("homepage member plan es index not found, recreate and sync automatically");
                homepageMemberPlanEsMapper.recreateIndex(HomepageMemberPlanEsMapper.loadMappingJson());
                syncAllToEs();
            }
        } catch (IOException | ServiceException exception) {
            log.warn("initialize homepage member plan es index failed", exception);
        }
    }

    private List<OshMemberPlan> queryAllEnabledPlans() {
        LambdaQueryWrapper<OshMemberPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshMemberPlan::getDeleteFlag, (byte) 0)
                .eq(OshMemberPlan::getStatus, PLAN_STATUS_ENABLED)
                .orderByAsc(OshMemberPlan::getSort)
                .orderByAsc(OshMemberPlan::getId);
        return memberPlanMapper.selectList(wrapper);
    }

    private void attachEnabledBenefits(List<OshMemberPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }

        List<Long> planIds = plans.stream()
                .map(OshMemberPlan::getId)
                .collect(Collectors.toList());
        List<OshMemberBenefit> benefits = memberBenefitMapper.selectEnabledByPlanIds(planIds);
        Map<Long, List<OshMemberBenefit>> benefitMap = benefits == null
                ? Collections.emptyMap()
                : benefits.stream().collect(Collectors.groupingBy(OshMemberBenefit::getPlanId));

        for (OshMemberPlan plan : plans) {
            List<OshMemberBenefit> benefitList =
                    new ArrayList<>(benefitMap.getOrDefault(plan.getId(), Collections.emptyList()));
            if (benefitList.size() > MAX_BENEFIT_SIZE) {
                benefitList = new ArrayList<>(benefitList.subList(0, MAX_BENEFIT_SIZE));
            }
            plan.setBenefits(benefitList);
        }
    }

    private HomepageMemberPlanEsDocument toDocument(OshMemberPlan plan) {
        HomepageMemberPlanEsDocument document = new HomepageMemberPlanEsDocument();
        document.setId(plan.getId());
        document.setPlanCode(plan.getPlanCode());
        document.setPlanName(plan.getPlanName());
        document.setMemberType(plan.getMemberType());
        document.setPeriodType(plan.getPeriodType());
        document.setDurationMonths(plan.getDurationMonths());
        document.setPrice(plan.getPrice());
        document.setOriginalPrice(plan.getOriginalPrice());
        document.setDescription(plan.getDescription());
        document.setMinPurchaseQuantity(plan.getMinPurchaseQuantity());
        document.setMaxPurchaseQuantity(plan.getMaxPurchaseQuantity());
        document.setGrowthCoefficient(plan.getGrowthCoefficient());
        document.setCapPlanCode(plan.getCapPlanCode());
        document.setCapRatio(plan.getCapRatio());
        document.setSort(plan.getSort());
        document.setStatus(plan.getStatus());
        document.setDeleteFlag(plan.getDeleteFlag());
        document.setCreateTime(plan.getCreateTime());
        document.setUpdateTime(plan.getUpdateTime());
        document.setBenefits(toBenefitDocuments(plan.getBenefits()));
        document.setPriceTiers(plan.getPriceTiers());
        document.setEffectiveMaxPurchaseQuantity(plan.getEffectiveMaxPurchaseQuantity());
        return document;
    }

    private List<HomepageMemberPlanEsDocument.BenefitDocument> toBenefitDocuments(List<OshMemberBenefit> benefits) {
        if (benefits == null || benefits.isEmpty()) {
            return Collections.emptyList();
        }

        List<HomepageMemberPlanEsDocument.BenefitDocument> documents = new ArrayList<>(benefits.size());
        for (OshMemberBenefit benefit : benefits) {
            HomepageMemberPlanEsDocument.BenefitDocument document =
                    new HomepageMemberPlanEsDocument.BenefitDocument();
            document.setId(benefit.getId());
            document.setPlanId(benefit.getPlanId());
            document.setBenefitTitle(benefit.getBenefitTitle());
            document.setBenefitDescription(benefit.getBenefitDescription());
            document.setIcon(benefit.getIcon());
            document.setSort(benefit.getSort());
            document.setStatus(benefit.getStatus());
            document.setDeleteFlag(benefit.getDeleteFlag());
            document.setCreateTime(benefit.getCreateTime());
            document.setUpdateTime(benefit.getUpdateTime());
            documents.add(document);
        }
        return documents;
    }

    private OshMemberPlan toPlan(HomepageMemberPlanEsDocument document) {
        OshMemberPlan plan = new OshMemberPlan();
        plan.setId(document.getId());
        plan.setPlanCode(document.getPlanCode());
        plan.setPlanName(document.getPlanName());
        plan.setMemberType(document.getMemberType());
        plan.setPeriodType(document.getPeriodType());
        plan.setDurationMonths(document.getDurationMonths());
        plan.setPrice(document.getPrice());
        plan.setOriginalPrice(document.getOriginalPrice());
        plan.setDescription(document.getDescription());
        plan.setMinPurchaseQuantity(document.getMinPurchaseQuantity());
        plan.setMaxPurchaseQuantity(document.getMaxPurchaseQuantity());
        plan.setGrowthCoefficient(document.getGrowthCoefficient());
        plan.setCapPlanCode(document.getCapPlanCode());
        plan.setCapRatio(document.getCapRatio());
        plan.setSort(document.getSort());
        plan.setStatus(document.getStatus());
        plan.setDeleteFlag(document.getDeleteFlag());
        plan.setCreateTime(document.getCreateTime());
        plan.setUpdateTime(document.getUpdateTime());
        plan.setBenefits(toBenefits(document.getBenefits()));
        plan.setPriceTiers(document.getPriceTiers());
        plan.setEffectiveMaxPurchaseQuantity(document.getEffectiveMaxPurchaseQuantity());
        return plan;
    }

    private List<OshMemberBenefit> toBenefits(List<HomepageMemberPlanEsDocument.BenefitDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        List<OshMemberBenefit> benefits = new ArrayList<>(documents.size());
        for (HomepageMemberPlanEsDocument.BenefitDocument document : documents) {
            OshMemberBenefit benefit = new OshMemberBenefit();
            benefit.setId(document.getId());
            benefit.setPlanId(document.getPlanId());
            benefit.setBenefitTitle(document.getBenefitTitle());
            benefit.setBenefitDescription(document.getBenefitDescription());
            benefit.setIcon(document.getIcon());
            benefit.setSort(document.getSort());
            benefit.setStatus(document.getStatus());
            benefit.setDeleteFlag(document.getDeleteFlag());
            benefit.setCreateTime(document.getCreateTime());
            benefit.setUpdateTime(document.getUpdateTime());
            benefits.add(benefit);
        }
        return benefits;
    }

    private void attachHomepagePriceTiers(List<OshMemberPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }
        Map<String, OshMemberPlan> planMap = plans.stream()
                .filter(plan -> plan.getPlanCode() != null)
                .collect(Collectors.toMap(OshMemberPlan::getPlanCode, plan -> plan, (first, second) -> first));
        for (OshMemberPlan plan : plans) {
            if (plan.getMinPurchaseQuantity() == null || plan.getMinPurchaseQuantity() < 1) {
                plan.setMinPurchaseQuantity(1);
            }
            if (plan.getMaxPurchaseQuantity() == null || plan.getMaxPurchaseQuantity() < plan.getMinPurchaseQuantity()) {
                plan.setMaxPurchaseQuantity(defaultMaxQuantity(null, plan.getPeriodType()));
            }
            OshMemberPlan capPlan = planMap.get(plan.getCapPlanCode());
            int min = defaultQuantity(plan.getMinPurchaseQuantity());
            int max = effectiveMaxPurchaseQuantity(plan, capPlan);
            plan.setEffectiveMaxPurchaseQuantity(max);
            List<MemberPlanPriceTierVO> tiers = new ArrayList<>();
            for (int quantity = min; quantity <= max; quantity++) {
                MemberPlanPriceTierVO tier = new MemberPlanPriceTierVO();
                tier.setQuantity(quantity);
                tier.setDurationMonths(plan.getDurationMonths() * quantity);
                tier.setPrice(calculatePrice(plan, quantity, capPlan));
                tier.setOriginalPrice(defaultMoney(plan.getOriginalPrice(), plan.getPrice()).multiply(BigDecimal.valueOf(quantity)));
                tiers.add(tier);
            }
            plan.setPriceTiers(tiers);
        }
    }

    private BigDecimal calculatePrice(OshMemberPlan plan, int quantity, OshMemberPlan capPlan) {
        BigDecimal price = normalizePrice(geometricPrice(plan.getPrice(), defaultGrowthCoefficient(plan.getGrowthCoefficient(), plan), quantity));
        BigDecimal capAmount = capAmount(plan, capPlan);
        if (capAmount != null && price.compareTo(capAmount) >= 0) {
            price = endingEightBelow(capAmount);
        }
        if (price.compareTo(plan.getPrice()) < 0) {
            price = plan.getPrice();
        }
        return price;
    }

    private BigDecimal geometricPrice(BigDecimal basePrice, BigDecimal coefficient, int quantity) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE;
        for (int i = 0; i < quantity; i++) {
            total = total.add(basePrice.multiply(factor));
            factor = factor.multiply(coefficient);
        }
        return total;
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        BigDecimal rounded = value.setScale(0, RoundingMode.HALF_UP);
        BigDecimal lower = endingEightFloor(rounded);
        BigDecimal upper = lower.add(BigDecimal.TEN);
        BigDecimal lowerDistance = rounded.subtract(lower).abs();
        BigDecimal upperDistance = upper.subtract(rounded).abs();
        BigDecimal result = lowerDistance.compareTo(upperDistance) <= 0 ? lower : upper;
        return result.setScale(2, RoundingMode.UNNECESSARY);
    }

    private BigDecimal endingEightBelow(BigDecimal capAmount) {
        BigDecimal candidate = normalizePrice(capAmount.subtract(BigDecimal.ONE));
        while (candidate.compareTo(capAmount) >= 0) {
            candidate = candidate.subtract(BigDecimal.TEN);
        }
        return candidate.setScale(2, RoundingMode.UNNECESSARY);
    }

    private BigDecimal endingEightFloor(BigDecimal value) {
        BigDecimal shifted = value.subtract(new BigDecimal("8"));
        BigDecimal tens = shifted.divide(BigDecimal.TEN, 0, RoundingMode.FLOOR);
        return tens.multiply(BigDecimal.TEN).add(new BigDecimal("8"));
    }

    private BigDecimal capAmount(OshMemberPlan plan, OshMemberPlan capPlan) {
        if (capPlan == null || capPlan.getPrice() == null) {
            return null;
        }
        BigDecimal ratio = defaultCapRatio(plan.getCapRatio(), plan.getCapPlanCode());
        return capPlan.getPrice().multiply(ratio);
    }

    private int effectiveMaxPurchaseQuantity(OshMemberPlan plan, OshMemberPlan capPlan) {
        int min = defaultQuantity(plan.getMinPurchaseQuantity());
        int configuredMax = defaultMaxQuantity(plan.getMaxPurchaseQuantity(), plan.getPeriodType());
        BigDecimal capAmount = capAmount(plan, capPlan);
        if (capAmount == null) {
            return configuredMax;
        }
        int max = min;
        for (int quantity = min; quantity <= configuredMax; quantity++) {
            BigDecimal price = geometricPrice(plan.getPrice(), defaultGrowthCoefficient(plan.getGrowthCoefficient(), plan), quantity);
            if (price.compareTo(capAmount) >= 0) {
                break;
            }
            max = quantity;
        }
        return Math.max(min, max);
    }

    private int defaultQuantity(Integer quantity) {
        return quantity == null || quantity < 1 ? 1 : quantity;
    }

    private int defaultMaxQuantity(Integer quantity, String periodType) {
        if (quantity != null && quantity >= 1) {
            return quantity;
        }
        return "year".equals(periodType) ? 5 : 36;
    }

    private BigDecimal defaultGrowthCoefficient(BigDecimal value, OshMemberPlan plan) {
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            return value;
        }
        if (plan != null && "month".equals(plan.getPeriodType())) {
            return MONTH_GROWTH_COEFFICIENT;
        }
        if (plan != null && MEMBER_TYPE_SMALL_CLASS.equals(plan.getMemberType())) {
            return SMALL_CLASS_GROWTH_COEFFICIENT;
        }
        return DEFAULT_GROWTH_COEFFICIENT;
    }

    private BigDecimal defaultCapRatio(BigDecimal value, String capPlanCode) {
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            return value;
        }
        return normalizeText(capPlanCode) == null ? BigDecimal.ONE : DEFAULT_CAP_RATIO;
    }

    private String normalizeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal defaultMoney(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : fallback;
    }

    private boolean shouldDeleteFromEs(OshMemberPlan plan) {
        return !Byte.valueOf((byte) 0).equals(plan.getDeleteFlag())
                || !Integer.valueOf(PLAN_STATUS_ENABLED).equals(plan.getStatus());
    }

    private void deletePlanQuietly(Long planId) {
        try {
            homepageMemberPlanEsMapper.deleteById(planId);
        } catch (IOException exception) {
            log.warn("delete homepage member plan from es failed, planId={}", planId, exception);
        }
    }

    private ServiceException buildServiceException(String message, Throwable exception) {
        String detail = exception == null ? "" : exception.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            return new ServiceException(message);
        }
        return new ServiceException(message + "：" + detail);
    }
}
