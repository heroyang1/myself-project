# Spec: book-content-access

## MODIFIED Requirements

### Requirement: 电子书详情不得泄露未授权漫画图片

系统 SHALL 在 `/pc/book/getById` 返回漫画电子书详情时，只为当前用户可读的章节返回 `pages`。

#### Scenario: 未购买用户查看付费漫画书详情

- GIVEN 一本漫画电子书包含免费章节和收费章节
- AND 当前用户未购买且等级不足
- WHEN 用户请求 `/pc/book/getById`
- THEN 免费章节可以返回 `pages`
- AND 收费章节的 `pages` 必须为空

#### Scenario: 已购买用户查看付费漫画书详情

- GIVEN 一本漫画电子书包含收费章节
- AND 当前用户已购买该电子书
- WHEN 用户请求 `/pc/book/getById`
- THEN 收费章节可以返回 `pages`

### Requirement: 电子书目录不得泄露未授权漫画图片

系统 SHALL 在 `/pc/book/menus` 返回漫画电子书目录时，只为当前用户可读的章节返回 `pages`。

#### Scenario: 未授权用户查看漫画目录

- GIVEN 一本漫画电子书包含收费章节
- AND 当前用户未购买且等级不足
- WHEN 用户请求 `/pc/book/menus`
- THEN 收费章节仍可作为目录项返回
- AND 收费章节的 `pages` 必须为空

### Requirement: 章节阅读接口继续执行章节级权限校验

系统 SHALL 在 `/pc/book/detail` 返回漫画章节内容前校验当前用户是否可读该章节。

#### Scenario: 未授权用户读取收费漫画章节

- GIVEN 一本付费漫画电子书包含收费章节
- AND 当前用户未购买且等级不足
- WHEN 用户请求 `/pc/book/detail`
- THEN 系统拒绝返回该章节内容和图片
