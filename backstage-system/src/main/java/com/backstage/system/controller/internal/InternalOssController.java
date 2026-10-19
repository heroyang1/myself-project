package com.backstage.system.controller.internal;

import com.backstage.contract.common.InternalApiConstants;
import com.backstage.contract.common.InternalResult;
import com.backstage.contract.internal.oss.OssSignRequest;
import com.backstage.contract.internal.oss.OssSignResponse;
import com.backstage.system.service.common.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部对象存储接口
 *
 * <p>platform 域对其它服务暴露的对象存储签名能力，仅供服务间内网调用。委托单体现有 OssService，
 * 由 objectKey 生成有时效的临时访问 URL。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
@RestController
@RequestMapping(InternalApiConstants.OSS_BASE)
public class InternalOssController {

    @Autowired
    private OssService ossService;

    /**
     * 生成对象存储临时签名 URL
     *
     * @param request 签名请求（objectKey + 有效期分钟）
     * @return 临时访问 URL
     */
    @PostMapping("/sign")
    public InternalResult<OssSignResponse> sign(@RequestBody OssSignRequest request) {
        if (request.getObjectKey() == null || request.getObjectKey().trim().isEmpty()) {
            throw new IllegalArgumentException("objectKey 不能为空");
        }
        if (request.getExpireMinutes() == null || request.getExpireMinutes() <= 0) {
            throw new IllegalArgumentException("签名有效期必须大于0分钟");
        }
        String url = ossService.getLimitedUrl(request.getObjectKey(), request.getExpireMinutes());
        return InternalResult.ok(new OssSignResponse(request.getObjectKey(), url, request.getExpireMinutes()));
    }
}
