# Spec: Book Bookmark Enhancement

## ADDED Requirements

### Requirement: 书签高亮与去重

系统 SHALL 在电子书章节阅读页支持书签高亮展示，并在同一章节相近位置阻止重复创建书签。

#### Scenario: 创建非重复书签

- GIVEN 用户在章节正文中选择或定位到一个尚无相近书签的位置
- WHEN 用户通过书签新增入口创建书签
- THEN 系统保存书签位置、标签、颜色和进度信息
- AND 阅读页展示对应高亮或书签标记

#### Scenario: 阈值内重复创建书签

- GIVEN 同一章节内目标位置附近已存在书签
- WHEN 用户再次在阈值范围内创建书签
- THEN 系统拒绝重复创建
- AND 返回可理解的提示信息

### Requirement: 书签标签与删除确认

系统 SHALL 支持用户自定义书签标签，并在删除书签前进行二次确认。

#### Scenario: 自定义标签为空

- GIVEN 用户创建书签时未填写标签
- WHEN 系统保存书签
- THEN 书签标签回退为章节标题或默认文本

#### Scenario: 删除书签

- GIVEN 用户点击删除已有书签
- WHEN 用户确认删除
- THEN 系统删除该书签
- AND 书签列表与阅读页标记同步更新

### Requirement: 书籍级媒体类型

系统 SHALL 在书籍层维护媒体类型，用于区分电子书和漫画。

#### Scenario: 保存漫画书籍

- GIVEN 编辑者将书籍媒体类型设置为漫画
- WHEN 保存书籍
- THEN 系统将 `mediaType=2` 保存到 `osh_book.media_type`
- AND 后续章节详情按漫画模式返回章节页数据

#### Scenario: 读取旧电子书

- GIVEN 历史书籍未设置媒体类型
- WHEN 客户端读取书籍详情或章节详情
- THEN 系统按电子书 `mediaType=1` 处理

### Requirement: 漫画章节页持久化

系统 SHALL 使用 MySQL 表 `osh_book_chapter_page` 保存漫画章节图片页快照。

#### Scenario: 保存漫画章节图片

- GIVEN 漫画章节包含多张图片
- WHEN 编辑者保存电子书
- THEN 系统物理清理该章节旧页数据
- AND 按提交数组顺序生成连续 `page_no`
- AND 批量写入 `osh_book_chapter_page`

#### Scenario: 拖动图片到其他章节后保存

- GIVEN 编辑者将第一章图片拖动到第二章
- WHEN 保存电子书
- THEN 系统仍按各章节当前图片数组重建页码
- AND 不产生同一章节重复 `page_no`

### Requirement: 漫画章节详情返回页数据

系统 SHALL 在漫画章节详情中返回按页码排序的 `pages` 数据。

#### Scenario: 阅读漫画章节

- GIVEN 书籍媒体类型为漫画且章节存在图片页
- WHEN 客户端请求章节详情
- THEN 响应包含 `pages`
- AND `pages` 按 `page_no` 升序排列

### Requirement: 电子书导入占位接口

系统 SHALL 暂时暴露电子书导入占位接口，但不在本次实现完整解析落库。

#### Scenario: 调用电子书导入接口

- GIVEN 客户端调用 `/pc/book/import/ebook`
- WHEN 后端收到请求
- THEN 系统返回占位响应或明确提示暂未实现
- AND 不影响漫画/电子书现有保存流程

### Requirement: 漫画批量图片上传不受固定频率限流中断

系统 SHALL 允许漫画压缩包导入过程连续调用通用上传接口上传图片，并保留上传接口权限与文件校验。

#### Scenario: 连续上传漫画图片

- GIVEN 编辑者正在导入包含多张图片的漫画压缩包
- WHEN 前端连续调用 `/pc/upload` 上传漫画图片
- THEN 系统不因固定 IP 频率限流拒绝请求
- AND 上传接口仍要求登录权限与 `upload:file`、`book:create` 或 `book:update` 业务权限

## MODIFIED Requirements

### Requirement: 资源编号与权限控制

系统 SHALL 继续使用电子书资源编号枚举，并尽量采用权限等级注解保护电子书管理类接口。

#### Scenario: 创建或保存漫画资源

- GIVEN 编辑者保存漫画书籍
- WHEN 系统生成或校验资源编号
- THEN 资源编号仍使用 `ResourceCodePrefixEnum.BOOK`
- AND 不新增漫画专用资源编号前缀

#### Scenario: 访问管理类接口

- GIVEN 用户请求电子书管理类接口
- WHEN 系统进行权限校验
- THEN 权限等级注解和既有权限控制共同限制访问
