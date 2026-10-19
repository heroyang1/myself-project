# Spec: cart-add（用户把商品加入购物车）

> **状态**：SDD 四步闭环 · 第 1 步「定规范」产物。
> ⚠️ **这是课程演练契约（场景为课程项目购物车域），不是 osh-backend 本仓库的功能契约**——
> 本仓库无 cart 域，勿当事实引用；本仓库真实契约的唯一住所是
> `openspec/changes/<change-id>/specs/<capability>/spec.md`。

## 一、业务规则 × 验收判据

| # | 规则 | 验收判据（可判定断言） |
|---|---|---|
| R1 | **同一 SKU 累加数量**：同一用户重复加同一 SKU 不新增行，数量合并 | 加 A×2 后再加 A×3 → `cart` 表 `user_id=U AND sku_id=A` 结果**恰 1 行**且 `quantity=5`；单测断言行数与值 |
| R2 | **单个 SKU 上限 99**：累加后超上限则截断为 99 | 现有 97，加 ×5 → `quantity=99`（不是 102、不是 2 行），响应体含 `capped=true` 提示字段 |
| R3 | **加购前校验库存**：写库前校验可售库存 ≥ 本次加购数量 | `stock=0` 时调用加购 → cart 行零变化 + 返回缺货错误码；单测按调用顺序断言"库存校验先于 cart 写入"（verify 顺序） |
| R4 | **下架商品不可加**：SKU 非在售状态直接拒绝 | 对已下架 SKU 加购 → 返回下架错误码，cart 行数不变 |
| R5 | **重复提交幂等**：连点/网络重试不重复累加 | 同幂等键（客户端 token）并发提交两次 ×3 → 终态 `quantity=3` 而非 6；服务端凭键短路，第二次直接返回首次结果 |

## 二、异常路径（边界逐个列出）

| # | 边界情况 | 规定行为 |
|---|---|---|
| E1 | SKU 不存在 | 参数类错误码返回，cart 零变化 |
| E2 | `quantity` ≤ 0 / 非数字 / 超 int | 400 字段级校验拒绝；Controller 必挂 `@Validated`（缺省=零校验，A-11 教训） |
| E3 | 库存不足（stock < 本次数量） | 拒绝 + 缺货错误码（R3 严线） |
| E4 | **库存查询本身失败**（下游超时/宕机） | **放行加购 + 记 WARN 日志，下单环节强制复查**——校验严格度由环节定位决定：cart 不是扣减点，误拒不伤数据、伤体验；超卖防线设在下单（扣减点才配 fail-closed） |
| E5 | 加购成功后商品才下架（竞态） | cart 行**保留不静默删**（用户数据），列表展示"已下架"标记；下单复查拦截 |
| E6 | 同用户多端并发加购同 SKU | `(user_id, sku_id)` 唯一索引防双行；累加用 SQL `quantity = quantity + #{n}` 原子写，禁"读-改-写"（A-01 教训） |
| E7 | 未登录调用 | 401，不产生任何写入 |
| E8 | 幂等 token 缺失 | 拒绝并提示（幂等是接口契约的一部分，不默认宽容） |

## 三、开放问题（未写明的=未定义的，留人验拍板）

- Q1 单用户 cart 行数是否设上限（如 200 SKU）？——**本契约未定义，不许按"默认无上限"实现**
- Q2 截断到 99 时是否同时提示"已达上限"文案？返回体字段名待定
- Q3 幂等 token 有效期与存储介质（Redis TTL 多久）待定

## 四、本次允许改动的文件范围（先定边界）

**允许改：**
- `…/cart/controller/CartController.java`（新增加购端点）
- `…/cart/service/CartService.java`、`…/service/impl/CartServiceImpl.java`
- `…/cart/mapper/CartMapper.java` + 对应 XML（upsert / 原子累加）
- `…/cart/entity/CartItem.java`、`…/cart/dto/CartAddDTO.java`、`…/cart/vo/CartItemVO.java`
- `src/test/java/…/cart/**`（R1~R5 每条规则至少一个用例 + E2~E8 边界用例）
- `sql/` 下 cart 建表脚本（含 `(user_id, sku_id)` 唯一索引与必备六字段）

**禁止改：** order / stock / product 域的类与表（跨域只走对方 Service 接口只读）；
`pom.xml`、`application*`（R2 保护域，如确需触碰另立 change）；全局异常/拦截器配置。
