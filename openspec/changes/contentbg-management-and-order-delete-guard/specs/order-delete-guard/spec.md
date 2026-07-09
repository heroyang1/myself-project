# Spec: order-delete-guard

## MODIFIED Requirements

### Requirement: 订单数据不可直接删除

系统 SHALL 不再提供订单主数据的直接删除能力，订单模块只允许查询、状态流转、关闭或取消等受控操作。

#### Scenario: 业务侧请求删除订单

- GIVEN 调用方希望删除订单主数据
- WHEN 进入订单模块相关能力
- THEN 系统 SHALL NOT 提供直接删除接口
- AND 调用方 SHALL 改用状态管理或关闭语义

#### Scenario: 已存在订单数据

- GIVEN 数据库中存在历史订单
- WHEN 订单模块升级到本次变更
- THEN 系统 SHALL 继续读取历史订单数据
- AND SHALL 不对历史订单做物理删除
