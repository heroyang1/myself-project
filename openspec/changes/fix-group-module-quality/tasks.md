# Tasks: fix-group-module-quality

## 准备

- [x] 阅读 `openspec/project.md`
- [x] 阅读拼团模块全部核心文件（3 Controller + ServiceImpl + Mapper XML）
- [x] 确认 proposal/design 评审（本次为修复性变更，事后补档）

## 实现 — 安全加固（P0）

- [x] join/create/pay 三个接口新增 Redis 分布式锁（setIfAbsent + 10s TTL + finally delete）
- [x] PcExceptionHandler 扩展 basePackages 覆盖 servergroup + order 包
- [x] pay 接口增加订单归属校验（userId 匹配）
- [x] getOrderStatus 增加订单归属校验

## 实现 — 权限与规范（P1）

- [x] initiableActivities 手写 userLevel 校验改为 @PreAuthorize
- [x] 修复接口编号重复（两个"接口11"）
- [x] Controller 层移除冗余 try-catch（4 个方法），由全局异常处理器兜底
- [x] addUserToGroup 返回 Map → AddUserToGroupVO
- [x] getOrderStatus 返回 Map → GroupOrderStatusVO
- [x] IOshGroupServerService 接口签名同步变更
- [x] 写操作补全 @Log 注解（join/create/addUserToGroup=INSERT, pay=UPDATE, add=INSERT）
- [x] GroupController/OshGroupOrderController 补全 @Api + @ApiOperation

## 实现 — 业务一致性（P1）

- [x] createGroupWork/addUserToGroup/handlePaymentSuccess 事务内统一时间戳变量
- [x] 订单号前缀统一：joinGroup 中 "GP" → GroupConstants.ORDER_NO_PREFIX（"GRP"）
- [x] 修复 handleUserInitiatedPayment 满员时 initiated 状态越界（3→2）

## 实现 — 枚举常量化（P2-P3）

- [x] 创建 GroupStatusEnum（组团状态 0/1/2/3）
- [x] 创建 GroupOrderStatusEnum（订单状态字符串）
- [x] 创建 GroupConstants（35 个常量）
- [x] 创建 AddUserToGroupVO（8 字段）
- [x] 创建 GroupOrderStatusVO（6 字段）
- [x] ServiceImpl 中 25+ 处组团状态码替换为 GroupStatusEnum
- [x] ServiceImpl 中 11 处订单状态字符串替换为 GroupOrderStatusEnum
- [x] ServiceImpl 中 6 处 SSH 默认值替换为 GroupConstants
- [x] ServiceImpl 中 3 处 "wechat" 替换为 GroupConstants.DEFAULT_PAY_METHOD
- [x] ServiceImpl 中 5 处业务数字替换为 RECRUIT_EXPIRE_DAYS/MAX_DURATION_MONTHS 等
- [x] ServiceImpl 中 4 处缓存 Key 前缀迁移到 GroupConstants
- [x] JoinGroupVO 中 "pending"/"success"/30 替换为枚举/常量引用

## 实现 — 日志与价格（P3）

- [x] 全量日志格式统一：`【拼团XXX】` → `[GROUP][XXX]` 结构化前缀（30+ 处）
- [x] GroupConstants 中定义 10 个日志标识常量
- [x] joinUserInitiatedGroup 内联价格计算改用 GroupPriceCalculator
- [x] 兜底逻辑 0.8 改用 GroupPriceCalculator.calculateMemberPrice()

## 实现 — 开发规范整改

- [x] GroupController 变量名 iGroupService → groupService
- [x] GroupController 类注释规范化（@author/@date）
- [x] OshGroupOrderController add 方法 R 改为 R<OshGroupOrder>
- [x] 清理未使用的 import

## 验证

- [x] IDE 编译零错误（全部 12 个修改/新增文件）
- [x] grep 扫描确认无遗留魔法值硬编码
- [x] grep 扫描确认无遗留旧日志格式
- [x] 确认 course/read、column/read、POST /pc/order/group 保持 @Anonymous
- [x] 确认 initiated 相关操作保留原始状态值并有注释说明

## PR

- [ ] PR 描述关联 OpenSpec change（待补充）
- [ ] 填写测试结果
- [ ] 填写风险与回滚
- [ ] 指派 Reviewer
