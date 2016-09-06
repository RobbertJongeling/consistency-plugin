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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.tap4j.model.Plan;
import org.tap4j.model.TestSet;
import org.tap4j.parser.ParserException;
import org.tap4j.parser.Tap13Parser;
import org.tap4j.plugin.model.ParseErrorTestSetMap;
import org.tap4j.plugin.model.TestSetMap;

import hudson.AbortException;
import hudson.Util;

/**
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapResult extends AbstractTapResult {
    /**
     * serial version UID.
     */
    private static final long serialVersionUID = 8916483249538062001L;
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TapResult.class.getName());
    /**
     * Key used to indicate duration in milli seconds.
     */
    private static final String DURATION_KEY = "duration_ms";
    /**
     * TAP Test Sets. At least one per file.
     */
    private final List<TestSet> testSets = new ArrayList<TestSet>();

    private final Tap13Parser parser;

    public TapResult(long buildTime, DirectoryScanner results, Boolean discardOldReports, Boolean enableSubtests,
            Boolean planRequired, Boolean outputTapToConsole) {
        super(buildTime, results, discardOldReports, outputTapToConsole);
        parser = new Tap13Parser("UTF-8", enableSubtests, planRequired);
        parse(buildTime, results);
    }

    @Override
    void parse(long buildTime, DirectoryScanner results) throws IOException {
        String[] includedFiles = results.getIncludedFiles();
        File baseDir = results.getBasedir();
        parse(buildTime, baseDir, includedFiles);
    }

    private void parse(long buildTime, File baseDir, String[] reportFiles) throws IOException {
        boolean parsed = false;

        for (String value : reportFiles) {
            File reportFile = new File(baseDir, value);
            // only count files that were actually updated during this build
            if (buildTime - 3000/* error margin */ <= reportFile.lastModified()) {
                parsePossiblyEmpty(reportFile);
                parsed = true;
            }
        }

        if (!parsed) {
            long localTime = System.currentTimeMillis();
            if (localTime < buildTime - 1000) /* margin */
                // build time is in the the future. clock on this slave must be running behind
                throw new AbortException("Clock on this slave is out of sync with the master, and therefore \n"
                        + "I can't figure out what test results are new and what are old.\n"
                        + "Please keep the slave clock in sync with the master.");

            File f = new File(baseDir, reportFiles[0]);
            throw new AbortException(String.format("Test reports were found but none of them are new. Did tests run? %n"
                    + "For example, %s is %s old%n", f, Util.getTimeSpanString(buildTime - f.lastModified())));
        }
    }

    private void parsePossiblyEmpty(File reportFile) {
        // As in JUnit plug-in comment, this may happen when the JVM crashes
        if (reportFile.length() == 0) {

        } else {
            parse(reportFile);
        }
    }

    private void parse(File reportFile) {
        try {
            final TestSet testSet = flattenTheSetAsRequired(stripSingleParentsAsRequired(parser.parseFile(reportFile)));

            if (containsNotOk(testSet) || testSet.containsBailOut()) {
                this.hasFailedTests = Boolean.TRUE;
            }

            final TestSetMap map = new TestSetMap(reportFile.getAbsolutePath(), testSet);
            testSets.add(map);

            if (this.outputTapToConsole) {
                try {
                    log(FileUtils.readFileToString(reportFile));
                } catch (RuntimeException re) {
                    log(re);
                } catch (IOException e) {
                    log(e);
                }
            }
        } catch (ParserException pe) {
            testSets.add(new ParseErrorTestSetMap(reportFile.getAbsolutePath(), pe));
            this.hasParserErrors = Boolean.TRUE;
            log(pe);
        }
    }

    public void tally() {
        failed = 0;
        passed = 0;
        skipped = 0;
        bailOuts = 0;
        total = 0;
        duration = 0.0f;

        for (TestSetMap testSet : testSets) {
            TestSet realTestSet = testSet.getTestSet();
            List<org.tap4j.model.TestResult> testResults = realTestSet.getTestResults();

            total += testResults.size();

            Plan plan = realTestSet.getPlan();

            if (plan != null && plan.isSkip()) {
                this.skipped += testResults.size();
            } else {
                for (org.tap4j.model.TestResult testResult : testResults) {
                    if (isSkipped(testResult)) {
                        skipped += 1;
                    } else if (isFailure(testResult, todoIsFailure)) {
                        failed += 1;
                    } else {
                        passed += 1;
                    }
                    // FIXME: code duplication. Refactor it and
                    // TapTestResultResult
                    Map<String, Object> diagnostic = testResult.getDiagnostic();
                    if (diagnostic != null && !diagnostic.isEmpty()) {
                        Object duration = diagnostic.get(DURATION_KEY);
                        if (duration != null) {
                            Float durationMS = Float.parseFloat(duration.toString());
                            this.duration += durationMS;
                        }
                    }
                }
            }

            this.bailOuts += realTestSet.getNumberOfBailOuts();
        }
    }

}
