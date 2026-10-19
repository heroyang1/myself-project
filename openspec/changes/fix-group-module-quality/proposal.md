# Proposal: fix-group-module-quality

## 背景

拼团模块（服务器拼团）在代码审查中发现 P0-P3 共 21 项代码漏洞，涵盖安全漏洞（匿名接口越权、缺少分布式锁）、并发风险（超卖、重复提交）、代码质量（魔法值 54+ 处、Map 返回值、超大 Service 类）、架构优化（价格计算不统一、时间戳不一致）四大方向。同时依据《OSH Backend 项目开发规范》进行合规性审计，发现写操作缺少 @Log、Swagger 文档缺失、常量未集中管理等问题。

此外，支付回调链路中发现一个严重 Bug：`handleUserInitiatedPayment` 满员结束时传入 `GroupStatusEnum.ENDED.getCode()`（值=3），但 `osh_group_user_initiated.group_status` 只有 0/1/2 三个有效值，导致状态无法识别。

## 目标

- 消除拼团模块所有 P0-P3 安全和并发风险
- 统一状态码、订单状态、SSH 默认值等魔法值为枚举和常量
- Map 返回值替换为强类型 VO，提升类型安全
- 修复 initiated 状态越界严重 Bug
- 统一日志格式为结构化前缀
- 补全 @Log、@Api、@ApiOperation 等开发规范注解
- 价格计算统一走 GroupPriceCalculator 工具类
- 事务内时间戳统一，保证一致性

## 非目标

- 本次不拆分 OshGroupServerServiceImpl（~1970 行）为多个 Service
- 本次不改变数据库表结构或 SQL 迁移
- 本次不创建 35xx 段错误码枚举
- 本次不改变课程/专栏/订单创建接口的匿名访问策略（保持 @Anonymous）
- 本次不改变前端接口契约（JSON 结构完全兼容）

## 用户影响

- C 端用户：join/create/pay 快速连点会收到"操作正在处理中，请勿重复提交"友好提示
- C 端用户：发起拼团接口权限校验方式变更（从 userLevel 改为权限注解），若角色未配置 `group:activity:initiable` 权限会返回 403
- C 端用户：course/read、column/read、POST /pc/order/group 保持 @Anonymous 免登录，无影响
- 管理员：手动添加用户操作现在会记录 @Log 操作日志
- 前端：所有 VO 替换 Map 的 JSON 字段名完全一致，前端无需修改

## 风险

- 兼容性风险：低。VO 字段与原 Map key 完全一致，JSON 序列化结果不变。
- 数据风险：低。无表结构变更，仅修复 initiated 状态越界写入错误值的问题。
- 性能风险：低。枚举转换和常量引用为编译期静态解析，无运行时开销。分布式锁增加一次 Redis 调用（~1ms）。
- 安全风险：降低。三个高频写接口增加分布式锁防重复提交，支付接口增加订单归属校验。
- 回滚风险：低。所有变更均为代码层面修改，回退代码即可。

## 验收标准

- [x] join/create/pay 三个接口增加 Redis 分布式锁，防重复提交
- [x] initiableActivities 改为 @PreAuthorize 权限注解
- [x] 全局异常处理器覆盖 servergroup + order 包
- [x] pay/getOrderStatus 增加订单归属校验（越权防护）
- [x] addUserToGroup 返回 Map → AddUserToGroupVO
- [x] getOrderStatus 返回 Map → GroupOrderStatusVO
- [x] 创建 GroupStatusEnum、GroupOrderStatusEnum、GroupConstants 基础类
- [x] 54+ 处魔法值替换为枚举/常量引用
- [x] 修复 handleUserInitiatedPayment initiated 状态越界 Bug
- [x] 全量日志格式统一为 [GROUP][XXX] 结构化前缀
- [x] join 时不递增 currentNum，延迟到支付成功回调（防超卖）
- [x] 事务内统一时间戳变量
- [x] 订单号前缀统一为 "GRP"
- [x] 价格计算统一走 GroupPriceCalculator
- [x] SSH 默认值替换为 GroupConstants 常量
- [x] 写操作补全 @Log 注解
- [x] 补全 @Api/@ApiOperation Swagger 文档注解
- [x] IDE 编译零错误

## 关联信息

- 禅道任务：（待补充）
- PR：（待补充）
- 设计稿：无
- 线上日志：无
