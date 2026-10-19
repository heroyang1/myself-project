package com.backstage.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 架构分层测试（ArchUnit，JUnit 5 写法）。
 *
 * <p>落位在 backstage-admin：该模块聚合 framework→system→common 全部依赖，
 * 因此可看到全工程 {@code com.backstage.*} 字节码，校验跨模块全局分层。
 *
 * <p>backstage-admin 未引入 junit-vintage-engine，本模块新测试一律用 JUnit 5，
 * 故此处使用 {@code @AnalyzeClasses}/{@code @ArchTest}（archunit-junit5 扩展，跑在 Jupiter 上）。
 * 字段用 {@code public static final}：Java 8 + JUnit 5 反射访问更稳妥。
 *
 * <p>规则依据 docs/architecture/boundaries.md、docs/architecture/package-structure.md 的单向依赖约定：
 * Controller → Service → Mapper → DB，common 为最底层不被反向依赖。
 *
 * <p>与通用模板的差异（按本项目现实做了保守适配）：
 * <ul>
 *   <li>层访问用 {@code consideringOnlyDependenciesInLayers()}：Kafka 消费者（framework.consumer）、
 *       定时任务（task/quartz）不属于任何已定义层，它们调用 Service/Mapper 不计为违规，避免误报。</li>
 *   <li>不定义 Infrastructure 层（本项目无 {@code ..infrastructure..} 包），不定义 Config 层
 *       （config→service 的装配依赖属正常，不应被判违规）。</li>
 *   <li>不启用"禁止 @Autowired 字段注入"规则：字段注入是本 RuoYi 项目的既有主流约定，非缺陷。</li>
 * </ul>
 *
 * <p>“零违规强制 + 存量冻结”分级（与 openspec/changes/enhance-harness-closure 同步）：
 * <ul>
 *   <li>{@code common_不得反向依赖上层模块} 实测零违规，保持普通 {@link ArchRule} 即时强制。</li>
 *   <li>{@code mapper_不得依赖_controller_或_service}：原注释断言"零违规"未经 CI 验证，实跑 53 处
 *       （{@code system.mapper.book.BookEsMapper}/{@code BookMapper}/{@code system.mapper.tool.OshToolEsMapper}
 *       依赖放错包的 {@code controller.book.BookListReqVO}、{@code service.tool.ToolIndexMessage} 两个数据类）。
 *       暂套 {@link FreezingArchRule} 将 53 处存量入档、只拦新增；根因是 VO/Message 归位问题，
 *       须另立 change 把这两个类迁到 domain/dto 后再拆下 freeze 升回硬断言。</li>
 *   <li>{@code 层间依赖方向} 与 {@code controller_不得直接使用_mapper} 存在存量违规（实测 12 个
 *       Controller 直接 import Mapper），套 {@link FreezingArchRule} 将当时存量入档、只拦新增；
 *       基线存于 {@code backstage-admin/archunit-store/}，必须提交入仓。存量清完后需拆下 freeze 升回硬断言。</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.backstage",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class LayeredArchitectureTest {

    /**
     * 层间依赖方向：Controller → Service → Mapper，Domain 为纯模型。
     * <p>存量存在（quartz/consumer 回调 + 12 个 Controller 直连 Mapper），本规则暂以 FreezingArchRule 基线形式上线。
     */
    @ArchTest
    public static final ArchRule 层间依赖方向 = FreezingArchRule.freeze(
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Domain").definedBy("..domain..")
                    .layer("Mapper").definedBy("..mapper..")
                    .layer("Service").definedBy("..service..")
                    .layer("Controller").definedBy("..controller..")
                    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                    .whereLayer("Mapper").mayOnlyBeAccessedByLayers("Service")
                    .as("[违规] 层级依赖方向被破坏。\n"
                            + "[修复] Controller 不得被任何层访问；Service 只能被 Controller 访问；Mapper 只能被 Service 访问。\n"
                            + "[参考] docs/architecture/boundaries.md"));

    /**
     * Controller 不得直接使用 Mapper（必须经 Service 编排）。
     * <p>实测存量 12 个 Controller 直接 import Mapper，本规则套 FreezingArchRule 只拦新增。
     */
    @ArchTest
    public static final ArchRule controller_不得直接使用_mapper = FreezingArchRule.freeze(
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..mapper..")
                    .as("[违规] Controller 不得直接依赖 Mapper。\n"
                            + "[修复] 在 Service 中编排数据访问，Controller 仅持有 Service 引用。\n"
                            + "[参考] docs/architecture/boundaries.md"));

    /**
     * Mapper 层是纯数据访问，禁止反向依赖 Controller / Service。
     */
    @ArchTest
    public static final ArchRule mapper_不得依赖_controller_或_service = FreezingArchRule.freeze(
            noClasses()
                    .that().resideInAPackage("..mapper..")
                    .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..")
                    .as("[违规] Mapper 反向依赖了 Controller/Service。\n"
                            + "[修复] Mapper 仅做数据访问，依赖方向恒为 Controller→Service→Mapper；"
                            + "若确为 VO/Message 放错包，应把该类迁到 domain/dto 而非放宽本规则。\n"
                            + "[参考] docs/architecture/boundaries.md"));

    /**
     * common 是最底层共享模块，禁止反向依赖 framework / system / admin(web)。
     * 这是模块依赖链（admin→framework→system→common）的硬约束。
     */
    @ArchTest
    public static final ArchRule common_不得反向依赖上层模块 = noClasses()
            .that().resideInAPackage("com.backstage.common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.backstage.framework..",
                    "com.backstage.system..",
                    "com.backstage.web..")
            .as("[违规] backstage-common 反向依赖了上层模块。\n"
                    + "[修复] common 是最底层，被所有模块依赖，不得引用 framework/system/admin 的类。\n"
                    + "[参考] docs/architecture/package-structure.md");
}
