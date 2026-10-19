package com.backstage.system.controller.contentbg;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.domain.R;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.contentbg.ContentBackgroundOptionsVO;
import com.backstage.system.service.contentbg.IContentBackgroundService;
import org.junit.After;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContentBackgroundControllerTest {

    @After
    public void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    public void shouldAllowAnonymousOptionsEndpoint() throws Exception {
        Method method = ContentBackgroundController.class.getMethod("getOptions");

        assertTrue(method.isAnnotationPresent(Anonymous.class));
        assertNull(method.getAnnotation(OshUserLevel.class));
    }

    @Test
    public void shouldReturnOptionsWithoutLoginContext() {
        ContentBackgroundController controller = new ContentBackgroundController();
        IContentBackgroundService contentBackgroundService = mock(IContentBackgroundService.class);
        ReflectionTestUtils.setField(controller, "contentBackgroundService", contentBackgroundService);

        ContentBackgroundOptionsVO optionsVO = new ContentBackgroundOptionsVO();
        when(contentBackgroundService.getOptions(eq(null))).thenReturn(optionsVO);

        R<ContentBackgroundOptionsVO> result = controller.getOptions();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(optionsVO, result.getData());
        verify(contentBackgroundService).getOptions(eq(null));
    }

    @Test
    public void shouldPassCurrentUserIdToOptionsEndpoint() {
        ContentBackgroundController controller = new ContentBackgroundController();
        IContentBackgroundService contentBackgroundService = mock(IContentBackgroundService.class);
        ReflectionTestUtils.setField(controller, "contentBackgroundService", contentBackgroundService);
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 88L);

        ContentBackgroundOptionsVO optionsVO = new ContentBackgroundOptionsVO();
        when(contentBackgroundService.getOptions(eq(88L))).thenReturn(optionsVO);

        R<ContentBackgroundOptionsVO> result = controller.getOptions();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(optionsVO, result.getData());
        verify(contentBackgroundService).getOptions(eq(88L));
    }

    @Test
    public void shouldRequireLevelFiveForDeleteEndpoint() throws Exception {
        Method method = ContentBackgroundController.class.getMethod("deleteImage", Long.class);

        OshUserLevel oshUserLevel = method.getAnnotation(OshUserLevel.class);
        assertNotNull(oshUserLevel);
        assertEquals(5, oshUserLevel.value());
    }

    @Test
    public void shouldDeleteBackgroundImageWhenUserExists() {
        ContentBackgroundController controller = new ContentBackgroundController();
        IContentBackgroundService contentBackgroundService = mock(IContentBackgroundService.class);
        ReflectionTestUtils.setField(controller, "contentBackgroundService", contentBackgroundService);

        OshUser currentUser = new OshUser();
        currentUser.setId(9L);
        currentUser.setUsername("tester");
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, currentUser);

        R<Void> result = controller.deleteImage(88L);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(contentBackgroundService).deleteImage(9L, "tester", 88L);
    }

    @Test
    public void shouldRejectDeleteWhenUserMissing() {
        ContentBackgroundController controller = new ContentBackgroundController();
        IContentBackgroundService contentBackgroundService = mock(IContentBackgroundService.class);
        ReflectionTestUtils.setField(controller, "contentBackgroundService", contentBackgroundService);

        R<Void> result = controller.deleteImage(88L);

        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
    }
}
