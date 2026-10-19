# Spec: harness-external-dependency

## ADDED Requirements

### Requirement: 外部依赖规范文档作为规范层单一事实源

系统 SHALL 在 `.harness/外部依赖规范.md` 维护外部调用三解法（线程池/超时/熔断降级）
的规范层全文，且每一节开头 SHALL 写明"什么时候读这份规范"。

#### Scenario: 开发者或 AI 准备编写外部调用代码

- GIVEN 任务涉及支付/物流/短信等外部服务调用或 HTTP 客户端构造
- WHEN 按 CLAUDE.md 第 3 层索引检索规范
- THEN 索引表中存在指向 `.harness/外部依赖规范.md` 的一行（内容→位置→何时装载三列齐备）
- AND 该文件三节齐全，每节首行可回答"什么时候读"

### Requirement: 宪法行数纪律不因新增索引而破坏

CLAUDE.md 追加索引行时 SHALL 同步压缩等量冗余行，总行数保持 ≤ 100。

#### Scenario: 索引行写入后校验

- GIVEN `.harness/外部依赖规范.md` 已在第 3 层登记一行
- WHEN 执行 `Get-Content CLAUDE.md | Measure-Object -Line`
- THEN 输出 ≤ 100

### Requirement: 规范文档不得与红线区形成双事实源

规范文档 SHALL 只作细则展开；涉及红线句 SHALL 以"待 P6 决定是否晋升"标注引用，
不得复制 CLAUDE.md 第 2 层正文形成第二住所。

#### Scenario: 审查文档治理合规

- GIVEN `.harness/外部依赖规范.md` 落盘完成
- WHEN 对照 CLAUDE.md 第 2 层逐条检查
- THEN 无一条红线正文被复制进规范文档；收口红线句仅以候选形态出现并注明待决状态

## MODIFIED Requirements

无。

## REMOVED Requirements

无。
