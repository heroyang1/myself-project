package com.backstage.system.mapper.bundle;

import com.backstage.system.domain.bundle.vo.BundleResourceVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OshBundleResourceMapper {
    List<BundleResourceVO> searchResources(@Param("resourceType") String resourceType,
                                           @Param("keyword") String keyword);
}
