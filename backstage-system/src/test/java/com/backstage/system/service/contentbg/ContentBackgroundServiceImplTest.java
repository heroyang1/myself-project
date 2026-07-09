package com.backstage.system.service.contentbg;

import com.backstage.common.enums.UploadPathEnum;
import com.backstage.system.domain.contentbg.OshContentBgImage;
import com.backstage.system.domain.contentbg.OshContentBgStyle;
import com.backstage.system.domain.contentbg.OshUserContentBgSetting;
import com.backstage.system.domain.vo.contentbg.ContentBackgroundOptionsVO;
import com.backstage.system.domain.vo.contentbg.ContentBgImageUploadVO;
import com.backstage.system.mapper.contentbg.OshContentBgImageMapper;
import com.backstage.system.mapper.contentbg.OshContentBgStyleMapper;
import com.backstage.system.mapper.contentbg.OshUserContentBgSettingMapper;
import com.backstage.system.request.contentbg.ContentBgSelectRequest;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.contentbg.impl.ContentBackgroundServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContentBackgroundServiceImplTest {

    @Test
    public void shouldUploadBackgroundImageByStyleCodeAndPersistRecord() throws Exception {
        ContentBackgroundServiceImpl service = new ContentBackgroundServiceImpl();
        OshContentBgStyleMapper styleMapper = mock(OshContentBgStyleMapper.class);
        OshContentBgImageMapper imageMapper = mock(OshContentBgImageMapper.class);
        OssService ossService = mock(OssService.class);
        ReflectionTestUtils.setField(service, "styleMapper", styleMapper);
        ReflectionTestUtils.setField(service, "imageMapper", imageMapper);
        ReflectionTestUtils.setField(service, "ossService", ossService);

        OshContentBgStyle style = new OshContentBgStyle();
        style.setId(11L);
        style.setCode("anime");
        style.setName("动漫");
        when(styleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(style);
        when(ossService.upload(any(MockMultipartFile.class), eq(UploadPathEnum.CONTENT_BACKGROUND), eq("anime")))
                .thenReturn("common/image/contentBackground/anime/202606/bg.png");
        when(ossService.getLimitedUrl("common/image/contentBackground/anime/202606/bg.png", 1440))
                .thenReturn("https://cdn.example.com/bg.png?sign=1");
        when(imageMapper.insert(any(OshContentBgImage.class))).thenAnswer(invocation -> {
            OshContentBgImage image = invocation.getArgument(0);
            image.setId(99L);
            return 1;
        });

        MockMultipartFile file = new MockMultipartFile("file", "bg.png", "image/png", new byte[]{1, 2, 3});
        ContentBgImageUploadVO result = service.uploadImage(
                "anime",
                "动漫背景",
                file,
                "#111827",
                "rgba(255,255,255,.72)",
                new BigDecimal("0.86"),
                12,
                true,
                "admin"
        );

        assertNotNull(result);
        assertEquals(Long.valueOf(99L), result.getImageId());
        assertEquals(Long.valueOf(11L), result.getStyleId());
        assertEquals("anime", result.getStyleCode());
        assertEquals("common/image/contentBackground/anime/202606/bg.png", result.getImagePath());
        assertEquals("https://cdn.example.com/bg.png?sign=1", result.getImageUrl());
        assertEquals("bg.png", result.getFileName());

        ArgumentCaptor<OshContentBgImage> captor = ArgumentCaptor.forClass(OshContentBgImage.class);
        verify(imageMapper).clearGlobalDefault("admin");
        verify(imageMapper).insert(captor.capture());
        OshContentBgImage saved = captor.getValue();
        assertEquals(Long.valueOf(11L), saved.getStyleId());
        assertEquals("动漫背景", saved.getName());
        assertEquals("common/image/contentBackground/anime/202606/bg.png", saved.getImagePath());
        assertEquals("common/image/contentBackground/anime/202606/bg.png", saved.getPreviewPath());
        assertEquals(Integer.valueOf(1), saved.getIsDefault());
        assertEquals(Integer.valueOf(4), saved.getStatus());
    }

    @Test
    public void shouldReturnGlobalDefaultBackgroundWhenUserHasNoSelection() {
        ContentBackgroundServiceImpl service = new ContentBackgroundServiceImpl();
        OshContentBgStyleMapper styleMapper = mock(OshContentBgStyleMapper.class);
        OshContentBgImageMapper imageMapper = mock(OshContentBgImageMapper.class);
        OshUserContentBgSettingMapper userSettingMapper = mock(OshUserContentBgSettingMapper.class);
        OssService ossService = mock(OssService.class);
        ReflectionTestUtils.setField(service, "styleMapper", styleMapper);
        ReflectionTestUtils.setField(service, "imageMapper", imageMapper);
        ReflectionTestUtils.setField(service, "userSettingMapper", userSettingMapper);
        ReflectionTestUtils.setField(service, "ossService", ossService);

        OshContentBgStyle animal = new OshContentBgStyle();
        animal.setId(3L);
        animal.setCode("animal");
        animal.setName("动物");
        animal.setSort(30);

        OshContentBgStyle ancient = new OshContentBgStyle();
        ancient.setId(6L);
        ancient.setCode("ancient_style");
        ancient.setName("古风");
        ancient.setSort(60);

        OshContentBgImage globalDefault = new OshContentBgImage();
        globalDefault.setId(1L);
        globalDefault.setStyleId(3L);
        globalDefault.setName("两匹马");
        globalDefault.setImagePath("common/image/contentBackground/animal/default.jpg");
        globalDefault.setPreviewPath("common/image/contentBackground/animal/default.jpg");
        globalDefault.setOverlayColor("rgba(255,255,255,.72)");
        globalDefault.setOpacity(new BigDecimal("1.00"));
        globalDefault.setIsDefault(1);
        globalDefault.setSort(0);

        OshContentBgImage other = new OshContentBgImage();
        other.setId(2L);
        other.setStyleId(6L);
        other.setName("古风画");
        other.setImagePath("common/image/contentBackground/ancient_style/bg.jpg");
        other.setPreviewPath("common/image/contentBackground/ancient_style/bg.jpg");
        other.setIsDefault(0);
        other.setSort(0);

        when(styleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(animal, ancient));
        when(imageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(other, globalDefault));
        when(userSettingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(ossService.getLimitedUrl("common/image/contentBackground/animal/default.jpg", 1440))
                .thenReturn("https://cdn.example.com/default.jpg?sign=1");
        when(ossService.getLimitedUrl("common/image/contentBackground/ancient_style/bg.jpg", 1440))
                .thenReturn("https://cdn.example.com/ancient.jpg?sign=1");

        ContentBackgroundOptionsVO result = service.getOptions(123L);

        assertNotNull(result);
        assertEquals(Long.valueOf(3L), result.getCurrentStyleId());
        assertEquals("animal", result.getCurrentStyleCode());
        assertEquals("动物", result.getCurrentStyleName());
        assertEquals(Long.valueOf(1L), result.getCurrentImageId());
        assertEquals("https://cdn.example.com/default.jpg?sign=1", result.getCurrentImageUrl());
        assertEquals(Boolean.TRUE, result.getStyles().stream()
                .flatMap(style -> style.getImages().stream())
                .filter(image -> Long.valueOf(1L).equals(image.getImageId()))
                .findFirst()
                .get()
                .getSelected());
    }

    @Test
    public void shouldKeepUploadedBackgroundRecordWhenSignedUrlGenerationFails() throws Exception {
        ContentBackgroundServiceImpl service = new ContentBackgroundServiceImpl();
        OshContentBgStyleMapper styleMapper = mock(OshContentBgStyleMapper.class);
        OshContentBgImageMapper imageMapper = mock(OshContentBgImageMapper.class);
        OssService ossService = mock(OssService.class);
        ReflectionTestUtils.setField(service, "styleMapper", styleMapper);
        ReflectionTestUtils.setField(service, "imageMapper", imageMapper);
        ReflectionTestUtils.setField(service, "ossService", ossService);

        OshContentBgStyle style = new OshContentBgStyle();
        style.setId(1L);
        style.setCode("natural");
        style.setName("自然");
        when(styleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(style);
        when(ossService.upload(any(MockMultipartFile.class), eq(UploadPathEnum.CONTENT_BACKGROUND), eq("natural")))
                .thenReturn("common/image/contentBackground/natural/202606/bg.png");
        when(ossService.getLimitedUrl("common/image/contentBackground/natural/202606/bg.png", 1440))
                .thenThrow(new RuntimeException("sign failed"));
        when(imageMapper.insert(any(OshContentBgImage.class))).thenAnswer(invocation -> {
            OshContentBgImage image = invocation.getArgument(0);
            image.setId(101L);
            return 1;
        });

        MockMultipartFile file = new MockMultipartFile("file", "bg.png", "image/png", new byte[]{1});
        ContentBgImageUploadVO result = service.uploadImage(
                "natural",
                "",
                file,
                null,
                null,
                null,
                null,
                false,
                "admin"
        );

        assertNotNull(result);
        assertEquals(Long.valueOf(101L), result.getImageId());
        assertEquals("common/image/contentBackground/natural/202606/bg.png", result.getImagePath());
        verify(imageMapper, times(1)).insert(any(OshContentBgImage.class));
    }

    @Test
    public void shouldPersistSelectionAndReturnCurrentBackgroundUrl() {
        ContentBackgroundServiceImpl service = new ContentBackgroundServiceImpl();
        OshContentBgStyleMapper styleMapper = mock(OshContentBgStyleMapper.class);
        OshContentBgImageMapper imageMapper = mock(OshContentBgImageMapper.class);
        OshUserContentBgSettingMapper userSettingMapper = mock(OshUserContentBgSettingMapper.class);
        OssService ossService = mock(OssService.class);
        ReflectionTestUtils.setField(service, "styleMapper", styleMapper);
        ReflectionTestUtils.setField(service, "imageMapper", imageMapper);
        ReflectionTestUtils.setField(service, "userSettingMapper", userSettingMapper);
        ReflectionTestUtils.setField(service, "ossService", ossService);

        OshContentBgStyle style = new OshContentBgStyle();
        style.setId(6L);
        style.setCode("ancient_style");
        style.setName("古风");
        style.setSort(60);

        OshContentBgImage image = new OshContentBgImage();
        image.setId(88L);
        image.setStyleId(6L);
        image.setName("古风背景");
        image.setImagePath("common/image/contentBackground/ancient_style/202606/bg.jpg");
        image.setPreviewPath("common/image/contentBackground/ancient_style/202606/bg.jpg");
        image.setOverlayColor("rgba(255,255,255,.72)");
        image.setOpacity(new BigDecimal("1.00"));
        image.setIsDefault(0);
        image.setSort(1);

        AtomicReference<OshUserContentBgSetting> persistedSetting = new AtomicReference<>();
        when(userSettingMapper.selectOne(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> persistedSetting.get());
        when(userSettingMapper.insert(any(OshUserContentBgSetting.class))).thenAnswer(invocation -> {
            OshUserContentBgSetting setting = invocation.getArgument(0);
            setting.setId(100L);
            persistedSetting.set(setting);
            return 1;
        });
        when(styleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(style);
        when(styleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(style));
        when(imageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(image);
        when(imageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(image));
        when(ossService.getLimitedUrl("common/image/contentBackground/ancient_style/202606/bg.jpg", 1440))
                .thenReturn("https://cdn.example.com/bg.jpg?sign=1");

        ContentBgSelectRequest request = new ContentBgSelectRequest();
        request.setStyleId(6L);
        request.setImageId(88L);
        ContentBackgroundOptionsVO result = service.saveSelection(123L, "admin", request);

        assertNotNull(result);
        assertEquals(Long.valueOf(6L), result.getCurrentStyleId());
        assertEquals("ancient_style", result.getCurrentStyleCode());
        assertEquals(Long.valueOf(88L), result.getCurrentImageId());
        assertEquals("https://cdn.example.com/bg.jpg?sign=1", result.getCurrentImageUrl());

        OshUserContentBgSetting saved = persistedSetting.get();
        assertNotNull(saved);
        assertEquals(Long.valueOf(123L), saved.getUserId());
        assertEquals(Long.valueOf(6L), saved.getStyleId());
        assertEquals(Long.valueOf(88L), saved.getImageId());
        assertEquals("admin", saved.getCreateBy());
        assertEquals("admin", saved.getUpdateBy());
    }

    @Test
    public void shouldReturnNoCurrentBackgroundWhenNoGlobalDefaultExists() {
        ContentBackgroundServiceImpl service = new ContentBackgroundServiceImpl();
        OshContentBgStyleMapper styleMapper = mock(OshContentBgStyleMapper.class);
        OshContentBgImageMapper imageMapper = mock(OshContentBgImageMapper.class);
        OshUserContentBgSettingMapper userSettingMapper = mock(OshUserContentBgSettingMapper.class);
        OssService ossService = mock(OssService.class);
        ReflectionTestUtils.setField(service, "styleMapper", styleMapper);
        ReflectionTestUtils.setField(service, "imageMapper", imageMapper);
        ReflectionTestUtils.setField(service, "userSettingMapper", userSettingMapper);
        ReflectionTestUtils.setField(service, "ossService", ossService);

        OshContentBgStyle natural = new OshContentBgStyle();
        natural.setId(7L);
        natural.setCode("natural");
        natural.setName("自然");
        natural.setSort(10);

        OshContentBgImage availableImage = new OshContentBgImage();
        availableImage.setId(71L);
        availableImage.setStyleId(7L);
        availableImage.setName("山林");
        availableImage.setImagePath("common/image/contentBackground/natural/bg.jpg");
        availableImage.setPreviewPath("common/image/contentBackground/natural/bg.jpg");
        availableImage.setIsDefault(0);
        availableImage.setSort(1);

        when(styleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(natural));
        when(imageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(availableImage));
        when(userSettingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(ossService.getLimitedUrl("common/image/contentBackground/natural/bg.jpg", 1440))
                .thenReturn("https://cdn.example.com/natural.jpg?sign=1");

        ContentBackgroundOptionsVO result = service.getOptions(null);

        assertNotNull(result);
        assertEquals(null, result.getCurrentImageId());
        assertEquals(null, result.getCurrentStyleId());
        assertEquals(null, result.getCurrentStyleCode());
        assertEquals(null, result.getCurrentStyleName());
        assertEquals(null, result.getCurrentImageUrl());
        assertEquals(Boolean.FALSE, result.getStyles().get(0).getImages().get(0).getSelected());
    }

    @Test
    public void shouldDeleteBackgroundImageById() {
        ContentBackgroundServiceImpl service = new ContentBackgroundServiceImpl();
        OshContentBgImageMapper imageMapper = mock(OshContentBgImageMapper.class);
        ReflectionTestUtils.setField(service, "imageMapper", imageMapper);

        OshContentBgImage image = new OshContentBgImage();
        image.setId(88L);
        image.setDeleteFlag(0);
        image.setIsDefault(0);
        when(imageMapper.selectById(88L)).thenReturn(image);

        service.deleteImage(9L, "tester", 88L);

        assertEquals(Integer.valueOf(1), image.getDeleteFlag());
        assertEquals("tester", image.getUpdateBy());
        assertNotNull(image.getUpdateTime());
        verify(imageMapper).updateById(image);
    }

    @Test
    public void shouldClearUserSelectionWhenDeletingSelectedImage() {
        ContentBackgroundServiceImpl service = new ContentBackgroundServiceImpl();
        OshContentBgImageMapper imageMapper = mock(OshContentBgImageMapper.class);
        OshUserContentBgSettingMapper userSettingMapper = mock(OshUserContentBgSettingMapper.class);
        ReflectionTestUtils.setField(service, "imageMapper", imageMapper);
        ReflectionTestUtils.setField(service, "userSettingMapper", userSettingMapper);

        OshContentBgImage image = new OshContentBgImage();
        image.setId(88L);
        image.setDeleteFlag(0);
        when(imageMapper.selectById(88L)).thenReturn(image);

        OshUserContentBgSetting setting = new OshUserContentBgSetting();
        setting.setId(3L);
        setting.setUserId(9L);
        setting.setImageId(88L);
        setting.setStyleId(2L);
        when(userSettingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(setting);

        service.deleteImage(9L, "tester", 88L);

        assertNull(setting.getImageId());
        assertNull(setting.getStyleId());
        verify(userSettingMapper).updateById(setting);
    }
}
