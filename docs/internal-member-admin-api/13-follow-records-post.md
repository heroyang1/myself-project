# POST /pc/admin/internal-member/{userId}/follow-records

## 用途

新增用户跟进记录。

## 路径参数

- `userId` 必填

## 请求体

- `followType` 必填，最大 64
- `content` 必填，最大 2000
- `nextFollowTime` 可选，格式: `yyyy-MM-dd HH:mm:ss`
- `responsibleUserId` 必填，必须是有效内部成员管理员

## 返回

- `R<Void>`

## 业务说明

- 先做付费用户校验
- 会校验负责人是否属于内部成员管理员角色
- 当前登录人作为 `operatorId`

## 代码入口

- Controller: `InternalMemberAdminController.addFollowRecord`
- Service: `InternalMemberServiceImpl.addFollowRecord`

