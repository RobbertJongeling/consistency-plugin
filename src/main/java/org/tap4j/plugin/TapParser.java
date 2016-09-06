/*
 * The MIT License
 *
 * Copyright (c) 2010-2016 Bruno P. Kinoshita
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
import java.io.PrintStream;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

/**
 * Executes remote TAP Stream retrieval and execution.
 *
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.1
 */
public class TapParser extends AbstractTapParser {

    public TapParser(Boolean failIfNoResults, Boolean discardOldReports, Boolean outputTapToConsole,
            Boolean enableSubtests, Boolean todoIsFailure, Boolean includeCommentDiagnostics,
            Boolean validateNumberOfTests, Boolean planRequired, Boolean verbose, Boolean stripSingleParents,
            Boolean flattenTapResult,PrintStream logger) {
        super(failIfNoResults, discardOldReports, outputTapToConsole, enableSubtests, todoIsFailure, includeCommentDiagnostics,
                validateNumberOfTests, planRequired, verbose, stripSingleParents, flattenTapResult, logger);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TapResult parseResult(String testResultLocations, Run<?, ?> build, FilePath workspace, Launcher launcher,
            TaskListener listener) throws IOException, InterruptedException {
        final long buildTime = build.getTimestamp().getTimeInMillis();
        final long timeOnMaster = System.currentTimeMillis();

        return workspace
                .act(new ParseResultCallable(this.failIfNoResults, testResultLocations, buildTime, timeOnMaster));
    }

    /**
     * TAP parser callable, for distributed environments.
     *
     * @since 2.1
     */
    private static final class ParseResultCallable extends MasterToSlaveFileCallable<TapResult> {

        private static final long serialVersionUID = -8155885659619524893L;

        private final Boolean allowEmptyResults;
        private final Boolean discardOldReports;
        private final Boolean enableSubtests;
        private final Boolean planRequired;
        private Boolean outputTapToConsole;
        private final String testResults;
        private final long buildTime;
        private final long nowMaster;

        public ParseResultCallable(Boolean allowEmptyResults, Boolean discardOldReports, Boolean enableSubtests,
                Boolean planRequired, Boolean outputTapToConsole, String testResults, long buildTime, long timeOnMaster) {
            this.allowEmptyResults = allowEmptyResults;
            this.discardOldReports = discardOldReports;
            this.enableSubtests = enableSubtests;
            this.planRequired = planRequired;
            this.outputTapToConsole = outputTapToConsole;
            this.testResults = testResults;
            this.buildTime = buildTime;
            this.nowMaster = timeOnMaster;
        }

        @Override
        public TapResult invoke(File ws, VirtualChannel channel) throws IOException, InterruptedException {
            final long nowSlave = System.currentTimeMillis();

            FileSet fs = Util.createFileSet(ws, testResults);
            DirectoryScanner ds = fs.getDirectoryScanner();
            TapResult result = null;

            String[] files = ds.getIncludedFiles();
            if (files.length > 0) {
                result = new TapResult(buildTime + (nowSlave - nowMaster), ds, discardOldReports, enableSubtests,
                        planRequired, outputTapToConsole);
                result.tally();
            } else {
                if (this.allowEmptyResults) {
                    result = new TapResult();
                } else {
                    // no test result. Most likely a configuration
                    // error or fatal problem
                    throw new AbortException("No TAP files found");
                }
            }
            return result;
        }

    }

}
