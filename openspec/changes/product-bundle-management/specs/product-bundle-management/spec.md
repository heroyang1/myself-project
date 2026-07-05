# Spec: Product Bundle Management Backend

## ADDED Requirements

### Requirement: 后台套餐列表管理

系统 SHALL 为 `level>=5` 的后台用户提供套餐分页查询能力，并支持按关键字、售卖模式和状态筛选。

#### Scenario: 查询套餐列表

- GIVEN 后台用户权限等级不低于 5
- WHEN 用户请求 `POST /pc/admin/bundle/page`
- THEN 系统返回 `total` 和 `rows`
- AND 每条套餐包含套餐编号、名称、售卖模式、价格、状态、销量、排序和商品数量

### Requirement: 后台创建和编辑套餐

系统 SHALL 支持后台用户创建和编辑套餐，并保存套餐基础信息和套餐商品明细。

#### Scenario: 创建打包优惠套餐

- GIVEN 后台用户提交 `saleMode=FLAT_BUNDLE`
- AND 请求中至少包含两个不同商品
- WHEN 用户请求 `POST /pc/admin/bundle/save`
- THEN 系统创建套餐主记录
- AND 系统创建套餐商品明细
- AND 新套餐默认处于草稿状态

#### Scenario: 创建买主送副套餐

- GIVEN 后台用户提交 `saleMode=PRIMARY_GIFT`
- AND 请求中存在且只存在一个 `primary` 商品
- AND 请求中至少存在一个 `secondary` 商品
- WHEN 用户请求 `POST /pc/admin/bundle/save`
- THEN 系统保存主商品和副商品角色
- AND 副商品 SHALL 标记为赠品

#### Scenario: 重复添加同一商品

- GIVEN 请求中的套餐商品存在相同 `resourceType` 和 `resourceId`
- WHEN 用户保存套餐
- THEN 系统拒绝保存并返回业务错误

### Requirement: 后台套餐状态管理

系统 SHALL 支持后台用户更新套餐状态，并阻止不安全的删除操作。

#### Scenario: 上架套餐

- GIVEN 套餐存在且包含商品明细
- WHEN 用户请求 `POST /pc/admin/bundle/status` 并提交 `status=1`
- THEN 系统将套餐状态更新为已上架

#### Scenario: 删除已上架套餐

- GIVEN 套餐状态为已上架
- WHEN 用户请求 `POST /pc/admin/bundle/delete`
- THEN 系统拒绝删除并提示先下架

### Requirement: 套餐资源搜索

系统 SHALL 在套餐模块内提供资源候选搜索能力，支持课程、电子书、工具、考试。

#### Scenario: 搜索课程资源

- GIVEN 后台用户权限等级不低于 5
- WHEN 用户请求 `POST /pc/admin/bundle/resource/search` 且 `resourceType=course`
- THEN 系统返回课程候选资源
- AND 每条候选包含 `resourceNo`、`resourceType`、`productType`、`productId`、`productName`、`price`、`originalPrice`、`coverUrl`

#### Scenario: 搜索不支持的资源类型

- GIVEN 请求参数中的 `resourceType` 不是 `course`、`book`、`tool`、`exam`
- WHEN 用户请求套餐资源搜索接口
- THEN 系统返回业务错误

### Requirement: 套餐模块边界隔离

系统 SHALL 保持套餐模块资源搜索实现独立，不修改兑换码模块资源 VO、Mapper 或 XML。

#### Scenario: 套餐搜索需要封面字段

- GIVEN 套餐资源搜索需要返回 `coverUrl`
- WHEN 系统查询资源候选
- THEN 字段 SHALL 定义在 `BundleResourceVO`
- AND 查询 SQL SHALL 位于 `mapper/bundle`
- AND 系统 SHALL NOT 修改兑换码模块资源查询契约
