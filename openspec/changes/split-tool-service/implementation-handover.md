# split-tool-service 单体侧增量 · 实施交接文档

> 生成时间：2026-09-30
> 适用范围：`openspec/changes/split-tool-service` 的**阶段三"单体侧最小改造"首个代码增量**
> 目标读者：接续本任务的后续会话 / 开发者
> 一句话概括：已在现有 SB2.5 / Java8 单体上，纯新增 `/internal/**` 服务间契约端点 + 共享契约模块 + fail-closed 内网鉴权 + 资金/认证测试；**未触碰任何现有对外路径与支付发放逻辑**。

---

## 一、背景与决策（不可遗漏的约束）

- 微服务拆分总纲已确认四项决策：绞杀者渐进式、粗粒度 7 服务、OpenFeign + 独立 xxx-api 契约模块、Outbox 可靠消息 + SAGA 补偿（不引入 Seata AT）。
- 本增量经用户拍板确定两项关键选择：
  - **首个代码增量** = 单体侧 `/internal` 契约端点 + 共享契约模块（纯新增、当前基线可构建、是 tool-service 的硬前置）。
  - **服务代码拓扑** = 在当前 osh-backend 仓库内新增独立 Maven 模块（非独立新仓库）。
- **硬前置现实（后续会话务必牢记）**：
  1. Phase 0（网关 `add-gateway-service`、JDK17/SB27 升级 `upgrade-jdk17-springboot27`、可观测性 `add-observability-stack`）目前**只有文档没有代码**。
  2. tool-service 目标基线是 SB3.2 / JDK17，**无法在当前 SB2.5 / Java8 单体上构建**，须待 Phase 0 落地。
  3. `ToolPaidHandler` 由"直接写工具表"改为"发事件"**不能孤立进行**——无消费者时会中断配额发放。本增量**刻意未改**。

---

## 二、本次会话已完成的内容

### 2.1 新增共享契约模块 `backstage-service-contract`（纯 POJO / Java 8 / 零框架依赖）

- `pom.xml`：parent=backstage:3.9.0，maven-compiler-plugin source/target=8，无任何 dependency。
- 已注册到根 `pom.xml`：`<modules>` 加入 `backstage-service-contract`；`dependencyManagement` 登记版本 `${backstage.version}`(=3.9.0)。
- 12 个契约文件：

| 包 | 类 | 说明 |
|---|---|---|
| common | `InternalResult<T>` | {code,msg,data}，与单体 `R<T>` JSON 同构；SUCCESS=200/FAIL=500；ok/fail/isSuccess |
| common | `InternalApiConstants` | 路径/头部/权限单一事实来源（见下） |
| trade | `OrderCheckoutRequest` | 含 userId/productType/productId/productName/**purchaseMode**/originalAmount/discountAmount/payableAmount/couponId/channel/clientIp/usePoints |
| trade | `OrderCheckoutResponse` | 扁平化：needPay/orderNo/paymentNo/payStatus/price/pointsUsed/pointsDeductAmount/remainingPoints/expireTime/closeExpireMinutes |
| trade | `OrderCancelRequest` | orderNo/reason |
| asset | `PointsChangeRequest` | userId/changeAmount(Long)/changeSource(Integer)/remark/operator（扣退共用） |
| asset | `PointsChangeResponse` | afterBalance/beforeBalance/changeAmount/recordId |
| asset | `PointsBalanceResponse` | userId/points；含 `(Long,Long)` 构造 |
| oss | `OssSignRequest` | objectKey/expireMinutes(Integer) |
| oss | `OssSignResponse` | objectKey/url/expireMinutes；含 `(String,String,Integer)` 构造 |
| qa | `ToolQuestionRequest` | userId/toolId/content/tags(List<String>) |
| qa | `ToolQuestionResponse` | questionId(String)；含 `(String)` 构造 |

- `InternalApiConstants` 关键常量：
  - `BASE_PATH=/internal`，`HEADER_INTERNAL_TOKEN=X-Internal-Token`，`HEADER_TRACE_ID=X-Trace-Id`，`HEADER_USER_ID=X-User-Id`（预留未用），`AUTHORITY_INTERNAL_SERVICE=INTERNAL_SERVICE`
  - `ASSET_BASE=/internal/auth-user/asset`（+`/deduct`、`/refund`、`/balance`）
  - `TRADE_BASE=/internal/trade/order`（+`/checkout`、`/cancel`）
  - `OSS_BASE=/internal/platform/oss`（+`/sign`）
  - `QA_BASE=/internal/course/qa`（+`/tool-question`）

### 2.2 资金服务 `UserAssetService` / `UserAssetServiceImpl`（backstage-system）

- 接口：`deductPoints`、`refundPoints`、`getPointsBalance`。
- 实现要点（资金敏感，评审重点）：
  - 走 `OshUserAssetMapper.updatePointsAtomic(userId, delta, requireEnough)` 原子条件更新（SQL `WHERE ... points+delta>=0` 防负余额），**替代有名无实的 `selectByUserIdForUpdate` 读改写**。
  - 账本一致性：先原子 UPDATE（持行锁）→ 同事务回读 after → `before = after - delta`。
  - 失败（rows≠1）抛 `ServiceException` 回滚，**不写账本**。
  - `createBy/updateBy` 显式置 userId（内部调用无登录上下文，避免自动填充得 null；`strictInsertFill` 只填 null 不覆盖，故显式值保留）。
  - 缓存刷新：`registerAssetCacheRefreshAfterCommit` —— 有事务同步器则 afterCommit 刷 `LoginUser:{id}` 的 `asset.points`，无事务则立即刷（镜像 `ToolPurchaseServiceImpl` 行为）。

### 2.3 四个 `/internal` 控制器（backstage-system，纯委托现有服务，返回 `InternalResult<T>`）

| 控制器 | 路径 | 委托 |
|---|---|---|
| `InternalAssetController` | `/internal/auth-user/asset` deduct/refund/balance | `UserAssetService` |
| `InternalTradeController` | `/internal/trade/order` checkout/cancel | `OrderCheckoutService` / `OrderService.cancelPaymentByOrderNo` |
| `InternalOssController` | `/internal/platform/oss` sign | `OssService.getLimitedUrl` |
| `InternalQaController` | `/internal/course/qa` tool-question | `IOshQAQuestionService.addToolQuestion`（透传 R 结果） |

### 2.4 fail-closed 内网鉴权（本次会话核心新增）

- 新增 `backstage-framework/.../security/filter/InternalApiAuthFilter.java`：
  - `@Component extends OncePerRequestFilter`；`@Value("${osh.internal.api-token:}")`（**空默认=fail-closed**）。
  - AntPathMatcher 匹配 `/internal/**`；非内部请求直接放行。
  - 内部请求：密钥未配置或 token 不匹配 → **403** `{"code":403,"msg":"内部接口鉴权失败"}`；**常量时间比对**防时序侧信道；通过则注入 `UsernamePasswordAuthenticationToken("internal-service", null, [INTERNAL_SERVICE])`，`finally` 清 SecurityContext。
- 改 `OshAuthenticationFilter.isWhiteList()`：加入 `/internal/**` 放行（内部请求不带用户 JWT；白名单放行分支**不触碰 SecurityContext**，保留 INTERNAL_SERVICE 权限）。
- 改 `SecurityConfig`：autowire `internalApiAuthFilter`；`.antMatchers("/internal/**").hasAuthority(INTERNAL_SERVICE)` 置于 `anyRequest().authenticated()` 前；`addFilterBefore(internalApiAuthFilter, OshAuthenticationFilter.class)`。
- 改 `backstage-framework/pom.xml`：显式新增 `backstage-service-contract` 依赖。
- **无需改 application.yml**：`@Value` 空默认即 fail-closed，密钥上线时由 Nacos 下发 `osh.internal.api-token`。

### 2.5 测试（均 JUnit 5，遵循项目"新测试一律 JUnit 5"决策）

- `backstage-system/src/test/.../service/user/UserAssetServiceImplTest.java`（9 用例，JUnit4+Mockito+ReflectionTestUtils 风格的 JUnit5 版）：
  - 扣减账本精确一致、余额不足**不落账**（`verify never insert`）、退还为收入且 requireEnough=false、退还目标缺失、非法金额/空参拦截、余额查询兜底/null userId、**缓存刷新有事务(afterCommit 延迟)/无事务(立即)双路径**。
- `backstage-admin/src/test/.../controller/internal/InternalApiAuthIntegrationTest.java`（3 用例，`@SpringBootTest(classes=RuoYiApplication.class, properties={"osh.internal.api-token=test-internal-token"}) + @AutoConfigureMockMvc(addFilters=true)`，`@MockBean UserAssetService`+`KafkaTemplate`）：
  - 无 token→403、错误 token→403、正确 token→200 且 `$.data.points=100`。

### 2.6 文档

- `openspec/changes/split-tool-service/tasks.md`：阶段二 auth-user/trade/course-api 勾选 [x]、platform-api 标 [~]（仅 sign 未 upload）；阶段三首项勾 [x]，并追加进度说明与测试记录。

---

## 三、验证状态（做到哪、卡在哪）

| 验证项 | 方式 | 结果 |
|---|---|---|
| 契约模块编译 | `javac --release 8` | ✅ exit 0，12 class |
| `InternalApiAuthFilter` 编译 | javac + .m2 真实 jar（Spring Security 5.7.12 / Spring 5.3.25 / servlet 4.0.1 / 契约 class） | ✅ exit 0（证明 `singletonList` 等报错是索引级联误报） |
| 两个测试文件 | GetProblems | ✅ 无 error，仅 2 个与现有测试同款 unchecked 警告 |
| **全量 Maven 构建** | — | ❌ **无法执行**：本机无 `mvn`/`mvnw`，`MAVEN_HOME`/`M2_HOME` 空，未定位到 `idea64.exe` |
| **运行测试** | — | ❌ 同上，无 Maven 无法运行 |

---

## 四、剩余未完成内容

### 4.1 立即待办（本增量的收尾，需用户在本地 IDE 执行）
1. IntelliJ Maven 面板 **Reload All Maven Projects**（消除所有 `com.backstage.contract cannot be resolved` 报红——纯索引问题，非代码错误）。
2. 全量构建：`mvn clean install -DskipTests -pl '!backstage-flink'`。
3. 跑测试：`mvn test -pl backstage-system -Dtest=UserAssetServiceImplTest`；`mvn test -pl backstage-admin -Dtest=InternalApiAuthIntegrationTest`（后者需 Nacos/MySQL/Redis 测试环境可达）。
4. 人工评审资金端点 + 认证链改动（见第五节风险），通过后合入。

### 4.2 本增量范围内尚未做（可选补齐）
- `platform-api` 的 **oss upload** 端点（当前仅 getLimitedUrl/sign）。
- `requestId` **幂等**：`PointsChangeRequest` 未含幂等键，deduct/refund 尚无去重；tool-service 侧接入 SAGA 时需补。
- `osh.internal.api-token` 的 **Nacos 配置项**下发与密钥管理（osh-secret）。
- 网络层限制 `/internal/**` **仅内网可达**（依赖 Phase 0 网关）。

### 4.3 后续阶段（依赖 Phase 0，未开始）
- 阶段三剩余：`ToolPaidHandler` 改发事件 + 发放预案开关 + 锁 key 常量抽到共享模块。
- 阶段二剩余：`tool-api` 对外契约、入向事件 `osh.trade.order.paid.v1` 信封、出向事件契约、API 契约清单冻结。
- 阶段四~七：tool-service（SB3.2/JDK17）实现、数据与配置、测试与验证、灰度发布。

---

## 五、存在的问题与风险（评审发现）

| # | 级别 | 问题 | 说明 / 建议 |
|---|---|---|---|
| 1 | 高·部署 | `/internal/**` 暴露面 | 密钥空时 fail-closed(403) 安全；**配置密钥后**若单体公网直达则仅靠共享密钥防护。上线务必在网关/安全组限制 `/internal/**` 仅内网可达（Phase 0 网关未建成前是主要残留风险）。 |
| 2 | 中 | userId 来自请求体、非可信校验 | 服务端不校验调用方是否有权操作该 userId，属"信任内网调用方"设计边界，依赖 #1 网络隔离 + 密钥保密。 |
| 3 | 中 | `selectByUserIdForUpdate` 名不副实 | 实为普通 `SELECT`（无 `FOR UPDATE`，未过滤 `delete_flag`）。当前**无 bug**（正确性由前一条 UPDATE 行锁兜底；delete_flag=1 时 UPDATE 影响 0 行即抛异常，走不到回读）。建议后续另立小改动澄清方法名/语义。 |
| 4 | 中 | 内部请求若同时带有效 OSH token | `OshAuthenticationFilter` 会覆盖 INTERNAL_SERVICE → `hasAuthority` 失败 → 403。属**安全失败**（非漏洞）；实践中 tool-service 不带用户 token，影响低。可选加固：oshAuth 对 `/internal/**` 直接放行不解析 token。 |
| 5 | 低 | `X-User-Id` 常量已定义未使用；403 未走统一响应封装 | 预留/可接受，无害。 |
| 6 | 提示 | `@Component` 过滤器双重注册 | `InternalApiAuthFilter` 作为 `@Component` 会被 Boot 自动注册为 servlet 过滤器，同时又在 security chain 中；`OncePerRequestFilter` 的 alreadyFiltered 保证逻辑只执行一次。与既有 `OshAuthenticationFilter` 同款模式，功能正确，仅需知晓。 |

---

## 六、后续会话执行指引（踩坑与约定）

1. **环境**：Windows PowerShell（`&&` 不支持，用 `;`）；无 Maven CLI；java=Amazon Corretto JDK 21（可 `javac --release 8` 验证零依赖模块）。
2. **契约模块报红**：任何 `com.backstage.contract cannot be resolved` 都是 **IDE 未导入新模块的索引问题**，非代码错误——不要为消红改正确代码，也不要用 .m2 hack；正解是 Maven re-import。
3. **测试规范**：**新测试一律 JUnit 5**（`org.junit.jupiter.api.*`）。`backstage-admin` 不含 junit-vintage，JUnit4 测试会**静默不执行**；`backstage-system` 保留 vintage（存量 JUnit4 可跑）。范式：Service 层用 `ReflectionTestUtils.setField` 注入 mock；集成测试用 `@ExtendWith(SpringExtension.class) @SpringBootTest(classes=RuoYiApplication.class)`。
4. **资金/账本改动**：必须覆盖"失败终态不落账"（`verify(..., never())`）与"有事务/无事务"双路径；断言面向业务语义（before/after、changeType）。
5. **依赖链**：`admin → framework → system → contract`（均 compile），契约类在 admin 测试中可传递解析。
6. **不要孤立改 `ToolPaidHandler`**：见第一节硬前置现实 #3。
7. **SDD/OpenSpec**：中/大改动前读 `AGENTS.md`、`docs/ai/README.md`、`openspec/project.md` 与本 change 的 proposal/design/tasks/spec；改 API 契约/权限/数据模型/中间件/部署/存储须同步四件套。

---

## 七、本次会话改动文件清单（速查）

**新增**
- `backstage-service-contract/`（pom + 12 契约类）※前序会话建，本会话沿用
- `backstage-system/.../service/user/UserAssetService.java`、`.../service/impl/user/UserAssetServiceImpl.java`※前序
- `backstage-system/.../controller/internal/InternalAssetController.java` 等 4 个※前序
- `backstage-framework/.../security/filter/InternalApiAuthFilter.java` ★本会话
- `backstage-system/src/test/.../service/user/UserAssetServiceImplTest.java` ★本会话
- `backstage-admin/src/test/.../controller/internal/InternalApiAuthIntegrationTest.java` ★本会话

**修改**
- 根 `pom.xml`（modules + dependencyManagement）※前序
- `backstage-system/pom.xml`（契约依赖）※前序
- `backstage-framework/pom.xml`（契约依赖）★本会话
- `backstage-framework/.../security/filter/OshAuthenticationFilter.java`（白名单 `/internal/**`）★本会话
- `backstage-framework/.../config/SecurityConfig.java`（注册过滤器 + hasAuthority）★本会话
- `backstage-service-contract/.../trade/OrderCheckoutRequest.java`（补 purchaseMode）※前序
- `openspec/changes/split-tool-service/tasks.md`（进度 + 测试记录）★本会话
