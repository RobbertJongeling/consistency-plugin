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
import org.tap4j.parser.ParserException;
import org.tap4j.parser.Tap13Parser;
import org.tap4j.plugin.model.CaseResult;
import org.tap4j.plugin.model.SuiteResult;

import hudson.AbortException;
import hudson.Util;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;

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
     * Test suite.
     */
    private final List<SuiteResult> suites = new ArrayList<SuiteResult>();

    private transient Tap13Parser parser;

    public TapResult() {
        super();
    }

    public TapResult(long buildTime, DirectoryScanner results, Boolean discardOldReports, Boolean enableSubtests,
            Boolean planRequired, Boolean outputTapToConsole) throws IOException {
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
            throw new AbortException(
                    String.format(
                            "Test reports were found but none of them are new. Did tests run? %n"
                                    + "For example, %s is %s old%n",
                            f, Util.getTimeSpanString(buildTime - f.lastModified())));
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
            final SuiteResult suite = flattenTheSetAsRequired(stripSingleParentsAsRequired(parser.parseFile(reportFile)));

            if (containsNotOk(testSet) || testSet.containsBailOut()) {
                this.hasFailedTests = Boolean.TRUE;
            }


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
            SuiteResult sr = new SuiteResult(reportFile);
            sr.addCase(new CaseResult(sr, 0, "[empty]", String.format("Error parsing the TAP file %s", reportFile), todoIsFailure));
            add(sr);
            this.hasParserErrors = Boolean.TRUE;
            log(pe);
        }
    }

    private void add(SuiteResult sr) {
        this.suites.add(sr);
        this.duration += sr.getDuration();
    }

    @Override
    public void tally() {
        failed = 0;
        passed = 0;
        skipped = 0;
        bailOuts = 0;
        total = 0;
        duration = 0.0f;

        for (SuiteResult suite : suites) {
            final List<CaseResult> cases = suite.getCases();

            total += cases.size();

            Plan plan = suite.getPlan();

            if (plan != null && plan.isSkip()) {
                this.skipped += cases.size();
            } else {
                for (CaseResult caseResult : cases) {
                    if (caseResult.isSkipped()) {
                        skipped += 1;
                    } else if (caseResult.isFailure()) {
                        failed += 1;
                    } else {
                        passed += 1;
                    }
                    // FIXME: code duplication. Refactor it and
                    // TapTestResultResult
                    Map<String, Object> diagnostics = caseResult.getDiagnostics();
                    if (diagnostics != null && !diagnostics.isEmpty()) {
                        Object duration = diagnostics.get(DURATION_KEY);
                        if (duration != null) {
                            Float durationMS = Float.parseFloat(duration.toString());
                            this.duration += durationMS;
                        }
                    }
                }
            }

            this.bailOuts += suite.countOfBailOuts();
        }
    }

    @Override
    public TestResult findCorrespondingResult(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TestObject getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Iterates through the list of test sets and validates its plans and test results.
     *
     * @param testSets
     * @return <true> if there are any test case that doesn't follow the plan
     */
    public boolean validateNumberOfTests() {
        boolean valid = true;
        for (SuiteResult suite : suites) {
            Plan plan = suite.getPlan();
            if (plan != null) {
                int planned = plan.getLastTestNumber();
                int numberOfTests = suite.getCases().size();
                if (planned != numberOfTests) {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }

}
