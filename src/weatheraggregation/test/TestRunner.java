package weatheraggregation.test;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestRunner {
    public static void main(String[] args) {
        // Specify the test classes to run
        Class<?>[] testClasses = { AggregationServerTests.class, ContentServerTests.class, GETClientTests.class, JsonParserTests.class, LamportTests.class, ReplicatedContentServerTests.class };

        // Run the tests
        Result result = JUnitCore.runClasses(testClasses);

        // Print the results
        System.out.println("Tests run: " + result.getRunCount());
        System.out.println("Failures: " + result.getFailureCount());

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
        }

        // Print whether all tests were successful
        if (result.wasSuccessful()) {
            System.out.println("[ALERT]: All tests passed.");
        } else {
            System.out.println("[ERROR]: Some tests failed.");
        }
    }
}
