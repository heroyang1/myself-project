package com.backstage.system.service.bundle;

import com.backstage.system.domain.bundle.dto.BundlePageQuery;
import com.backstage.system.domain.bundle.dto.BundleResourceSearchQuery;
import com.backstage.system.domain.bundle.dto.BundleSaveRequest;
import com.backstage.system.domain.bundle.dto.BundleStatusRequest;
import com.backstage.system.domain.bundle.vo.BundleDetailVO;
import com.backstage.system.domain.bundle.vo.BundleListVO;
import com.backstage.system.domain.bundle.vo.BundleResourceVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

public interface ProductBundleService {
    PageInfo<BundleListVO> pageAdminBundles(BundlePageQuery query);

    BundleDetailVO getDetail(Long id);

    BundleDetailVO save(BundleSaveRequest request);

    void updateStatus(BundleStatusRequest request);

    void delete(Long id);

    PageInfo<BundleResourceVO> searchResources(BundleResourceSearchQuery query);

    List<BundleDetailVO> recommendByResource(String resourceType, Long resourceId);

    OrderCheckoutRespVO createBundlePay(Long bundleId, String payType, String clientIp, Long userId);

    Map<String, Object> getBundlePayStatus(String orderNo, Long userId);

    void grantBundleAccess(String orderNo);
}
