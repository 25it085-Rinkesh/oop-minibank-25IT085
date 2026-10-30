package test;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

/**
 * Command-line runner for MiniBank JUnit 5 test suites.
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   RUNNING MINIBANK JUNIT 5 TESTS");
        System.out.println("========================================\n");

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("test.service"))
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));

        long failed = summary.getTotalFailureCount();
        long passed = summary.getTestsSucceededCount();
        long found  = summary.getTestsFoundCount();

        System.out.println("\n----------------------------------------");
        System.out.printf("Tests Run: %d, Succeeded: %d, Failed: %d%n", found, passed, failed);
        System.out.println("Result   : " + (failed == 0 ? "ALL TESTS PASSED" : "TEST FAILURES DETECTED"));
        System.out.println("========================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
