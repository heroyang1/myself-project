package com.backstage.system.service.contentbg;

import com.backstage.system.domain.vo.contentbg.ContentBackgroundOptionsVO;
import com.backstage.system.domain.vo.contentbg.ContentBgImageUploadVO;
import com.backstage.system.request.contentbg.ContentBgSelectRequest;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface IContentBackgroundService {

    ContentBgImageUploadVO uploadImage(String styleCode,
                                       String name,
                                       MultipartFile file,
                                       String dominantColor,
                                       String overlayColor,
                                       BigDecimal opacity,
                                       Integer sort,
                                       Boolean isDefault,
                                       String operator) throws Exception;

    ContentBackgroundOptionsVO getOptions(Long userId);

    ContentBackgroundOptionsVO saveSelection(Long userId,
                                             String operator,
                                             ContentBgSelectRequest request);

    void deleteImage(Long userId,
                     String operator,
                     Long imageId);
}
