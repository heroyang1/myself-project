---
title: 编码规范（Coding Standards，AI 新增代码必读必守）
path: docs/ai/conventions/coding-standards.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-10
last_updated: 2026-10-10
openspec: none          # 关联的 change-id；无则 none
---

# 编码规范（Coding Standards）

> **定位**：本文所有规则与示例均**从现有真实代码提炼**（每段示例标注来源文件），是 AI/人工新增代码**必须遵守**的单一事实源。与现有代码冲突时以本文为准（本文选的是符合规范的模范代码）；本文未覆盖的场景，模仿同模块既有代码。
> **强制方式**如实标注：`CI 阻断` = ArchUnit / code-quality-guard / 测试强制；`评审拦截` = Code Review 必查。
> 相关：分层职责 → [boundaries.md](../../architecture/boundaries.md)；落位与命名 → [package-structure.md](../../architecture/package-structure.md)；日志细则 → [logging.md](./logging.md)。

## 1. 命名规范

### 1.1 类命名

| 类型 | 规则 | 真实示例（来源） |
|---|---|---|
| 业务实体 | `Osh<PascalCase>` | `OshCourse`（domain/course/OshCourse.java） |
| 系统实体 | `Sys<PascalCase>` | `SysUser`（common/core/domain/entity） |
| Service 接口 | `I<实体或域>Service` | `IOshCourseService` |
| Service 实现 | `<接口去 I>ServiceImpl`，放 `service/impl/<模块>/` | `OshCourseServiceImpl` |
| Controller | `<实体或域>Controller` | `OshCourseController`、`SysUserController` |
| Mapper | `<实体>Mapper` | `OshCourseMapper` |
| 请求对象 | `<域><动作>Request` | `CourseCreateRequest`、`CourseSearchRequest` |
| 视图对象 | `<域><用途>Vo` | `CourseSearchLoginVo`、`OshCourseDetailVo` |
| Kafka 消息 | `<域><事件>Message` | `CourseIndexDeleteMessage` |
| 配置属性类 | `<域>Properties` | `SearchEsProperties`（system/config/properties） |
| 枚举 | `<域><语义>Enum` | `OshCourseStatusEnum`、`BusinessType` |

### 1.2 方法命名

- **查询** `selectXxx` / `pageQueryXxx`（分页）/ `getXxx`（C 端详情类）；**新增** `insertXxx` / `createXxx`；**修改** `updateXxx`；**删除** `deleteXxx` / `removeXxx`（收藏等关系用 remove）。来源：`OshCourseMapper.selectCourseById / pageQuerySearchCourse`、`IOshCourseService.createCourse / updateCourse / deleteCoursesByIds`。
- **跨模块调用的方法名必须含业务前缀**（`insertGroupOrder` 而非 `insertOrder`），防复制误调（评审拦截）。
- 布尔变量/方法用 `isXxx`/`canXxx`/`hasXxx`：`canViewUnpublishedDetail(...)`（OshCourseController）。
- 私有构建/校验方法 `buildXxxForYyy` / `ensureXxx`：`buildCourseForCreate`、`ensureCourseExists`（OshCourseServiceImpl）。

### 1.3 字段与常量

- 数据库列 `snake_case` → Java 字段 `camelCase`（`create_time` → `createTime`）；金额一律 `BigDecimal`（`private BigDecimal price;` OshCourse），禁 double/float。
- 常量全大写下划线，集中放 `constant` 包或类内 `static final`：`private static final int FOUNDER_ROLE_LEVEL = 6;`（OshCourseController——魔法数字必须提取成具名常量，评审拦截）。
- 集合变量用复数：`teacherIds`、`courseIds`。

## 2. Controller 规范

### 2.1 基本结构（B 端模板，来源 SysUserController）

```java
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {

    @Autowired
    private ISysUserService userService;

    /** 分页列表：固定三段式 startPage → Service → getDataTable */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUser user) {
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    /** 单条查询 */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public AjaxResult getInfo(@PathVariable Long userId) {
        return success(userService.selectUserById(userId));
    }

    /** 新增：@Log 记操作审计，toAjax(rows) 包装结果 */
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user) {
        return toAjax(userService.insertUser(user));
    }
}
```

### 2.2 基本结构（C 端模板，来源 OshCourseController）

```java
@Api(tags = "课程管理")
@RestController
@RequestMapping("/pc/course")
public class OshCourseController extends BaseController {

    @ApiOperation("课程详情")
    @GetMapping("/detail/{id}")
    @OshUserEvent(module = "课程模块", actionType = "浏览",
                  resourceType = ResourceType.COURSE_TYPE, description = "浏览课程详情", recordAnonymous = true)
    @Anonymous
    public R<OshCourseDetailVo> getCourseDetail(@OshResourceId @NotNull @PathVariable("id") Long id) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        OshCourseDetailVo vo = oshCourseService.getCourseDetail(id, userId, false);
        if (vo == null) {
            return R.fail("课程不存在");
        }
        return R.ok(vo);
    }

    /** 写操作：等级门禁 + 分布式锁防并发重复提交 */
    @ApiOperation("批量隐藏课程（下架，创始人专属）")
    @PostMapping("/hide")
    @OshUserEvent(module = "课程模块", actionType = "修改", resourceType = ResourceType.COURSE_TYPE, description = "隐藏课程")
    @OshUserLevel(value = 6)
    public R<String> hideCourses(@RequestBody CourseDeleteRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) return R.fail("请先登录");
        oshCourseService.hideCoursesByIds(request.getIds(), currentOshUser);
        return R.ok("隐藏成功");
    }
}
```

### 2.3 Controller 规则清单

1. 注解顺序（从上到下）：`@ApiOperation` → 权限（`@PreAuthorize` B 端 / `@OshUserLevel`/`@Anonymous` C 端）→ `@XxxMapping` → 业务注解（`@Log`/`@DistributeLock`/`@RepeatSubmit`/`@OshUserEvent`）。
2. HTTP 方法语义：GET=查询、POST=新增/导出/复杂查询（带 body 的搜索用 POST）、PUT=修改、DELETE=删除。
3. **不写业务逻辑**：编排、判断、事务全在 Service（评审拦截；复杂兼容逻辑散落 Controller 是明令禁止项，见 05-code-style）。
4. **不做 try-catch 兜底**：业务失败由 Service 抛 `ServiceException`，全局处理器统一转响应（细则见 §7）。
5. `startPage()` 必须**紧贴**目标查询语句，中间不得插入其他查询/逻辑（否则污染分页，评审拦截）。
6. 登录用户只能服务端取：B 端 `SecurityUtils.getUserId()`、C 端 `UserContextUtil.getCurrentUserId()`，**禁信任前端传 userId**（越权红线，评审拦截）。
7. C 端接口禁混用 B 端 `@PreAuthorize` 权限码（无码必 403，评审拦截 + 集成测试）。
8. 返回类型：B 端 `AjaxResult`/`TableDataInfo`，C 端 `R<T>`；同一 Controller 内保持一致（见 §6）。
9. 入参校验用 `@Validated @RequestBody <XxxRequest>`；简单对象查询参数直接绑定实体。

## 3. Service 规范

### 3.1 基本结构（来源 IOshCourseService / OshCourseServiceImpl）

```java
// 接口：service/course/IOshCourseService.java
public interface IOshCourseService {
    /**
     * 新增课程
     * @param request  创建入参
     * @param operator 操作人
     * @return 新课程 ID，失败返回 null
     */
    Long createCourse(CourseCreateRequest request, OshUser operator);
}

// 实现：service/impl/course/OshCourseServiceImpl.java
@Service
public class OshCourseServiceImpl implements IOshCourseService {

    private static final Logger log = LoggerFactory.getLogger(OshCourseServiceImpl.class);

    @Autowired
    private OshCourseMapper oshCourseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourse(CourseCreateRequest request, OshUser operator) {
        OshCourse course = buildCourseForCreate(request, operator);   // 构建拆私有方法
        ensureCourseNo(course);                                        // 前置校验独立成方法
        oshCourseMapper.insertCourse(course);
        return course.getId();
    }
}
```

### 3.2 Service 规则清单

1. 接口放 `service/<模块>/`，实现放 `service/impl/<模块>/`（CI 阻断：ArchUnit 分层）。
2. 写操作方法必须 `@Transactional(rollbackFor = Exception.class)`——**默认 rollbackFor 只回滚 RuntimeException，必须显式写**（评审拦截）。
3. 事务边界最小化：**事务方法内禁止 RPC/HTTP/文件等慢外部调用**（长事务红线，评审拦截）；"核心同步 + 非核心异步"——通知/统计等放事务外或走事件（参考课程 ES 同步走 Kafka `CourseIndexKafkaProducer`）。
4. 业务失败 `throw new ServiceException("用户可读提示")`，不返回 null 让 Controller 猜（见 §7）。
5. 禁止直接操作 `HttpServletRequest/Response`、禁止返回 `AjaxResult` 等 HTTP 响应对象（评审拦截）。
6. 复杂条件提取具名私有方法（`canXxx`/`ensureXxx`/`buildXxx`），方法超 80 行拆分（05-code-style）。
7. 跨模块调用只走对方 `I<Domain>Service` 接口，禁注入他域 Mapper（[module-dependency-rules.md §4.2](../../architecture/module-dependency-rules.md)，评审拦截）。

## 4. Mapper 规范

### 4.1 接口定义（来源 OshCourseMapper）

```java
@Mapper
public interface OshCourseMapper {

    /** 简单查询：一个主键一个对象 */
    OshCourse selectCourseById(Long id);

    /** 多参数必须 @Param 具名，禁裸参数顺序依赖 */
    List<Long> selectUserBoughtCourseIds(@Param("userId") Long userId,
                                         @Param("courseIds") List<Long> courseIds);

    /** 分页查询：返回 List，PageHelper 在外层 startPage() 控制，不在 Mapper 传 offset/limit */
    List<CourseSearchLoginVo> pageQuerySearchCourse(@Param("request") CourseSearchRequest request,
                                                    @Param("userId") Long userId);
}
```

### 4.2 Mapper 规则

1. 简单 CRUD 用 MyBatis-Plus `BaseMapper<T>` / 注解 SQL；**复杂查询（多表、子查询、动态条件）必须走 XML**（现状主流，评审拦截）。
2. 接口不做业务判断——纯数据访问（CI 阻断：ArchUnit 分层）。
3. 方法命名 `selectXxx/insertXxx/updateXxx/deleteXxx/pageQueryXxx`，与 XML `<select id>` 一致。
4. 逻辑删除条件 `delete_flag = 0` 必须显式写在 WHERE/JOIN 里（本表与 JOIN 的每张带 delete_flag 的表都要），漏写会查出已删数据（评审拦截；全局逻辑删除拦截器有正则误匹配前科，不能依赖）。

### 4.3 XML 规范及示例（来源 OshCourseMapper.xml）

```xml
<mapper namespace="com.backstage.system.mapper.course.OshCourseMapper">

    <!-- 列清单抽成 <sql> 片段复用，避免多处手写列名漂移 -->
    <sql id="searchCourseColumns">
        c.id, c.title, c.cover, c.price, c.t_price, c.status, c.delete_flag,
        c.create_by, c.create_time, c.update_by, c.update_time,
        <!-- 关联计数用相关子查询聚合，避免 JOIN 放大行数再 GROUP BY -->
        (select count(distinct cb.user_id) from osh_course_buy cb
          where cb.course_id = c.id and cb.pay_status = 'paid'
            and cb.delete_flag = 0) as sales_count
    </sql>

    <select id="pageQuerySearchCourse" resultType="com.backstage.system.domain.course.vo.CourseSearchLoginVo">
        SELECT <include refid="searchCourseColumns"/>
        FROM osh_course c
        WHERE c.delete_flag = 0
          AND c.status = 4
        <if test="request.keyword != null and request.keyword != ''">
            AND c.title LIKE CONCAT('%', #{request.keyword}, '%')
        </if>
        ORDER BY c.create_time DESC
    </select>
</mapper>
```

- 参数一律 `#{}`；`${}` 唯一白名单例外 `${params.dataScope}`（数据权限注入，CI 的 code-quality-guard + 评审双查）。
- 禁 `SELECT *`：显式列清单（模板列用 `<sql>` + `<include>`）。
- 动态条件用 `<if test>`/`<where>`/`<foreach>`，禁字符串拼接条件。
- `resultType` 直出 VO 或 `resultMap` 映射实体，二选一按同模块现状跟随。

## 5. 实体类规范

### 5.1 示例（来源 OshCourse）

```java
@ApiModel(description = "课程信息")
public class OshCourse {                          // 普通 POJO + getter/setter（RuoYi 生成器风格）

    @ApiModelProperty("课程 ID")
    private Long id;

    @Excel(name = "当前价格")
    @ApiModelProperty("当前价格")
    private BigDecimal price;                     // 金额 = BigDecimal 铁律

    @ApiModelProperty("课程类型：media-视频课，live-直播课，text-图文课")
    private String type;                          // 枚举/状态字段注释必须标取值含义

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;                      // 时间字段显式格式
}
```

### 5.2 实体规则

1. 业务表实体 `@TableName("osh_xxx")`（MyBatis-Plus 表）继承 `BaseEntity`/`OSHBaseEntity` 复用审计字段；纯查询对象可如 `OshCourse` 自带字段（跟随同模块现状）。
2. **每个字段必须有中文注释；状态/类型字段必须标注取值含义**（`0-草稿 1-已上架…`），评审拦截。
3. Lombok：本项目主流是生成器风格显式 getter/setter；**新文件若用 Lombok 仅允许 `@Data`/`@Slf4j`，禁 `@Builder` 混入生成器实体**（评审拦截，保持生成器可重跑）。
4. Entity 禁止直接作为接口出入参泄露表结构；对外用 VO/Request 投影（`delete_flag`/`create_by` 等审计字段不得出现在 C 端响应，评审拦截）。
5. DDL↔实体↔VO↔ALTER 四层一致由 `scripts/code-quality-guard.sh` **CI 阻断**（列类型 Java↔SQL 匹配：`decimal→BigDecimal`、`bigint→Long`、`datetime→Date`）。

## 6. 响应封装规范（三种响应体禁混用）

| 场景 | 类型 | 结构 | 用法 |
|---|---|---|---|
| B 端单对象/操作 | `AjaxResult` | `{code,msg,data}` | `success(data)` / `error(msg)` / `toAjax(rows)`（BaseController） |
| B 端分页列表 | `TableDataInfo` | `{code,rows,total}` | `startPage(); …; return getDataTable(list);` |
| C 端全部 | `R<T>` | `{code,msg,data}` | `R.ok(data)` / `R.fail("用户可读提示")` |
| C 端分页 | `R<PageResponse<T>>` | PageResponse.of(list,total,pageNum,pageSize) | 或 PageInfo 包装（跟随同模块现状） |

```java
// B 端（来源 SysUserController）
@Log(title = "用户管理", businessType = BusinessType.EXPORT)
@PostMapping("/export")
public void export(HttpServletResponse response, SysUser user) {   // 导出例外：void + response 流
    List<SysUser> list = userService.selectUserList(user);
    new ExcelUtil<>(SysUser.class).exportExcel(response, list, "用户数据");
}
```

- 业务结果看 body `code`，**HTTP 状态恒 200**（401/403 由认证/权限层返回）。
- 禁 `Map<String,Object>`/`HashMap` 直出接口结果，必须定义 VO（评审拦截）。
- 导出/下载例外：`void` + `response.getOutputStream()`。
- 细则与 Swagger 查看方式 → [api-spec.md](../../reference/api-spec.md)。

## 7. 异常处理规范

```java
// ✅ Service 抛业务异常（来源：error-codes.md 约定 + 现网代码主流）
if (!Objects.equals(existingCourse.getStatus(), OshCourseStatusEnum.PENDING_AUDIT.getCode())) {
    throw new ServiceException("课程不在待审核状态，无法审核");
}

// ✅ 需要携带错误码时
throw new ServiceException("库存不足", 409);

// ❌ 禁止：Controller 逐方法 try-catch 自拼错误码
// ❌ 禁止：e.printStackTrace()          —— CI 阻断（ArchUnit 零违规规则）
// ❌ 禁止：吞异常空 catch {} / catch 后只打日志不处理不抛出
// ❌ 禁止：把堆栈/SQL 片段返回给前端
```

- 兜底分工：B 端 `GlobalExceptionHandler`（framework）；C 端 `PcExceptionHandler`（admin，`@Order(1)`，`ServiceException`→601 WARN 不打堆栈）。异常→码映射表见 [error-codes.md](../../reference/error-codes.md)。
- 记日志用 `log.error("msg, orderId={}", orderId, e)`（带业务 ID + 异常对象，见 §10）。
- 参数非法在 Service 前置校验抛 `IllegalArgumentException`/`ServiceException` 均可被兜底器捕获，但**面向用户的提示一律 `ServiceException`**（评审拦截）。

## 8. 注解使用规范

```java
// 操作日志（B 端写操作必带，来源 SysUserController）
@Log(title = "用户管理", businessType = BusinessType.INSERT)      // 审计落库，含请求参数与结果

// 分布式锁（防并发重复写，来源 OshCourseController.save）
@DistributeLock(scene = "resource", key = "operation",
                expireTime = 10000, waitTime = 3000, releaseImmediately = true)
// 注意：注解参数决定 lock/tryLock 分支（waitTime=0 走 tryLock 快速失败，如 audit 接口 waitTime=0）

// 防重复提交 / 限流
@RepeatSubmit                       // 写接口默认 5s（interval=5000）内同参拒绝
@RateLimiter(time = 60, count = 100) // 公开查询接口按 IP/用户限流（默认 60s/100 次）

// C 端行为埋点 + 资源解析（来源 OshCourseController）
@OshUserEvent(module = "课程模块", actionType = "浏览",
              resourceType = ResourceType.COURSE_TYPE, description = "浏览课程详情", recordAnonymous = true)
public R<OshCourseDetailVo> getCourseDetail(@OshResourceId @PathVariable("id") Long id) { … }
// @OshResourceId 标记的参数会被解析为事件资源 ID

// C 端权限
@OshUserLevel(value = 6)            // 等级门禁：level>=6 才可调
@Anonymous                          // 免登录白名单（过滤器直接放行，不解析 token）

// 数据权限（B 端列表按部门/角色过滤，来源 @DataScope 用法）
@DataScope(deptAlias = "d", userAlias = "u")   // AOP 拼接 ${params.dataScope} 进 SQL
```

全部 14 个注解清单 → [backend.md 注解速查](./backend.md)；权限注解选型规则 → §2.3 规则 1/7。

## 9. SQL 规范（DDL + 编写）

**DDL（表设计，来源 database.md + 现网表）**：
- 表名 `osh_`/`sys_` 前缀 snake_case；必备字段 `create_by/create_time/update_by/update_time/remark` + 逻辑删除 `delete_flag tinyint DEFAULT 0`。
- 列必须 `COMMENT`；状态列注释标全枚举含义；金额 `decimal(10,2)`；外键关系用索引列（`KEY idx_course_id (course_id)`），禁物理外键。
- DDL 入 `sql/` 目录，文件名 `yyyymmdd_<主题>.sql`；**改表必须同步实体/VO（CI 阻断：code-quality-guard）**。

**查询编写**：
```sql
-- ✅ 覆盖逻辑删除：主表与 JOIN 表都带 delete_flag 条件
SELECT c.id, c.title
FROM osh_course c
LEFT JOIN osh_course_tag_rel r ON r.course_id = c.id AND r.delete_flag = 0
WHERE c.delete_flag = 0 AND c.status = 4;

-- ✅ 列表关联统计用子查询聚合（防 JOIN 行数放大），或批量 IN 查询后内存组装（防 N+1）
-- ❌ 循环里逐条查：for (id) { mapper.selectById(id); }
-- ✅ 正确姿势：先收集 ID 集合，一次 selectBatchIds / IN 查询
```
- 参数化 `#{}` 铁律（Java 侧），SQL 拼接仅 `<if>`/`<foreach>` 动态标签。
- 分页总数用 PageHelper（`startPage()`），禁手写 `LIMIT offset,n` 深分页（大偏移性能陷阱，需要时走游标/ES，见 [ES_Deep_Pagination_Analysis](../../analysis/ES_Deep_Pagination_Analysis.md)）。
- 慢查询意识：WHERE 条件列必须有索引（EXPLAIN 自查），新表高频查询列在建表 DDL 里就建好索引。

## 10. 日志规范

铁律与示例统一收敛在 **[logging.md](./logging.md)**（唯一权威源，避免双源）：
- `slf4j` `Logger log = LoggerFactory.getLogger(X.class)` 或 `@Slf4j`；禁 `System.out.println`（ArchUnit FreezingArchRule 拦新增）；禁 `e.printStackTrace()`（CI 阻断）。
- 日志必须带业务 ID（userId/orderId/courseId），占位符名与变量语义一致；敏感信息（密码/token/手机号全量）禁入日志。
- 级别：业务可预期失败 WARN，系统异常 ERROR（`log.error("msg, id={}", id, e)`），调试 INFO 起步。

## 11. 强制方式总表（本文规则如何被执行）

| 机制 | 覆盖规则 | 强度 |
|---|---|---|
| ArchUnit `LayeredArchitectureTest` | 分层方向、common 不反向、Controller 不直用 Mapper | CI 阻断 |
| ArchUnit `CodingConventionArchitectureTest` | printStackTrace 零违规；System.out/裸 HTTP 冻结基线只拦新增 | CI 阻断 |
| `code-quality-guard.sh` | DDL↔实体↔VO 四层一致、`{}` 参数化抽查 | CI 阻断 |
| `verify-ai-process.sh` | 行为/接口/权限/数据变更须有 OpenSpec 四件套；保护域 | CI 阻断 |
| `PageHelper` 语义 + 集成测试 | startPage 紧贴查询 | 评审拦截 + 测试 |
| Code Review（[07-review-rubric.md](../07-review-rubric.md)） | 本文其余全部标注"评审拦截"的条目 | 人工 |

> AI 生成代码自检顺序：先过 §11 机器门禁（本地 `mvn test -pl backstage-admin -am` 跑 ArchUnit，DDL 变更另跑 `scripts/code-quality-guard.sh`），再对照 §1–§10 清单逐条自查。
