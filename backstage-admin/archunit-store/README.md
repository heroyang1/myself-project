# ArchUnit FreezingArchRule 基线目录

本目录存放 `FreezingArchRule.freeze(...)` 生成的**存量违规基线**。CI 在跑
`backstage-admin` 模块的 `LayeredArchitectureTest` 与 `CodingConventionArchitectureTest` 时读取此目录，
把当时的存量视为"合法"放行、**只拦新增**。

## 生成方式

在仓库根执行：

```bash
mvn -B test -pl backstage-admin -am \
  -Dtest='LayeredArchitectureTest,CodingConventionArchitectureTest'
```

首次运行会在本目录写入若干 `.properties` 文件（每个冻结规则一份）。**运行完成后必须**：

```bash
git add backstage-admin/archunit-store/
git commit -m "chore(harness): freeze ArchUnit baseline for existing violations"
```

否则 CI 的 `Assert ArchUnit test files present` 步骤会 fail（防静默跳过）。

## 当前冻结范围

| 测试类 | 规则 | 存量描述 |
|---|---|---|
| LayeredArchitectureTest | `层间依赖方向` | quartz/consumer 回调 + 12 个 Controller 直连 Mapper（详见 openspec/changes/enhance-harness-closure/proposal.md） |
| LayeredArchitectureTest | `controller_不得直接使用_mapper` | UserBlock/FriendLink/BehaviorStats/CoursePay/OshOpenProject/UserOrder/OshQAQuestion/SeckillItemEsIndex/OshGroupServer/AdminInvite/UserInvitation/UserManage 共 12 处 |
| CodingConventionArchitectureTest | `禁止_System_out_println` | 存量 println 使用 |
| CodingConventionArchitectureTest | `裸HTTP客户端仅限client包` | client/integration 之外仍有直接 HttpURLConnection/RestTemplate 用法 |

`禁止_printStackTrace`、`mapper_不得依赖_controller_或_service`、`common_不得反向依赖上层模块`
实测零违规，保持普通 `ArchRule`，不入本基线。

## 到期与升级

存量清理完毕后：
1. 删除本目录下对应规则的 `.properties` 文件；
2. 把 `LayeredArchitectureTest` / `CodingConventionArchitectureTest` 中对应字段的
   `FreezingArchRule.freeze(...)` 拆下，恢复为普通 `ArchRule`；
3. 若整目录已空，保留 `.gitkeep` 与本 README 即可（供未来新冻结规则复用）。

参考：`openspec/changes/enhance-harness-closure/`、`docs/architecture/boundaries.md`、
`docs/ai/conventions/logging.md`、`docs/ai/conventions/api-calls.md`。
