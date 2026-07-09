# Spec: content-background-management

## ADDED Requirements

### Requirement: 内容背景图匿名读取

系统 SHALL 允许匿名访问内容背景图读取接口，并按登录态返回用户背景图、默认背景图或空背景。

#### Scenario: 未登录读取背景图

- GIVEN 用户未登录
- WHEN 请求 `/pc/content-background/options`
- THEN 系统 SHALL 返回系统默认背景图
- AND 若系统未设置默认背景图，则 SHALL 返回空背景

#### Scenario: 已登录读取背景图

- GIVEN 用户已登录且已选择自定义背景图
- WHEN 请求 `/pc/content-background/options`
- THEN 系统 SHALL 优先返回该用户自定义背景图

### Requirement: 内容背景图管理权限

系统 SHALL 仅允许等级 5 及以上用户上传、管理和删除内容背景图。

#### Scenario: 等级不足用户上传

- GIVEN 当前用户等级小于 5
- WHEN 请求 `/pc/content-background/image/upload`
- THEN 系统 SHALL 拒绝请求

#### Scenario: 等级足够用户删除

- GIVEN 当前用户等级大于等于 5
- WHEN 请求 `/pc/content-background/image/delete`
- THEN 系统 SHALL 允许逻辑删除背景图

### Requirement: 删除当前已选背景图后的回退

系统 SHALL 在删除用户当前正在使用的背景图后，清理该用户的背景设置并回退到默认图或空背景。

#### Scenario: 删除当前已选图

- GIVEN 用户当前选中的图片就是被删除的图片
- WHEN 删除操作成功
- THEN 系统 SHALL 清空该用户的背景图引用
- AND 后续读取 SHALL 回退到系统默认背景图或空背景
