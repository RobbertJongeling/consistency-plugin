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

import java.io.IOException;
import java.io.PrintStream;

import javax.annotation.Nonnull;

import org.apache.commons.lang.BooleanUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.test.TestResultAggregator;
import jenkins.tasks.SimpleBuildStep;

/**
 * Publishes TAP results in Jenkins builds.
 *
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapPublisher extends Recorder implements MatrixAggregatable, SimpleBuildStep {
    /**
     * TAP ant-pattern to find reports
     */
    private final String testResults;
    /**
     * Fail the job, if there are no files matching the ant-pattern
     */
    private final Boolean failIfNoResults;
    /**
     * If there is a failed test, the build is marked as failure.
     */
    private final Boolean failedTestsMarkBuildAsFailure;
    /**
     * Output the TAP streams found, to the build console.
     */
    private final Boolean outputTapToConsole;
    /**
     * Enable subtests.
     */
    private final Boolean enableSubtests;
    /**
     * Discard old reports.
     */
    private final Boolean discardOldReports;
    /**
     * TO DOs are treated as failures.
     */
    private final Boolean todoIsFailure;
    /**
     * Include comment as diagnostics.
     */
    private final Boolean includeCommentDiagnostics;
    /**
     * Flag to tell the parser to validate the number of tests as given in the test plan.
     */
    private final Boolean validateNumberOfTests;
    /**
     * Flag to tell the parser that Test Plans are required.
     */
    private final Boolean planRequired;
    /**
     * Enable verbose model.
     */
    private final Boolean verbose;
    /**
     * Display only test failures.
     */
    private final Boolean showOnlyFailures;
    /**
     * Remove single parents.
     */
    private final Boolean stripSingleParents;
    /**
     * Flatten tap result. All subtests are merged with the parents.
     */
    private final Boolean flattenTapResult;
    /**
     * Skip the publisher if the build status is not OK (worse than unstable)
     */
    private final Boolean skipIfBuildNotOk;

    // --- constructors and serialization

    @DataBoundConstructor
    public TapPublisher(String testResults, Boolean failIfNoResults, Boolean failedTestsMarkBuildAsFailure,
            Boolean outputTapToConsole, Boolean enableSubtests, Boolean discardOldReports, Boolean todoIsFailure,
            Boolean includeCommentDiagnostics, Boolean validateNumberOfTests, Boolean planRequired, Boolean verbose,
            Boolean showOnlyFailures, Boolean stripSingleParents, Boolean flattenTapResult, Boolean skipIfBuildNotOk) {
        // The default values in these utility methods represent the default
        // value for old behaviour, in order to keep backward compatibility
        this.testResults = testResults;
        this.failIfNoResults = BooleanUtils.toBooleanDefaultIfNull(failIfNoResults, false);
        this.failedTestsMarkBuildAsFailure = BooleanUtils.toBooleanDefaultIfNull(failedTestsMarkBuildAsFailure, false);
        this.outputTapToConsole = BooleanUtils.toBooleanDefaultIfNull(outputTapToConsole, true);
        this.enableSubtests = BooleanUtils.toBooleanDefaultIfNull(enableSubtests, true);
        this.discardOldReports = BooleanUtils.toBooleanDefaultIfNull(discardOldReports, false);
        this.todoIsFailure = BooleanUtils.toBooleanDefaultIfNull(todoIsFailure, true);
        this.includeCommentDiagnostics = BooleanUtils.toBooleanDefaultIfNull(includeCommentDiagnostics, true);
        this.validateNumberOfTests = BooleanUtils.toBooleanDefaultIfNull(validateNumberOfTests, false);
        this.planRequired = BooleanUtils.toBooleanDefaultIfNull(planRequired, true);
        this.verbose = BooleanUtils.toBooleanDefaultIfNull(verbose, true);
        this.showOnlyFailures = BooleanUtils.toBooleanDefaultIfNull(showOnlyFailures, false);
        this.stripSingleParents = BooleanUtils.toBooleanDefaultIfNull(stripSingleParents, false);
        this.flattenTapResult = BooleanUtils.toBooleanDefaultIfNull(flattenTapResult, false);
        this.skipIfBuildNotOk = BooleanUtils.toBooleanDefaultIfNull(skipIfBuildNotOk, false);
    }

    /**
     * Called when reading objects from disk. In order to keep backward compatibility with other {@link TapPublisher}
     * objects, which may not have all attributes, here we set a default value if null.
     *
     * @return {@link TapResult}
     */
    public Object readResolve() {
        final String _testResults = this.getTestResults();
        final Boolean _failIfNoResults = BooleanUtils.toBooleanDefaultIfNull(this.getFailIfNoResults(), false);
        final Boolean _failedTestsMarkBuildAsFailure = BooleanUtils
                .toBooleanDefaultIfNull(this.getFailedTestsMarkBuildAsFailure(), false);
        final Boolean _outputTapToConsole = BooleanUtils.toBooleanDefaultIfNull(this.getOutputTapToConsole(), false);
        final Boolean _enableSubtests = BooleanUtils.toBooleanDefaultIfNull(this.getEnableSubtests(), true);
        final Boolean _discardOldReports = BooleanUtils.toBooleanDefaultIfNull(this.getDiscardOldReports(), false);
        final Boolean _todoIsFailure = BooleanUtils.toBooleanDefaultIfNull(this.getTodoIsFailure(), true);
        final Boolean _includeCommentDiagnostics = BooleanUtils
                .toBooleanDefaultIfNull(this.getIncludeCommentDiagnostics(), true);
        final Boolean _validateNumberOfTests = BooleanUtils.toBooleanDefaultIfNull(this.getValidateNumberOfTests(),
                false);
        final Boolean _planRequired = BooleanUtils.toBooleanDefaultIfNull(this.getPlanRequired(), true);
        final Boolean _verbose = BooleanUtils.toBooleanDefaultIfNull(this.getVerbose(), true);
        final Boolean _showOnlyFailures = BooleanUtils.toBooleanDefaultIfNull(this.getShowOnlyFailures(), false);
        final Boolean _stripSingleParents = BooleanUtils.toBooleanDefaultIfNull(this.getStripSingleParents(), false);
        final Boolean _flattenTapResult = BooleanUtils.toBooleanDefaultIfNull(this.getFlattenTapResult(), false);
        final Boolean _skipIfBuildNotOk = BooleanUtils.toBooleanDefaultIfNull(this.getSkipIfBuildNotOk(), false);

        return new TapPublisher(_testResults, _failIfNoResults, _failedTestsMarkBuildAsFailure, _outputTapToConsole,
                _enableSubtests, _discardOldReports, _todoIsFailure, _includeCommentDiagnostics, _validateNumberOfTests,
                _planRequired, _verbose, _showOnlyFailures, _stripSingleParents, _flattenTapResult, _skipIfBuildNotOk);
    }

    // --- getters

    public Boolean getShowOnlyFailures() {
        return this.showOnlyFailures;
    }

    public Boolean getStripSingleParents() {
        return this.stripSingleParents;
    }

    public Boolean getFailIfNoResults() {
        return failIfNoResults;
    }

    public String getTestResults() {
        return testResults;
    }

    public Boolean getFailedTestsMarkBuildAsFailure() {
        return failedTestsMarkBuildAsFailure;
    }

    public Boolean getOutputTapToConsole() {
        return outputTapToConsole;
    }

    public Boolean getEnableSubtests() {
        return enableSubtests;
    }

    public Boolean getDiscardOldReports() {
        return discardOldReports;
    }

    public Boolean getTodoIsFailure() {
        return todoIsFailure;
    }

    public Boolean getIncludeCommentDiagnostics() {
        return includeCommentDiagnostics;
    }

    public Boolean getValidateNumberOfTests() {
        return validateNumberOfTests;
    }

    public Boolean getPlanRequired() {
        return planRequired;
    }

    public Boolean getVerbose() {
        return verbose;
    }

    public Boolean getFlattenTapResult() {
        return flattenTapResult;
    }

    public Boolean getSkipIfBuildNotOk() {
        return skipIfBuildNotOk;
    }

    // --- plugin logic

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new TapProjectAction(project);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
            @Nonnull TaskListener listener) throws InterruptedException, IOException {
        performImpl(run, workspace, launcher, listener);
    }

    /**
     * Execute the plug-in code.
     *
     * @param build project build
     * @param workspace project workspace
     * @param launcher Jenkins launcher
     * @param listener job listener
     * @return {@code true} if the job succeeded.
     * @throws IOException
     * @throws InterruptedException
     */
    private void performImpl(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException {
        final PrintStream logger = listener.getLogger();
        if (isPerformPublisher(build)) {
            logger.println("TAP Reports Processing: START");

            final String testResults = build.getEnvironment(listener).expand(this.testResults);
            logger.println("Looking for TAP results report in workspace using pattern: " + testResults);

            final TapResult result = parse(testResults, build, workspace, launcher, listener);

            TapTestResultAction trAction = build.getAction(TapTestResultAction.class);
            boolean appending;

            if (trAction == null) {
                appending = false;
                trAction = new TapTestResultAction(build, result);
            } else {
                appending = true;
                trAction.mergeResult(result);
            }

            if (!appending) {
                build.addAction(trAction);
            }

            if (result.isEmpty()) {
                if (build.getResult() == Result.FAILURE) {
                    // most likely a build failed before it gets to the test phase.
                    // don't report confusing error message.
                    return;
                }
                if (!this.failIfNoResults) {
                    logger.println("Test Result is empty");
                    return;
                }

                throw new AbortException("Test Result is empty");
            }

            // create an individual report for all of the results and add it
            // to the build

            TapBuildAction action = build.getAction(TapBuildAction.class);
            if (action == null) {
                action = new TapBuildAction(build, result);
                build.addAction(action);
            } else {
                appending = true;
                action.mergeResult(result);
            }

            if (result.hasParseErrors()) {
                listener.getLogger().println("TAP parse errors found in the build. Marking build as UNSTABLE");
                build.setResult(Result.UNSTABLE);
                return;
            }
            if (this.getValidateNumberOfTests()) {
                if (!result.validateNumberOfTests()) {
                    listener.getLogger().println(
                            "Not all test cases were executed according to the test set plan. Marking build as UNSTABLE");
                    build.setResult(Result.UNSTABLE);
                }
            }
            if (result.getFailed() > 0) {
                if (!this.getFailedTestsMarkBuildAsFailure()) {
                    listener.getLogger().println("There are failed test cases. Marking build as UNSTABLE");
                    build.setResult(Result.UNSTABLE);
                }
                throw new AbortException(
                        "There are failed test cases and the job is configured to mark the build as failure. Marking build as FAILURE");
            }

            if (appending) {
                build.save();
            }
            logger.println("TAP Reports Processing: FINISH");
        } else {
            logger.println("Build result is not better or equal unstable. Skipping TAP publisher.");
        }
    }

    /**
     * Return {@code true} if the build is ongoing, if the user did not ask to fail when failed, or otherwise if the
     * build result is not better or equal to unstable.
     *
     * @param build Run
     * @return whether to perform the publisher or not, based on user provided configuration
     */
    private boolean isPerformPublisher(Run<?, ?> build) {
        final Result result = build.getResult();
        // may be null if build is ongoing
        if (result == null) {
            return true;
        }

        if (!getSkipIfBuildNotOk()) {
            return true;
        }

        return result.isBetterOrEqualTo(Result.UNSTABLE);
    }

    private TapResult parse(String testResults, Run<?, ?> build, FilePath workspace, Launcher launcher,
            TaskListener listener) throws IOException, InterruptedException {
        return new TapParser(failIfNoResults, discardOldReports, outputTapToConsole, enableSubtests, todoIsFailure,
                includeCommentDiagnostics, validateNumberOfTests, planRequired, verbose, stripSingleParents,
                flattenTapResult, listener.getLogger()).parseResult(testResults, build, workspace, launcher, listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see hudson.tasks.BuildStep#getRequiredMonitorService()
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    // matrix jobs and test result aggregation support

    /*
     * (non-Javadoc)
     *
     * @see hudson.matrix.MatrixAggregatable#createAggregator(hudson.matrix. MatrixBuild, hudson.Launcher,
     * hudson.model.BuildListener)
     */
    @Override
    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new TestResultAggregator(build, launcher, listener);
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            super(TapPublisher.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Publish TAP Results";
        }

        /*
         * (non-Javadoc)
         *
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         */
        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return Boolean.TRUE;
        }

    }
}
