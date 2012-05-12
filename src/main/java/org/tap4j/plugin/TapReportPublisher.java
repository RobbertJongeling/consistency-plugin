/*
 * The MIT License
 *
 * Copyright (c) <2012> <Bruno P. Kinoshita>
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

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.tap4j.plugin.model.TestSetMap;

/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 2.0
 */
@SuppressWarnings("deprecation")
public class TapReportPublisher extends TestDataPublisher {

	private final String testResults;
	private final Boolean failedTestsMarkBuildAsFailure;

	@DataBoundConstructor
	public TapReportPublisher(String testResults,
			Boolean failedTestsMarkBuildAsFailure) {
		this.testResults = testResults;
		this.failedTestsMarkBuildAsFailure = failedTestsMarkBuildAsFailure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * hudson.tasks.junit.TestDataPublisher#getTestData(hudson.model.AbstractBuild
	 * , hudson.Launcher, hudson.model.BuildListener,
	 * hudson.tasks.junit.TestResult)
	 */
	@Override
	public TapData getTestData(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener, TestResult testResult) throws IOException,
			InterruptedException {
		TapResult tapResult = null;
		TapBuildAction buildAction = null;

		final TapRemoteCallable remoteCallable = new TapRemoteCallable(
				testResults, listener);
		final List<TestSetMap> testSets = build.getWorkspace().act(
				remoteCallable);

		if (remoteCallable.hasParserErrors()) {
			build.setResult(Result.UNSTABLE);
		}

		if (remoteCallable.hasFailedTests()
				&& this.getFailedTestsMarkBuildAsFailure()) {
			build.setResult(Result.FAILURE);
		}

		tapResult = new TapResult(build, testSets);
		buildAction = new TapBuildAction(build, tapResult);

		return new TapData(buildAction);
	}

	/**
	 * @return
	 */
	private boolean getFailedTestsMarkBuildAsFailure() {
		if (this.failedTestsMarkBuildAsFailure == null) {
			return Boolean.FALSE;
		}
		return this.failedTestsMarkBuildAsFailure;
	}

	/**
	 * @return the testResults
	 */
	public String getTestResults() {
		return testResults;
	}

	public static class TapData extends TestResultAction.Data {

		private final TapBuildAction buildAction;

		/**
		 * @param buildAction
		 */
		public TapData(TapBuildAction buildAction) {
			super();
			this.buildAction = buildAction;
		}
		
		/**
		 * @return the buildAction
		 */
		public TapBuildAction getBuildAction() {
			return buildAction;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * hudson.tasks.junit.TestResultAction.Data#getTestAction(hudson.tasks
		 * .junit.TestObject)
		 */
		@Override
		public List<TapTestAction> getTestAction(TestObject testObject) {
			TapResult tapResult = buildAction.getResult();
			if(tapResult.getTotal() > 0) {
				return Collections.singletonList(new TapTestAction(tapResult));
			} else {
				return Collections.EMPTY_LIST;
			}
		}

	}

	@Extension
	public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
		/*
		 * (non-Javadoc)
		 * 
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return "Publish TAP test result report";
		}
	}

}
