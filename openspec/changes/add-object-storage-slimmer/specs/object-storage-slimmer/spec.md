# Spec: object-storage-slimmer

## ADDED Requirements

### Requirement: 对象瘦身任务登记

系统 SHALL 为对象存储中的视频对象建立瘦身任务记录，并按 bucket 与 object key 保持唯一。

#### Scenario: 扫描 R2 视频对象

- GIVEN 管理员触发对象扫描
- WHEN R2 桶中存在支持的视频扩展名对象
- THEN 系统 SHALL 为尚未登记的对象创建 `pending` 任务
- AND 已登记对象 SHALL 更新对象大小和最近扫描时间

### Requirement: 备份优先

系统 SHALL 在覆盖原对象前先把原对象复制到备份桶。

#### Scenario: 备份桶不存在

- GIVEN 任务准备瘦身
- WHEN `<bucket>-backup` 不存在
- THEN 系统 SHALL 创建备份桶
- AND 将原对象复制到备份桶后才允许下载和转码

### Requirement: 保守视频瘦身

系统 SHALL 默认保留视频分辨率，仅通过压码率降低体积。

#### Scenario: 输出视频校验成功

- GIVEN 视频已完成 ffmpeg 输出
- WHEN 输出视频可被 ffprobe 读取、时长差不超过阈值、体积节省达到阈值
- THEN 系统 SHALL 覆盖原 object key
- AND 记录 `success`、新大小、节省比例和备份 key

#### Scenario: 输出视频不满足条件

- GIVEN 视频已完成 ffmpeg 输出
- WHEN 输出视频不可播放、时长差过大或节省不足
- THEN 系统 SHALL NOT 覆盖原 object key
- AND SHALL 将任务标记为 `failed` 或 `no_need`

### Requirement: 可选瘦身模式

系统 SHALL 支持管理员和 XXL-JOB 选择不同视频瘦身模式，并记录任务实际使用的模式。

#### Scenario: 使用默认模式

- GIVEN 管理员未指定瘦身模式
- WHEN 系统执行视频瘦身
- THEN 系统 SHALL 使用 `balanced` 模式
- AND SHALL 不降低视频分辨率

#### Scenario: 使用节省优先模式

- GIVEN 管理员指定 `storage_saver` 或 `low_cost` 模式
- WHEN 原视频分辨率高于模式上限
- THEN 系统 SHALL 按模式上限降低分辨率
- AND SHALL 记录任务的 `slim_mode`

### Requirement: 管理员可控调度

系统 SHALL 提供业务开关控制 XXL-JOB 触发的瘦身任务，手动触发不受该开关限制。

#### Scenario: 开关关闭

- GIVEN XXL-JOB 触发 handler
- WHEN 对象瘦身业务开关为关闭
- THEN handler SHALL 直接退出
- AND 不扫描、不转码、不覆盖对象

#### Scenario: 管理员手动灰度验证

- GIVEN 对象瘦身业务开关为关闭
- WHEN 管理员手动触发单个视频瘦身
- THEN 系统 SHALL 允许执行
- AND 仍 SHALL 遵守备份、校验和覆盖保护规则

### Requirement: 备份不自动删除

系统 SHALL NOT 自动删除备份桶中的原视频。

#### Scenario: 管理员确认删除备份

- GIVEN 任务状态为 `success`
- WHEN 管理员在后台手动确认删除备份
- THEN 系统 SHALL 删除对应备份对象
- AND 记录备份删除时间和状态

### Requirement: 回滚

系统 SHALL 支持从备份桶把对象复制回原 key。

#### Scenario: 管理员回滚成功任务

- GIVEN 任务存在备份对象
- WHEN 管理员触发回滚
- THEN 系统 SHALL 将备份对象复制回原 bucket/object key
- AND 将任务标记为 `rolled_back`

### Requirement: QA 桶隔离

系统 SHALL 在测试环境使用 `osh-qa` 桶，避免测试写入生产 `osh` 桶。

#### Scenario: 测试环境上传或瘦身

- GIVEN 应用运行在测试环境
- WHEN 读写对象存储
- THEN 系统 SHALL 使用 `osh-qa`
- AND 不写入生产 `osh`
