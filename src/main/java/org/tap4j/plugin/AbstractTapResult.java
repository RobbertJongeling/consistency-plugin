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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.export.Exported;
import org.tap4j.model.BailOut;
import org.tap4j.model.Comment;
import org.tap4j.model.Directive;
import org.tap4j.model.Plan;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.model.ParseErrorTestSetMap;
import org.tap4j.plugin.model.TapAttachment;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.plugin.util.Constants;
import org.tap4j.plugin.util.DiagnosticUtil;
import org.tap4j.util.DirectiveValues;
import org.tap4j.util.StatusValues;

import hudson.model.ModelObject;
import hudson.tasks.test.TestResult;

public abstract class AbstractTapResult extends TestResult implements ModelObject, Serializable {

    int failed = 0;
    int passed = 0;
    int skipped = 0;
    int bailOuts = 0;
    int total = 0;
    float duration = 0.0f;

    private final String name;

    protected final Boolean todoIsFailure;
    protected final Boolean includeCommentDiagnostics;
    protected final Boolean validateNumberOfTests;
    protected final Boolean showOnlyFailures;

    protected final Boolean discardOldReports;
    protected final Boolean outputTapToConsole;

    protected final Boolean hasFailedTests;
    protected final Boolean parserErrors;

    public AbstractTapResult(long buildTime, DirectoryScanner results, Boolean discardOldReports,
            Boolean outputTapToConsole) {
        super();
        this.discardOldReports = discardOldReports;
        this.outputTapToConsole = outputTapToConsole;
        parse(buildTime, results);
    }

    abstract void parse(long buildTime, DirectoryScanner results) throws IOException;

    public int getFailed() {
        return this.failed;
    }

    public int getSkipped() {
        return this.skipped;
    }

    public int getPassed() {
        return this.passed;
    }

    public int getBailOuts() {
        return this.bailOuts;
    }

    public int getTotal() {
        return this.total;
    }

    public float getDuration() {
        return this.duration;
    }

    @Exported(visibility = 999)
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    public Boolean getShowOnlyFailures() {
        return BooleanUtils.toBooleanDefaultIfNull(showOnlyFailures, Boolean.FALSE);
    }

    public Boolean getTodoIsFailure() {
        return todoIsFailure;
    }

    public Boolean getIncludeCommentDiagnostics() {
        return (includeCommentDiagnostics == null) ? true : includeCommentDiagnostics;
    }

    public Boolean getValidateNumberOfTests() {
        return (validateNumberOfTests == null) ? false : validateNumberOfTests;
    }

    // --- utility methods

    /**
     * Called from TapResult/index.jelly
     */
    public String createDiagnosticTable(String tapFile, Map<String, Object> diagnostic) {
        return DiagnosticUtil.createDiagnosticTable(tapFile, diagnostic);
    }

    public boolean isTestResult(Object tapResult) {
        return (tapResult != null && tapResult instanceof org.tap4j.model.TestResult);
    }

    public boolean isBailOut(Object tapResult) {
        return (tapResult != null && tapResult instanceof BailOut);
    }

    public boolean isComment(Object tapResult) {
        return (tapResult != null && tapResult instanceof Comment);
    }

    public String escapeHTML(String html) {
        return StringUtils.replaceEach(html, new String[] { "&", "\"", "<", ">" },
                new String[] { "&amp;", "&quot;", "&lt;", "&gt;" });
    }

    private TapAttachment getAttachment(TestSet ts, String key) {
        for (org.tap4j.model.TestResult tr : ts.getTestResults()) {
            Map<String, Object> diagnostics = tr.getDiagnostic();
            if (diagnostics != null && diagnostics.size() > 0) {
                TapAttachment attachement = recursivelySearch(diagnostics, null, key);
                if (attachement != null) {
                    return attachement;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private TapAttachment recursivelySearch(Map<String, Object> diagnostics, String parentKey, String key) {
        for (String diagnosticKey : diagnostics.keySet()) {
            Object value = diagnostics.get(diagnosticKey);
            if (value != null) {
                if (value instanceof Map<?, ?>) {
                    TapAttachment attachment = recursivelySearch((Map<String, Object>) value, diagnosticKey, key);
                    if (attachment != null) {
                        return attachment;
                    }
                } else {
                    if (parentKey != null && parentKey.equals(key)) {
                        Object o = diagnostics.get("File-Content");
                        if (o == null)
                            o = diagnostics.get("File-content");
                        if (o != null && o instanceof String)
                            return new TapAttachment(Base64.decodeBase64((String) o), diagnostics);
                    } else if (diagnosticKey.equalsIgnoreCase("file-name") && value.equals(key)) {
                        Object o = diagnostics.get("File-Content");
                        if (o == null)
                            o = diagnostics.get("File-content");
                        if (o != null && o instanceof String)
                            return new TapAttachment(Base64.decodeBase64((String) o), diagnostics);
                    }
                }
            }
        }
        return null;
    }

    public boolean isSkipped(org.tap4j.model.TestResult testResult) {
        boolean r = false;
        Directive directive = testResult.getDirective();
        if (directive != null && directive.getDirectiveValue() == DirectiveValues.SKIP) {
            r = true;
        }
        return r;
    }

    public boolean isFailure(org.tap4j.model.TestResult testResult, Boolean todoIsFailure) {
        boolean r = false;
        Directive directive = testResult.getDirective();
        StatusValues status = testResult.getStatus();
        if (directive != null) {
            if (directive.getDirectiveValue() == DirectiveValues.TODO && todoIsFailure != null
                    && true == todoIsFailure) {
                r = true;
            }
        } else if (status != null && status == StatusValues.NOT_OK) {
            r = true;
        }
        return r;
    }

    /**
     * Normalizes a folder path in relation to the workspace path.
     * <p>
     * A folder that is subdirectory of workspace will return only the difference. It means that if the workspace is
     * /home/workspace and the folder we want to normalize is /home/workspace/job-1/test.txt, then the return will be
     * job-1/test.txt.
     *
     * @param workspace workspace path
     * @param relative relative path
     * @return normalized path
     */
    public static String normalizeFolders(String workspace, String relative) {
        workspace = workspace.replaceAll("\\\\", "\\/");
        relative = relative.replaceAll("\\\\", "\\/");
        if (relative.length() > workspace.length() && relative.contains(workspace)) {
            String temp = relative.substring(workspace.length(), relative.length());
            if (temp.startsWith("/") || temp.startsWith("\\"))
                temp = temp.substring(1, temp.length());
            return temp;
        }
        return relative;
    }

    /**
     * @param testSets Unfiltered test sets
     * @return Test sets that didn't fail to parse
     */
    private List<TestSetMap> filterTestSet(List<TestSetMap> testSets) {
        final List<TestSetMap> filtered = new ArrayList<TestSetMap>();
        for (TestSetMap testSet : testSets) {
            if (testSet instanceof ParseErrorTestSetMap == false) {
                String rootDir = build.getRootDir().getAbsolutePath();
                try {
                    rootDir = new File(build.getRootDir().getCanonicalPath().toString(), Constants.TAP_DIR_NAME)
                            .getAbsolutePath();
                } catch (IOException e) {
                    LOGGER.warning(e.getMessage());
                }
                filtered.add(new TestSetMap(normalizeFolders(rootDir, testSet.getFileName()), testSet.getTestSet()));
            }
        }
        return filtered;
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

    protected void log(String str) {
        if (verbose && logger != null) {
            logger.println(str);
        } else {
            log.fine(str);
        }
    }

    protected void log(Exception ex) {
        if (logger != null) {
            ex.printStackTrace(logger);
        } else {
            log.severe(ex.toString());
        }
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

                        final org.tap4j.model.TestResult timeoutTestResult = new TestResult();
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
            return testSet.getTestResults().get(0).getSubtest() != null; // which has a child(ern)
        } else {
            return false;
        }
    }

}
