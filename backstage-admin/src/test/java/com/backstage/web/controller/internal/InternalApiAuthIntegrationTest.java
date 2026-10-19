package com.backstage.web.controller.internal;

import com.backstage.RuoYiApplication;
import com.backstage.contract.common.InternalApiConstants;
import com.backstage.system.service.user.UserAssetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 内部接口鉴权链集成测试
 *
 * <p>在完整 Spring 上下文中以 {@code addFilters=true} 启用真实安全过滤链，验证 {@code /internal/**}
 * 的 fail-closed 共享密钥鉴权：无 token / 错误 token 一律 403，正确 token 经 InternalApiAuthFilter
 * 注入 INTERNAL_SERVICE 权限后被 SecurityConfig 的 hasAuthority 放行至控制器。</p>
 *
 * <p>为将断言聚焦在"认证链"而非业务/数据库，积分服务以 {@code @MockBean} 打桩，余额查询返回固定值；
 * KafkaTemplate 同样打桩，避免上下文依赖真实消息中间件（与本模块既有 @SpringBootTest 一致）。</p>
 *
 * <p>运行前置：需与既有 @SpringBootTest 相同的测试环境（Nacos/MySQL/Redis 可达）；
 * 密钥通过 {@code osh.internal.api-token} 测试属性下发。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuoYiApplication.class,
        properties = {"osh.internal.api-token=test-internal-token"})
@AutoConfigureMockMvc(addFilters = true)
public class InternalApiAuthIntegrationTest {

    /**
     * 与 @SpringBootTest properties 下发的密钥保持一致
     */
    private static final String VALID_TOKEN = "test-internal-token";

    /**
     * 内部余额查询端点（GET，便于以最小副作用验证鉴权链）
     */
    private static final String BALANCE_URL = InternalApiConstants.ASSET_BALANCE;

    @Autowired
    private MockMvc mockMvc;

    /**
     * 打桩积分服务，隔离数据库，令鉴权通过后的控制器返回确定结果
     */
    @MockBean
    private UserAssetService userAssetService;

    /**
     * 打桩 Kafka，避免上下文加载依赖真实消息中间件
     */
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void shouldRejectInternalRequestWhenTokenMissing() throws Exception {
        // 【fail-closed】缺失 X-Internal-Token 直接 403，不进入业务
        mockMvc.perform(get(BALANCE_URL).param("userId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldRejectInternalRequestWhenTokenWrong() throws Exception {
        // 【密钥不匹配】错误 token 同样 403
        mockMvc.perform(get(BALANCE_URL)
                        .param("userId", "1")
                        .header(InternalApiConstants.HEADER_INTERNAL_TOKEN, "wrong-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldReachControllerWhenTokenValid() throws Exception {
        // 【鉴权通过】正确 token 授予 INTERNAL_SERVICE，经 hasAuthority 放行至控制器
        when(userAssetService.getPointsBalance(1L)).thenReturn(100L);

        mockMvc.perform(get(BALANCE_URL)
                        .param("userId", "1")
                        .header(InternalApiConstants.HEADER_INTERNAL_TOKEN, VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.points").value(100));
    }
}
