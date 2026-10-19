package com.backstage.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import com.backstage.common.utils.spring.SpringUtils;
import java.io.PrintStream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 编码约定架构测试（ArchUnit，JUnit 5）——践行"错误信息即 Prompt"：
 * 每条规则的失败信息都含三要素 [违规]什么错了 / [修复]怎么改含代码 / [参考]看哪个文档，
 * 让 Agent/开发者无需额外提示即可自助修复。
 *
 * <p>分两档：
 * <ul>
 *   <li>零违规规则（如 printStackTrace）→ 普通 ArchRule，现在即强制。</li>
 *   <li>存量有违规规则（如 println、裸 HTTP）→ {@link FreezingArchRule} 冻结基线，
 *       放行现有、只拦新增；基线存于 backstage-admin/archunit-store（首次运行生成，需提交）。</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.backstage",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class CodingConventionArchitectureTest {

    /** 禁止 e.printStackTrace()（阿里手册【强制】）——实测零违规，直接强制。 */
    @ArchTest
    public static final ArchRule 禁止_printStackTrace = noClasses()
            .should().callMethod(Throwable.class, "printStackTrace")
            .as("[违规] 生产代码禁止 e.printStackTrace()。\n"
                    + "[修复] 用 SLF4J 输出完整堆栈：\n"
                    + "  private static final Logger log = LoggerFactory.getLogger(X.class);\n"
                    + "  log.error(\"操作失败, param={}\", param, e);\n"
                    + "[参考] docs/ai/conventions/logging.md");

    /** 禁止 System.out.println（阿里手册【强制】）——存量有违规，冻结基线只拦新增。 */
    @ArchTest
    public static final ArchRule 禁止_System_out_println = FreezingArchRule.freeze(
            noClasses()
                    .should().callMethod(PrintStream.class, "println", String.class)
                    .orShould().callMethod(PrintStream.class, "println", Object.class)
                    .as("[违规] 生产代码禁止 System.out.println。\n"
                            + "[修复] 用 SLF4J 占位符日志：\n"
                            + "  private static final Logger log = LoggerFactory.getLogger(X.class);\n"
                            + "  log.info(\"结果={}\", result);\n"
                            + "[参考] docs/ai/conventions/logging.md"));

    /** 裸 HTTP 客户端只允许封装在 client/integration 包内——存量有违规，冻结基线只拦新增。 */
    @ArchTest
    public static final ArchRule 裸HTTP客户端仅限client包 = FreezingArchRule.freeze(
            noClasses()
                    .that().resideOutsideOfPackages("..client..", "..integration..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "okhttp3..", "org.apache.http..", "org.springframework.web.client..")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName("java.net.HttpURLConnection")
                    .as("[违规] 禁止在 client/integration 之外直接使用裸 HTTP 客户端"
                            + "（RestTemplate/OkHttp/HttpURLConnection/Apache HttpClient）。\n"
                            + "[修复] 把外部调用封装进 ..client../..integration.. 的 client 类"
                            + "（如 JdkHuifuHttpTransport、BaiduPanClient），Service 只注入 client：\n"
                            + "  private final FooClient fooClient; // 构造器注入\n"
                            + "  Foo foo = fooClient.get(\"/endpoint\", Foo.class);\n"
                            + "[参考] docs/ai/conventions/api-calls.md"));

    /** 禁经 SpringUtils 运行时取 Bean（CLAUDE.md R4④）——存量 infra 有违规，冻结基线只拦新增。 */
    @ArchTest
    public static final ArchRule 禁止经SpringUtils运行时取Bean = FreezingArchRule.freeze(
            noClasses()
                    .should().callMethod(SpringUtils.class, "getBean", String.class)
                    .orShould().callMethod(SpringUtils.class, "getBean", Class.class)
                    .as("[违规] 生产代码禁止经 SpringUtils.getBean 运行时取 Bean"
                            + "（依赖方向红线 R4④，编译期看不见、静态扫描最难抓）。\n"
                            + "[修复] 用构造器/字段注入声明依赖，让容器在装配期注入：\n"
                            + "  private final FooService fooService; // 构造器注入\n"
                            + "  确需运行时取 Bean 的场景走评审并记 .harness/candidate/。\n"
                            + "[参考] CLAUDE.md R4④、docs/architecture/module-dependency-rules.md"));
}
