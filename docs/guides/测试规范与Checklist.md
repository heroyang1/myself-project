# 测试规范与 Checklist

> 本文档沉淀支付/拼团/权限核心链路的测试验收标准，基于 2026-08 落地的 7 个测试文件（66 用例）及全部修复经验编写。
> 关联测试类：`PaymentScenarioTest`、`GroupBuyingScenarioTest`、`SecurityBoundaryTest`、`BoundaryValueTest`、`MiddlewareFailoverTest`、`EquivalenceClassTest`、`ConcurrencySafetyTest`。

---

## 一、测试框架与运行约定

| 约定 | 说明 |
|------|------|
| 新测试一律 JUnit 5 | `@ExtendWith(MockitoExtension.class)` + `@MockitoSettings(strictness = Strictness.LENIENT)` |
| 存量 JUnit 4 | 保持不动，依赖 `junit-vintage-engine:5.7.2`（仅 backstage-system 引入） |
| 命名规范 | `should<预期行为>When<前置条件>`，如 `shouldSkipWhenDuplicateCallbackClaimsSameOrder` |
| 运行命令 | `mvn test -pl backstage-system -am -Dtest=XxxTest -Dsurefire.failIfNoSpecifiedTests=false` |
| CI 触发 | `pr-check.yml` 对 7 个核心测试类自动执行（见 `.github/workflows/pr-check.yml`） |

### JUnit 5 测试模板

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class XxxTest {
    @InjectMocks private TargetService service;
    @Mock private DependencyMapper mapper;

    @BeforeEach
    void setUp() {
        // 凡是源码对 int 返回值做 != 1 / <= 0 判断的，必须显式 mock 返回 1
        lenient().when(mapper.markSuccess(anyString(), any(LocalDateTime.class))).thenReturn(1);
    }

    @Test
    void shouldXxxWhenYyy() { /* Given-When-Then */ }
}
```

---

## 二、支付回调测试 Checklist

对应 `PaymentScenarioTest`（P0-A）+ `EquivalenceClassTest`（P1-A）。

- [ ] **双 CAS 闸门**：`completePaidNotify` 中 payment CAS（PENDING→SUCCESS）+ order CAS（PENDING→PAID）均验证成功才推进
- [ ] **幂等短路**：重复回调 → `taskMapper.claim` 返回 0 → 静默返回，不查询订单、不发放权益
- [ ] **并发争用**：N 个线程同时推进同一订单 → 恰好 1 个成功，权益 handler 仅触发 1 次（`shouldGuaranteeSingleSuccessWhenConcurrentCallbacksContend`）
- [ ] **Handler 路由**：`OrderPaidHandlerRegistry` 按商品类型分发，未注册类型抛 `IllegalStateException`（fail-fast）
- [ ] **异常隔离**：`processOrderSafely` 吞异常，不影响支付平台 ACK
- [ ] **markSuccess 守卫**：`markSuccess` 返回 != 1 时抛 `IllegalStateException`，测试必须 mock 返回 1
- [ ] **失败通知**：支付失败 → 订单关闭 + 积分退还

### 常见陷阱（支付）
1. **Mockito int 默认返回 0**：源码 `if (mapper.updateXxx(...) != 1)` 会因 mock 返回 0 而误抛异常，必须显式 `thenReturn(1)`。
2. **并发测试用 `AtomicInteger` 模拟 DB CAS**：`thenAnswer(inv -> counter.incrementAndGet() == 1 ? 1 : 0)`，勿用固定 `thenReturn`。

---

## 三、拼团并发安全测试 Checklist

对应 `GroupBuyingScenarioTest`（P0-B）+ `ConcurrencySafetyTest`（P2-B）。

- [ ] **CAS 原子递增**：`atomicIncrementCurrentNum` / `incrementCurrentNumAtomic` 返回 0 → 满员降级
- [ ] **满员回滚**：用户发起路径满员 → `handleUserInitiatedPayment` 返回 false → `handlePaymentSuccess` 抛 `ServiceException` 触发事务回滚（`shouldThrowAndRollbackWhenGroupAlreadyFull`）
- [ ] **分布式锁**：`tryLock(5, 15, SECONDS)` → 未获锁抛 `ServiceException("系统繁忙")`
- [ ] **守卫条件**：`requireOrderGroupWork` 校验 `work.getOrderId().equals(order.getId())` 且 `work.getUserId().equals(order.getUserId())`，不一致抛异常
- [ ] **来源路由**：`SOURCE_TYPE_SYSTEM` → `atomicIncrementCurrentNum`；`SOURCE_TYPE_INITIATED` → `incrementCurrentNumAtomic`
- [ ] **work 状态校验**：`groupStatus != 0` 抛"参团记录状态异常"
- [ ] **事件发布**：成团 → `publishGroupFormed`；满员 → `publishGroupFull`；支付 → `publishOrderPaid`

### 常见陷阱（拼团）
1. **私有方法不可 Mock**：只能通过控制其依赖（Mapper/Redis/Lock）的返回值间接控制行为。
2. **`verify` 调用次数**：跨方法调用链中同一 Mapper 方法可能被多次调用（如 `selectById` 在 `handlePaymentSuccess` + `handleUserInitiatedPayment` 各调一次 → `times(2)`）。
3. **`@DistributeLock` 分支路由**：`waitTime != -1` 走 `tryLock(waitTime, expireTime, MILLISECONDS)`，Mock 必须匹配（见 `ConcurrencySafetyTest`）。

---

## 四、权限与安全边界测试 Checklist

对应 `SecurityBoundaryTest`（P0-C）。

- [ ] **多角色权限合集**：用户持有多个角色 → 权限取并集
- [ ] **角色实时变更**：权限刷新后角色缩减 → 二次请求权限同步收窄
- [ ] **匿名接口放行**：`@Anonymous` 标注接口不被 `OshAuthenticationFilter` 拦截
- [ ] **token 校验**：无 token / 过期 token → 拒绝并返回 401
- [ ] **SecurityContextHolder 清理**：请求结束后清理上下文，防止线程复用污染

### 常见陷阱（权限）
1. **Redis 会话滑动续期**：Mock `cacheService.getExpire` + `setCacheObject(key, value, timeout)` 时，过期时间用 `eq(500)` 精确匹配，值用 `any()`。
2. **TransmittableThreadLocal 跨线程传播**：子线程**会**继承父线程值（非隔离），`ThreadLocalUtil.get(key, Class)` 为双参数签名。

---

## 五、中间件故障降级测试 Checklist

对应 `MiddlewareFailoverTest`（P1-B）+ `ConcurrencySafetyTest`（P2-B）。

- [ ] **Redis 不可用**：降级读库 / 返回默认值，不抛异常阻断主流程
- [ ] **Kafka 不可用**：`sendPaidMessage` 吞异常，权益仍发放成功
- [ ] **线程上下文隔离/传播**：验证 `ThreadLocalUtil` 在子线程的行为符合 TTL 语义
- [ ] **50 线程 CAS 压力**：并发递增计数器无丢失、无超发

---

## 六、CI/CD 回归策略

| 项 | 配置 |
|----|------|
| 触发条件 | `pr-check.yml`：PR 到 `qa/**`、`release/**`、`main`、`master` |
| 测试范围 | 7 个核心测试类（`-Dtest=...`），`-Dsurefire.failIfNoSpecifiedTests=false` 跳过无匹配模块 |
| 报告归档 | `actions/upload-artifact@v4` 上传 `**/target/surefire-reports/*.txt` |
| 失败处理 | 测试失败即阻断 PR 合并（Build & Verify job 失败） |

### 本地快速验证

```powershell
# 运行全部 7 个核心测试类（Windows PowerShell）
C:\Users\Admin1\tools\apache-maven-3.9.6\bin\mvn.cmd -pl backstage-system,backstage-admin -am `
  test "-Dtest=PaymentScenarioTest,GroupBuyingScenarioTest,SecurityBoundaryTest,BoundaryValueTest,MiddlewareFailoverTest,EquivalenceClassTest,ConcurrencySafetyTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false"
```

---

## 七、变更历史

| 日期 | 变更 | 关联 |
|------|------|------|
| 2026-08-28 | 初版：7 个测试文件落地 + 全部修复经验沉淀 | 本会话 |
