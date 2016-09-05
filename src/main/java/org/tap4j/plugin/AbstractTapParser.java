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

import java.util.List;

import org.tap4j.model.Plan;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.util.DirectiveValues;
import org.tap4j.util.StatusValues;

import hudson.tasks.test.TestResult;
import hudson.tasks.test.TestResultParser;

public class AbstractTapParser extends TestResultParser {

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

    private TestSet stripSingleParentsAsRequired(TestSet originalSet) {
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

    private TestSet flattenTheSetAsRequired(TestSet originalSet) {
        if (!flattenTheTap) {
            return originalSet;
        } else {
            TestSet result = new TestSet();
            final List<TestResult> resultsToProcess = originalSet.getTestResults();
            int testIndex = 1;
            while (!resultsToProcess.isEmpty()) {
                final TestResult actualTestResult = resultsToProcess.remove(0);
                TestSet subtests = actualTestResult.getSubtest();
                if (subtests == null || subtests.getNumberOfTestResults() == 0) {
                    actualTestResult.setTestNumber(testIndex++);
                    result.addTestResult(actualTestResult);
                } else {
                    final List<TestResult> subtestResults = subtests.getTestResults();
                    for (TestResult subtestResult : subtestResults) {
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

                        final TestResult timeoutTestResult = new TestResult();
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

    private boolean containsNotOk(TestSet testSet) {
        for (TestResult testResult : testSet.getTestResults()) {
            if (testResult.getStatus().equals(StatusValues.NOT_OK) && !(testResult.getDirective() != null
                    && DirectiveValues.SKIP == testResult.getDirective().getDirectiveValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSingleParent(TestSet testSet) {

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

    private void log(String str) {
        if (verbose && logger != null) {
            logger.println(str);
        } else {
            log.fine(str);
        }
    }

    private void log(Exception ex) {
        if (logger != null) {
            ex.printStackTrace(logger);
        } else {
            log.severe(ex.toString());
        }
    }
}
