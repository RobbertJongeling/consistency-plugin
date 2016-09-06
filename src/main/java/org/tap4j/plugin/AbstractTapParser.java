/*
 * The MIT License
 *
 * Copyright (c) 2011-2016 Bruno P. Kinoshita
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.tap4j.plugin;

import java.io.PrintStream;
import java.util.List;
import java.util.logging.Logger;

import org.tap4j.model.Plan;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.util.DirectiveValues;
import org.tap4j.util.StatusValues;

import hudson.tasks.test.TestResultParser;

public class AbstractTapParser extends TestResultParser {

    /**
     * Prints the logs to the web server's console log files.
     * */
    private static final Logger LOGGER = Logger.getLogger(AbstractTapParser.class.getName());

    protected final Boolean outputTapToConsole;
    protected final Boolean enableSubtests;
    protected final Boolean todoIsFailure;

    protected final Boolean includeCommentDiagnostics;
    protected final Boolean validateNumberOfTests;
    protected final Boolean planRequired;
    protected final Boolean verbose;
    protected final Boolean stripSingleParents;
    protected final Boolean flattenTheTap;

    protected boolean hasFailedTests;
    protected boolean parserErrors;

    protected final Boolean failIfNoResults;
    protected final Boolean discardOldReports;

    private final PrintStream logger;

    public AbstractTapParser(Boolean failIfNoResults, Boolean discardOldReports, Boolean outputTapToConsole,
            Boolean enableSubtests, Boolean todoIsFailure, Boolean includeCommentDiagnostics,
            Boolean validateNumberOfTests, Boolean planRequired, Boolean verbose, Boolean stripSingleParents,
            Boolean flattenTapResult, PrintStream logger) {
        this.failIfNoResults = failIfNoResults;
        this.discardOldReports = discardOldReports;

        this.outputTapToConsole = outputTapToConsole;
        this.enableSubtests = enableSubtests;
        this.todoIsFailure = todoIsFailure;
        this.parserErrors = false;
        this.includeCommentDiagnostics = includeCommentDiagnostics;
        this.validateNumberOfTests = validateNumberOfTests;
        this.planRequired = planRequired;
        this.verbose = verbose;
        this.stripSingleParents = stripSingleParents;
        this.flattenTheTap = flattenTapResult;
        this.logger = logger;
    }

    @Override
    public String getDisplayName() {
        return "TAP 13 Parser";
    }

    @Override
    public String getTestResultLocationMessage() {
        return "TAP files:";
    }

    /**
     * Iterates through the list of test sets and validates its plans and test results.
     *
     * @param testSets
     * @return {@code true} if there are any test case that doesn't follow the plan
     */
    protected boolean validateNumberOfTests(List<TestSetMap> testSets) {
        for (TestSetMap testSetMap : testSets) {
            TestSet testSet = testSetMap.getTestSet();
            Plan plan = testSet.getPlan();
            if (plan != null) {
                int planned = plan.getLastTestNumber();
                int numberOfTests = testSet.getTestResults().size();
                if (planned != numberOfTests)
                    return false;
            }
        }
        return true;
    }

    protected TestSet stripSingleParentsAsRequired(TestSet originalSet) {
        if (!stripSingleParents) {
            return originalSet;
        } else {
            TestSet result = originalSet;
            while (hasSingleParent(result)) {
                result = result.getTestResults().get(0).getSubtest();
            }
            return result;
        }
    }

    protected TestSet flattenTheSetAsRequired(TestSet originalSet) {
        if (!flattenTheTap) {
            return originalSet;
        } else {
            TestSet result = new TestSet();
            final List<org.tap4j.model.TestResult> resultsToProcess = originalSet.getTestResults();
            int testIndex = 1;
            while (!resultsToProcess.isEmpty()) {
                final org.tap4j.model.TestResult actualTestResult = resultsToProcess.remove(0);
                TestSet subtests = actualTestResult.getSubtest();
                if (subtests == null || subtests.getNumberOfTestResults() == 0) {
                    actualTestResult.setTestNumber(testIndex++);
                    result.addTestResult(actualTestResult);
                } else {
                    final List<org.tap4j.model.TestResult> subtestResults = subtests.getTestResults();
                    for (org.tap4j.model.TestResult subtestResult : subtestResults) {
                        subtestResult
                                .setDescription(actualTestResult.getDescription() + subtestResult.getDescription());
                        resultsToProcess.add(subtestResult);
                    }

                    final Plan subtestPlan = subtests.getPlan();
                    final boolean planIsPresent = subtestPlan != null;
                    final int subtestCountAsPlanned = planIsPresent
                            ? subtestPlan.getLastTestNumber() - subtestPlan.getInitialTestNumber() + 1 : -1;

                    final boolean subtestCountDiffersFromPlan = planIsPresent
                            && subtestCountAsPlanned != subtestResults.size();

                    if (subtestCountDiffersFromPlan) {

                        final int missingTestCount = subtestCountAsPlanned - subtestResults.size();

                        final org.tap4j.model.TestResult timeoutTestResult = new org.tap4j.model.TestResult();
                        timeoutTestResult.setStatus(StatusValues.NOT_OK);
                        timeoutTestResult.setDescription(String.format("%s %s %d %s", actualTestResult.getDescription(),
                                "failed:", missingTestCount, "subtest(s) missing"));

                        resultsToProcess.add(timeoutTestResult);
                    }
                }
            }
            return result;
        }
    }

    protected boolean containsNotOk(TestSet testSet) {
        for (org.tap4j.model.TestResult testResult : testSet.getTestResults()) {
            if (testResult.getStatus().equals(StatusValues.NOT_OK) && !(testResult.getDirective() != null
                    && DirectiveValues.SKIP == testResult.getDirective().getDirectiveValue())) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasSingleParent(TestSet testSet) {

        if (testSet == null) {
            return false;
        }

        if (testSet.getNumberOfTestResults() != 1) {
            return false; // not a single test result
        }

        int planSpan = testSet.getPlan() != null
                ? (testSet.getPlan().getLastTestNumber() - testSet.getPlan().getInitialTestNumber()) : 0;

        if (planSpan == 0) { // exactly one test
            return testSet.getTestResults().get(0).getSubtest() != null; // which
                                                                         // has
                                                                         // a
                                                                         // child(ern)
        } else {
            return false;
        }
    }

    protected void log(String str) {
        if (verbose && logger != null) {
            logger.println(str);
        } else {
            LOGGER.fine(str);
        }
    }

    protected void log(Exception ex) {
        if (logger != null) {
            ex.printStackTrace(logger);
        } else {
            LOGGER.severe(ex.toString());
        }
    }
}
