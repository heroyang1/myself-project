import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/** 沙盒测试入口：无 Maven 时用 JUnit Platform Launcher 直接跑并打印汇总。 */
public class TestRunner {
    public static void main(String[] args) {
        LauncherDiscoveryRequest req = request()
                .selectors(selectClass("com.backstage.system.cart.service.impl.CartServiceImplTest"))
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(req);
        TestExecutionSummary summary = listener.getSummary();
        PrintWriter out = new PrintWriter(System.out);
        summary.printTo(out);
        summary.printFailuresTo(out);
        out.flush();
        System.out.println("RESULT tests=" + summary.getTestsFoundCount()
                + " succeeded=" + summary.getTestsSucceededCount()
                + " failed=" + summary.getTotalFailureCount());
        if (summary.getTotalFailureCount() > 0) {
            System.exit(1);
        }
    }
}
