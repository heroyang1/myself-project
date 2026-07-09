package com.backstage.system.service.contentbg.impl;

import com.backstage.common.enums.UploadPathEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.constants.CourseUploadConstants;
import com.backstage.system.domain.contentbg.OshContentBgImage;
import com.backstage.system.domain.contentbg.OshContentBgStyle;
import com.backstage.system.domain.contentbg.OshUserContentBgSetting;
import com.backstage.system.domain.vo.contentbg.ContentBackgroundOptionsVO;
import com.backstage.system.domain.vo.contentbg.ContentBgImageUploadVO;
import com.backstage.system.domain.vo.contentbg.ContentBgImageVO;
import com.backstage.system.domain.vo.contentbg.ContentBgStyleVO;
import com.backstage.system.mapper.contentbg.OshContentBgImageMapper;
import com.backstage.system.mapper.contentbg.OshContentBgStyleMapper;
import com.backstage.system.mapper.contentbg.OshUserContentBgSettingMapper;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.contentbg.IContentBackgroundService;
import com.backstage.system.request.contentbg.ContentBgSelectRequest;
import com.backstage.system.utils.OssUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ContentBackgroundServiceImpl implements IContentBackgroundService {

    private static final Logger log = LoggerFactory.getLogger(ContentBackgroundServiceImpl.class);

    private static final int ENABLED_STATUS = 4;
    private static final int SIGNED_URL_MINUTES = 1440;
    private static final BigDecimal DEFAULT_OPACITY = new BigDecimal("1.00");
    private static final List<String> ALLOWED_BACKGROUND_EXTENSIONS = Arrays.asList("bmp", "gif", "jpg", "jpeg", "png", "webp");

    @Autowired
    private OshContentBgStyleMapper styleMapper;

    @Autowired
    private OshContentBgImageMapper imageMapper;

    @Autowired
    private OshUserContentBgSettingMapper userSettingMapper;

    @Autowired
    private OssService ossService;

    @Autowired
    private OssUtil ossUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentBgImageUploadVO uploadImage(String styleCode,
                                              String name,
                                              MultipartFile file,
                                              String dominantColor,
                                              String overlayColor,
                                              BigDecimal opacity,
                                              Integer sort,
                                              Boolean isDefault,
                                              String operator) throws Exception {
        validateFile(file);
        String normalizedStyleCode = normalizeStyleCode(styleCode);
        OshContentBgStyle style = selectEnabledStyle(normalizedStyleCode);
        if (style == null) {
            throw new ServiceException("背景风格不存在或未启用");
        }

        String imagePath = ossService.upload(file, UploadPathEnum.CONTENT_BACKGROUND, normalizedStyleCode);
        if (StringUtils.isBlank(imagePath) || CourseUploadConstants.isUploadError(imagePath)) {
            throw new ServiceException(StringUtils.isBlank(imagePath) ? "上传背景图失败" : imagePath);
        }

        String actualOperator = StringUtils.defaultIfBlank(operator, "system");
        boolean defaultFlag = Boolean.TRUE.equals(isDefault);
        if (defaultFlag) {
            imageMapper.clearGlobalDefault(actualOperator);
        }

        OshContentBgImage image = new OshContentBgImage();
        image.setStyleId(style.getId());
        image.setName(StringUtils.defaultIfBlank(name, file.getOriginalFilename()));
        image.setImagePath(imagePath);
        image.setPreviewPath(imagePath);
        image.setDominantColor(StringUtils.trimToNull(dominantColor));
        image.setOverlayColor(StringUtils.trimToNull(overlayColor));
        image.setOpacity(opacity == null ? DEFAULT_OPACITY : opacity);
        image.setIsDefault(defaultFlag ? 1 : 0);
        image.setSort(sort == null ? 0 : sort);
        image.setStatus(ENABLED_STATUS);
        image.setDeleteFlag(0);
        image.setCreateBy(actualOperator);
        image.setUpdateBy(actualOperator);
        imageMapper.insert(image);

        ContentBgImageUploadVO vo = new ContentBgImageUploadVO();
        vo.setImageId(image.getId());
        vo.setStyleId(style.getId());
        vo.setStyleCode(style.getCode());
        vo.setImagePath(imagePath);
        vo.setImageUrl(safeLimitedUrl(imagePath));
        vo.setPreviewPath(image.getPreviewPath());
        vo.setPreviewUrl(safeLimitedUrl(image.getPreviewPath()));
        vo.setOverlayColor(image.getOverlayColor());
        vo.setOpacity(image.getOpacity());
        vo.setFileName(file.getOriginalFilename());
        vo.setFileSize(file.getSize());
        vo.setFileType(file.getContentType());
        return vo;
    }

    @Override
    public ContentBackgroundOptionsVO getOptions(Long userId) {
        List<OshContentBgStyle> styles = styleMapper.selectList(new LambdaQueryWrapper<OshContentBgStyle>()
                .eq(OshContentBgStyle::getStatus, ENABLED_STATUS)
                .eq(OshContentBgStyle::getDeleteFlag, 0)
                .orderByAsc(OshContentBgStyle::getSort)
                .orderByAsc(OshContentBgStyle::getId));

        List<OshContentBgImage> images = imageMapper.selectList(new LambdaQueryWrapper<OshContentBgImage>()
                .eq(OshContentBgImage::getStatus, ENABLED_STATUS)
                .eq(OshContentBgImage::getDeleteFlag, 0)
                .orderByAsc(OshContentBgImage::getStyleId)
                .orderByDesc(OshContentBgImage::getIsDefault)
                .orderByAsc(OshContentBgImage::getSort)
                .orderByAsc(OshContentBgImage::getId));

        Map<Long, List<OshContentBgImage>> imageMap = images.stream()
                .filter(item -> item.getStyleId() != null)
                .collect(Collectors.groupingBy(OshContentBgImage::getStyleId, LinkedHashMap::new, Collectors.toList()));

        OshUserContentBgSetting userSetting = loadUserSetting(userId);
        OshContentBgImage selectedImage = resolveSelectedImage(styles, imageMap, userSetting);
        ContentBackgroundOptionsVO optionsVO = buildOptionsVO(styles, imageMap, selectedImage);
        if (selectedImage != null) {
            optionsVO.setCurrentOverlayColor(selectedImage.getOverlayColor());
            optionsVO.setCurrentOpacity(selectedImage.getOpacity());
        }
        return optionsVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentBackgroundOptionsVO saveSelection(Long userId, String operator, ContentBgSelectRequest request) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        if (request == null) {
            throw new ServiceException("背景选择参数不能为空");
        }
        String actualOperator = StringUtils.defaultIfBlank(operator, "system");
        OshUserContentBgSetting setting = userSettingMapper.selectOne(new LambdaQueryWrapper<OshUserContentBgSetting>()
                .eq(OshUserContentBgSetting::getUserId, userId)
                .eq(OshUserContentBgSetting::getDeleteFlag, 0)
                .last("limit 1"));

        if (setting == null) {
            setting = new OshUserContentBgSetting();
            setting.setUserId(userId);
            setting.setCreateBy(actualOperator);
            setting.setCreateTime(LocalDateTime.now());
        }

        setting.setStatus(ENABLED_STATUS);
        setting.setDeleteFlag(0);
        setting.setUpdateBy(actualOperator);
        setting.setUpdateTime(LocalDateTime.now());

        OshContentBgImage selectedImage = requireValidImage(request.getImageId());
        OshContentBgStyle selectedStyle = requireValidStyle(request.getStyleId(), selectedImage.getStyleId());
        setting.setStyleId(selectedStyle.getId());
        setting.setImageId(selectedImage.getId());
        setting.setCustomOverlayColor(selectedImage.getOverlayColor());
        setting.setCustomOpacity(selectedImage.getOpacity());

        if (setting.getId() == null) {
            userSettingMapper.insert(setting);
        } else {
            userSettingMapper.updateById(setting);
        }
        return getOptions(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(Long userId, String operator, Long imageId) {
        if (imageId == null) {
            throw new ServiceException("背景图ID不能为空");
        }
        OshContentBgImage image = imageMapper.selectById(imageId);
        if (image == null || Integer.valueOf(1).equals(image.getDeleteFlag())) {
            return;
        }
        image.setDeleteFlag(1);
        image.setUpdateBy(StringUtils.defaultIfBlank(operator, "system"));
        image.setUpdateTime(LocalDateTime.now());
        imageMapper.updateById(image);

        if (userId == null) {
            return;
        }
        OshUserContentBgSetting setting = userSettingMapper.selectOne(new LambdaQueryWrapper<OshUserContentBgSetting>()
                .eq(OshUserContentBgSetting::getUserId, userId)
                .eq(OshUserContentBgSetting::getDeleteFlag, 0)
                .last("limit 1"));
        if (setting == null || !Objects.equals(setting.getImageId(), imageId)) {
            return;
        }
        setting.setImageId(null);
        setting.setStyleId(null);
        setting.setCustomOverlayColor(null);
        setting.setCustomOpacity(null);
        setting.setUpdateBy(StringUtils.defaultIfBlank(operator, "system"));
        setting.setUpdateTime(LocalDateTime.now());
        userSettingMapper.updateById(setting);
    }

    private OshContentBgStyle selectEnabledStyle(String styleCode) {
        LambdaQueryWrapper<OshContentBgStyle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshContentBgStyle::getCode, styleCode)
                .eq(OshContentBgStyle::getStatus, ENABLED_STATUS)
                .eq(OshContentBgStyle::getDeleteFlag, 0)
                .last("limit 1");
        return styleMapper.selectOne(wrapper);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("背景图不能为空");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_BACKGROUND_EXTENSIONS.contains(extension)) {
            throw new ServiceException("背景图仅支持 bmp、gif、jpg、jpeg、png、webp 格式");
        }
        if (file.getSize() > CourseUploadConstants.MAX_IMAGE_SIZE) {
            throw new ServiceException("背景图大小不能超过 5MB");
        }
    }

    private String normalizeStyleCode(String styleCode) {
        String code = StringUtils.trimToEmpty(styleCode).toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(code)) {
            throw new ServiceException("背景风格编码不能为空");
        }
        return code;
    }

    private String resolveExtension(String fileName) {
        if (StringUtils.isBlank(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private OshUserContentBgSetting loadUserSetting(Long userId) {
        if (userId == null) {
            return null;
        }
        return userSettingMapper.selectOne(new LambdaQueryWrapper<OshUserContentBgSetting>()
                .eq(OshUserContentBgSetting::getUserId, userId)
                .eq(OshUserContentBgSetting::getDeleteFlag, 0)
                .last("limit 1"));
    }

    private OshContentBgImage resolveSelectedImage(List<OshContentBgStyle> styles,
                                                   Map<Long, List<OshContentBgImage>> imageMap,
                                                   OshUserContentBgSetting userSetting) {
        if (userSetting != null && userSetting.getImageId() != null) {
            for (List<OshContentBgImage> list : imageMap.values()) {
                for (OshContentBgImage image : list) {
                    if (Objects.equals(image.getId(), userSetting.getImageId())) {
                        return image;
                    }
                }
            }
        }
        for (List<OshContentBgImage> list : imageMap.values()) {
            for (OshContentBgImage image : list) {
                if (Integer.valueOf(1).equals(image.getIsDefault())) {
                    return image;
                }
            }
        }
        return null;
    }

    private ContentBackgroundOptionsVO buildOptionsVO(List<OshContentBgStyle> styles,
                                                      Map<Long, List<OshContentBgImage>> imageMap,
                                                      OshContentBgImage selectedImage) {
        ContentBackgroundOptionsVO optionsVO = new ContentBackgroundOptionsVO();
        if (selectedImage != null) {
            optionsVO.setCurrentImageId(selectedImage.getId());
            optionsVO.setCurrentStyleId(selectedImage.getStyleId());
        }

        Map<Long, OshContentBgStyle> styleById = styles.stream()
                .collect(Collectors.toMap(OshContentBgStyle::getId, style -> style, (left, right) -> left, LinkedHashMap::new));
        List<ContentBgStyleVO> styleVos = new ArrayList<>();
        for (OshContentBgStyle style : styles) {
            ContentBgStyleVO styleVO = new ContentBgStyleVO();
            styleVO.setStyleId(style.getId());
            styleVO.setStyleCode(style.getCode());
            styleVO.setName(style.getName());
            styleVO.setDescription(style.getDescription());
            styleVO.setSort(style.getSort());
            List<OshContentBgImage> styleImages = imageMap.getOrDefault(style.getId(), new ArrayList<>());
            styleVO.setCoverImageUrl(resolveStyleCoverUrl(style, styleImages));
            List<ContentBgImageVO> imageVOs = styleImages.stream().map(image -> buildImageVO(image, selectedImage)).collect(Collectors.toList());
            styleVO.setImages(imageVOs);
            styleVos.add(styleVO);
        }
        optionsVO.setStyles(styleVos);

        if (selectedImage != null) {
            OshContentBgStyle selectedStyle = styleById.get(selectedImage.getStyleId());
            if (selectedStyle != null) {
                optionsVO.setCurrentStyleId(selectedStyle.getId());
                optionsVO.setCurrentStyleCode(selectedStyle.getCode());
                optionsVO.setCurrentStyleName(selectedStyle.getName());
                optionsVO.setCurrentImageUrl(safeLimitedUrl(selectedImage.getImagePath()));
                optionsVO.setCurrentPreviewUrl(safeLimitedUrl(selectedImage.getPreviewPath()));
            }
        }
        return optionsVO;
    }

    private String resolveStyleCoverUrl(OshContentBgStyle style, List<OshContentBgImage> styleImages) {
        if (style != null && StringUtils.isNotBlank(style.getCoverImage())) {
            return safeLimitedUrl(style.getCoverImage());
        }
        if (styleImages == null || styleImages.isEmpty()) {
            return null;
        }
        OshContentBgImage first = styleImages.get(0);
        if (first == null || StringUtils.isBlank(first.getPreviewPath())) {
            return null;
        }
        return safeLimitedUrl(first.getPreviewPath());
    }

    private ContentBgImageVO buildImageVO(OshContentBgImage image, OshContentBgImage selectedImage) {
        ContentBgImageVO vo = new ContentBgImageVO();
        vo.setImageId(image.getId());
        vo.setStyleId(image.getStyleId());
        vo.setName(image.getName());
        vo.setImagePath(image.getImagePath());
        vo.setImageUrl(safeLimitedUrl(image.getImagePath()));
        vo.setPreviewPath(image.getPreviewPath());
        vo.setPreviewUrl(safeLimitedUrl(image.getPreviewPath()));
        vo.setDominantColor(image.getDominantColor());
        vo.setOverlayColor(image.getOverlayColor());
        vo.setOpacity(image.getOpacity());
        vo.setIsDefault(image.getIsDefault());
        vo.setSort(image.getSort());
        vo.setSelected(selectedImage != null && Objects.equals(selectedImage.getId(), image.getId()));
        return vo;
    }

    private String safeLimitedUrl(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        if (StringUtils.startsWithIgnoreCase(path, "http://")
                || StringUtils.startsWithIgnoreCase(path, "https://")) {
            return path;
        }
        try {
            String signedUrl = ossService.getLimitedUrl(path, SIGNED_URL_MINUTES);
            if (StringUtils.isNotBlank(signedUrl)) {
                return signedUrl;
            }
        } catch (Exception ex) {
            log.warn("生成内容背景图签名地址失败，回退公开地址，path={}", path, ex);
        }
        return buildPublicOssUrl(path);
    }

    private String buildPublicOssUrl(String path) {
        if (StringUtils.isBlank(path) || ossUtil == null || ossUtil.getOssProperties() == null) {
            return "";
        }
        String publicDomain = StringUtils.removeEnd(ossUtil.getOssProperties().getPublicDomain(), "/");
        if (StringUtils.isBlank(publicDomain)) {
            return "";
        }
        return publicDomain + "/" + StringUtils.removeStart(path, "/");
    }

    private OshContentBgImage requireValidImage(Long imageId) {
        if (imageId == null) {
            throw new ServiceException("背景图ID不能为空");
        }
        OshContentBgImage image = imageMapper.selectOne(new LambdaQueryWrapper<OshContentBgImage>()
                .eq(OshContentBgImage::getId, imageId)
                .eq(OshContentBgImage::getStatus, ENABLED_STATUS)
                .eq(OshContentBgImage::getDeleteFlag, 0)
                .last("limit 1"));
        if (image == null) {
            throw new ServiceException("背景图不存在或已下线");
        }
        return image;
    }

    private OshContentBgStyle requireValidStyle(Long styleId, Long imageStyleId) {
        if (styleId != null && imageStyleId != null && !Objects.equals(styleId, imageStyleId)) {
            throw new ServiceException("背景风格与背景图不匹配");
        }
        Long effectiveStyleId = styleId != null ? styleId : imageStyleId;
        if (effectiveStyleId == null) {
            throw new ServiceException("背景风格不能为空");
        }
        OshContentBgStyle style = styleMapper.selectOne(new LambdaQueryWrapper<OshContentBgStyle>()
                .eq(OshContentBgStyle::getId, effectiveStyleId)
                .eq(OshContentBgStyle::getStatus, ENABLED_STATUS)
                .eq(OshContentBgStyle::getDeleteFlag, 0)
                .last("limit 1"));
        if (style == null) {
            throw new ServiceException("背景风格不存在或已下线");
        }
        return style;
    }
}
