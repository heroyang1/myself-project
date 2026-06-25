# Spec: Course Section Doc Save Read

## ADDED Requirements

### Requirement: 小节文档读取优先 osh_doc 并回退 text_content

系统 SHALL 在读取小节文档内容时，优先返回 `osh_doc` 主文档内容；若为空则回退 `osh_course_section.text_content`。

#### Scenario: 已绑定 osh_doc 的 text 小节

- GIVEN 小节已保存并写入 `osh_doc.content`
- WHEN 调用 `getCourseSectionEditContent(sectionId)`
- THEN 返回 `osh_doc` 中的文档内容
- AND 内容与最近一次 save 一致

#### Scenario: 仅存在 text_content 的历史小节

- GIVEN 小节无 `osh_doc` 绑定但 `text_content` 非空
- WHEN 调用 `getCourseSectionContent(sectionId, userId)`
- THEN 返回 `text_content` 内容

### Requirement: 编辑内容读取不回退 video media_url

系统 SHALL 在 `getCourseSectionEditContent` 中不回退 video 小节的 `media_url`，避免编辑页展示视频 URL 而非文档。

#### Scenario: video 小节无文档时读取 editContent

- GIVEN video 类型小节未绑定文档
- WHEN 调用 `getCourseSectionEditContent`
- THEN 返回空字符串
- AND 不回退 `media_url`

### Requirement: bindExisting 模式仅由 docMode 决定

系统 SHALL 仅在请求 `docMode=bindExisting` 时进入绑定已有文档模式，不因存在 `docId` 单独误判。

#### Scenario: 更新已有文档内容

- GIVEN 小节已绑定 doc 且请求非 bindExisting 模式
- WHEN 保存带 `textContent` 的更新
- THEN 更新对应 `osh_doc.content`
- AND 不因 docId 存在而跳过内容更新

### Requirement: 保存 text 小节时同步 osh_doc

系统 SHALL 在非 bindExisting 模式下，将 `textContent` 写入或更新 `osh_doc`，并维护 section 与 doc 的主引用关系。

#### Scenario: 首次保存带内容的 text 小节

- GIVEN 小节尚无主文档
- WHEN 保存含 `textContent` 的请求
- THEN 创建 `osh_doc` 记录并绑定到该 section
- AND 后续 editContent 可读到相同内容
