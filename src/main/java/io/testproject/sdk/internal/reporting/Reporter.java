/*
 * Copyright (c) 2020 TestProject LTD. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.testproject.sdk.internal.reporting;

import io.testproject.sdk.drivers.GenericDriver;
import io.testproject.sdk.drivers.ReportingDriver;
import io.testproject.sdk.internal.helpers.ReportingCommandsExecutor;
import io.testproject.sdk.internal.rest.AgentClient;
import io.testproject.sdk.internal.rest.messages.StepReport;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Steps reporter.
 */
public final class Reporter {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Reporter.class);

    /**
     * Regex which checks if the class is a Cucumber runner class.
     */
    private static final String CUCUMBER_OPTIONS_REGEX = "io\\.cucumber\\.\\w+\\.CucumberOptions";

    /**
     * {@link AgentClient} instance to submit reports to the Agent.
     */
    private final AgentClient agentClient;

    /**
     * Driver instance to be used for taking screenshots.
     */
    private final ReportingDriver driver;

    /**
     * An action's result. This value will be reported after the addon is uploaded.
     */
    private String result;

    /**
     * Initializes a new instance using provided driver and agentClient.
     *
     * @param driver      Driver instance to be used for taking screenshots.
     * @param agentClient {@link AgentClient} instance to submit reports to the Agent.
     */
    public Reporter(final ReportingDriver driver, final AgentClient agentClient) {
        this.agentClient = agentClient;
        this.driver = driver;
        if (Boolean.getBoolean("TP_DISABLE_AUTO_REPORTS") || checkIfCucumber()) {
            this.disableTestAutoReports(true);
            this.disableCommandReports(true);
        }
    }

    /**
     * Checks if the caller class is a runner class for Cucumber tests.
     * @return True if the class is a Cucumber tests runner class, false otherwise.
     */
    private boolean checkIfCucumber() {
        try {
            List<StackTraceElement> traces = Arrays.asList(Thread.currentThread().getStackTrace());
            return isCucumberPresent(traces);
        } catch (ClassNotFoundException e) {
            LOG.error("Unable to find calling class related to Cucumber", e);
            return false;
        }
    }

    /**
     * Checks if the current class is annotated with a cucumber annotation.
     * @param callerClass The current class.
     * @return True if the class is annotated with CucumberOptions, false otherwise.
     */
    private boolean isCucumberAnnotationPresent(final Class<?> callerClass) {
        return callerClass != null && Arrays.stream(callerClass.getDeclaredAnnotations()).anyMatch(a ->
                a.annotationType().getName().matches(CUCUMBER_OPTIONS_REGEX));
    }

    /**
     * Helper method which scans the current stack trace for the cucumber runner class.
     * @param traces Current thread stacktrace.
     * @return True if one of the classes was annotated with Cucumber Annotations, false otherwise.
     * @throws ClassNotFoundException If no class was found for the stacktrace element.
     */
    private boolean isCucumberPresent(final List<StackTraceElement> traces) throws ClassNotFoundException {
        for (StackTraceElement ste : traces) {
            if (isCucumberAnnotationPresent(Class.forName(ste.getClassName()))) {
                return true;
            }
        }

        // No annotation was found, return false.
        return false;
    }

    /**
     * Enables or disables all types of reports.
     * @param disable True to disable or False to enable.
     */
    public void disableReports(final boolean disable) {
        this.driver.getReportingCommandExecutor().setReportsDisabled(disable);
    }

    /**
     * Enables or disables driver commands reporting.
     * @param disable True to disable or False to enable.
     */
    public void disableCommandReports(final boolean disable) {
        this.driver.getReportingCommandExecutor().setCommandReportsDisabled(disable);
    }

    /**
     * Enables or disables <em>automatic</em> test reporting.
     * Test name is inferred by traversing the stack trace and searching for automation framework annotations.
     * <p>
     * For example if JUnit @Test or @DisplayName annotation (if found).
     * If no supported annotations are found, it uses the <em>very first method in the call stack.</em>.
     * <p>
     * Disabling this mechanism will require manual calls to report tests using {@link #test(String)} ()}
     * This feature <b>must</b> by disabled in order to use the manual reporting.
     *
     * @param disable True to disable or False to enable.
     */
    public void disableTestAutoReports(final boolean disable) {
        this.driver.getReportingCommandExecutor().setTestAutoReportsDisabled(disable);
    }

    /**
     * Enables or disables redaction for driver command reports with values of secured elements.
     * When enabled it will redact the characters sent using driver.sendKeys() with three stars: <em>***</em>
     * <p>
     * Redaction will occur only when the characters are sent to a secured element.
     * <p>
     * Secured elements are:
     * Any element with <em>type</em> attribute set to <b>password</b>
     * iOS element <b>XCUIElementTypeSecureTextField</b>
     *
     * @param disable True to disable or False to enable.
     */
    public void disableRedaction(final boolean disable) {
        this.driver.getReportingCommandExecutor().setRedactionDisabled(disable);
    }

    /**
     * Report step with description.
     *
     * @param description Step description.
     */
    public void step(final String description) {
        step(description, "", false);
    }

    /**
     * Report step with description and pass/fail flag.
     *
     * @param description Step description.
     * @param passed      True to mark steps as Passed, otherwise False.
     */
    public void step(final String description, final boolean passed) {
        step(description, passed, false);
    }

    /**
     * Report step with description, message and screenshot.
     *
     * @param description Step description.
     * @param passed      True to mark step as Passed, otherwise False.
     * @param screenshot  True to take a screenshot, otherwise False.
     */
    public void step(final String description, final boolean passed, final boolean screenshot) {
        step(description, "", passed, screenshot);
    }

    /**
     * Report step with description and message.
     *
     * @param description Step description.
     * @param message     Step message.
     */
    public void step(final String description, final String message) {
        step(description, message, true, false);
    }

    /**
     * Report step with description, message and screenshot.
     *
     * @param description Step description.
     * @param message     Step message.
     * @param screenshot  True to take a screenshot, otherwise False.
     */
    public void step(final String description, final String message, final boolean screenshot) {
        step(description, message, true, screenshot);
    }

    /**
     * Report step with description, message, screenshot and pass/fail indication.
     * @param description Step description.
     * @param message     Step message.
     * @param passed      True to mark step as Passed, otherwise False.
     * @param screenshot  True to take a screenshot, otherwise False.
     */
    public void step(final String description,
                     final String message,
                     final boolean passed,
                     final boolean screenshot) {
        // Report test step if needed.

        // If manual reporting is disabled, skip.
        if (Boolean.getBoolean("TP_DISABLE_MANUAL_REPORTS")) {
            LOG.warn("Manual reporting is disabled");
            return;
        }

        if (!this.driver.getReportingCommandExecutor().isReportsDisabled()) {
            List<StackTraceElement> traces = Arrays.asList(Thread.currentThread().getStackTrace());
            this.driver.getReportingCommandExecutor().reportTest(traces, false);
        }

        StepReport report = new StepReport(description, message, passed,
                screenshot ? driver.getScreenshot() : null);

        if (driver.getReportingCommandExecutor().isReportsDisabled()) {
            LOG.trace("Step [{}] - [{}]", description, passed ? "Passed" : "Failed");
            return;
        }

        if (!agentClient.reportStep(report)) {
            LOG.error("Failed reporting step to Agent");
        }
    }

    /**
     * Creates a new report using provided name.
     * <em>Note:</em> Result is set to <b>false</b> (failed) by default!
     * To specify result type use {@link #test(String, boolean)} overloaded method.
     *
     * @param name Test name.
     * @return {@link ClosableTestReport} instance
     */
    public ClosableTestReport test(final String name) {
        return test(name, false);
    }

    /**
     * Creates a new report with provided name and result.
     *
     * @param name   Test name.
     * @param passed True is passed, otherwise False.
     * @return {@link ClosableTestReport} instance
     */
    public ClosableTestReport test(final String name, final boolean passed) {
        return test(name, passed, null);
    }

    /**
     * Creates a new report with provided name, result and message.
     *
     * @param name    Test name.
     * @param passed  True is passed, otherwise False.
     * @param message Result message.
     * @return {@link ClosableTestReport} instance
     */
    public ClosableTestReport test(final String name, final boolean passed, final String message) {

        ReportingCommandsExecutor executor;

        if (driver instanceof GenericDriver) {
            executor = driver.getReportingCommandExecutor();
        } else {
            executor = (ReportingCommandsExecutor) ((RemoteWebDriver) driver).getCommandExecutor();
        }

        if (!executor.isTestAutoReportsDisabled()) {
            LOG.warn("Automatic test reports is enabled, disable it to report tests manually and avoid duplicates.");
        }

        return new ClosableTestReport(agentClient, driver, name, passed, message);
    }

    /**
     * Reports the result of an action execution. This is only sent when addon is executed after upload.
     * @param message Action result.
     */
    public void result(final String message) {
        this.result = message;
    }
}
