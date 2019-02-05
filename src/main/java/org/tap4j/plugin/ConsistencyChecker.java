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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang.BooleanUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tap4j.model.Plan;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.TapProjectAction.Config;
import org.tap4j.plugin.TapProjectAction.ConsistencyRuleEntry;
import org.tap4j.plugin.TapProjectAction.Entry;
import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.plugin.util.Constants;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.XmlFile;
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
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

/**
 *
 *
 */
public class ConsistencyChecker extends Recorder implements MatrixAggregatable, SimpleBuildStep {

	private final Boolean failIfChecksFail;
	private final Boolean showOnlyFailures;
	private String testResults;
	
	private static final String consistencyFileName = "consistencyChecks.xml";

	@DataBoundConstructor
	public ConsistencyChecker(Boolean failIfChecksFail, Boolean showOnlyFailures) {
		this.failIfChecksFail = BooleanUtils.toBooleanDefaultIfNull(failIfChecksFail, false);
		this.showOnlyFailures = BooleanUtils.toBooleanDefaultIfNull(showOnlyFailures, false);
	}

	public Object readResolve() {
		final Boolean _failIfChecksFail = BooleanUtils.toBooleanDefaultIfNull(this.getFailIfChecksFail(), false);
		final Boolean _showOnlyFailures = BooleanUtils.toBooleanDefaultIfNull(this.getShowOnlyFailures(), false);

		return new ConsistencyChecker(_failIfChecksFail, _showOnlyFailures);
	}

	public Boolean getFailIfChecksFail() {
		return this.failIfChecksFail;
	}

	public Boolean getShowOnlyFailures() {
		return this.showOnlyFailures;
	}

	/**
	 * @return the testResults
	 */
	public String getTestResults() {
		return testResults;
	}

	/**
	 * Gets the directory where the plug-in saves its TAP streams before processing
	 * them and displaying in the UI.
	 * <p>
	 * Adapted from JUnit Attachments Plug-in.
	 *
	 * @param build Jenkins build
	 * @return virtual directory (FilePath)
	 */
	public static FilePath getReportsDirectory(Run build) {
		return new FilePath(new File(build.getRootDir().getAbsolutePath())).child(Constants.TAP_DIR_NAME);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see hudson.tasks.BuildStepCompatibilityLayer#getProjectAction(hudson.model
	 * .AbstractProject)
	 */
	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		return new TapProjectAction(project);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild ,
	 * hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
			@Nonnull TaskListener listener) throws InterruptedException, IOException {

		performImpl(run, workspace, listener);
	}

	/**
	 * This is the method that is executed in the post-build action "Run Consistency
	 * Checks"
	 * 
	 * @param build
	 * @param workspace
	 * @param listener
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private boolean performImpl(Run<?, ?> build, FilePath workspace, TaskListener listener)
			throws IOException, InterruptedException {
		final PrintStream logger = listener.getLogger();
		if (isPerformChecker(build)) {
			logger.println("Consistency Checking: START");
			
			//Stub:
			logger.println("Placeholder, for now we only copy the config");
			
			//copying the config from last build (or default place) to new build
			//oldPath is previous build, or, if not exists, the default
			FilePath oldPath = null;
			
			if(build.getPreviousBuild() != null) {
				File file = new File(build.getPreviousBuild().getRootDir().getAbsolutePath() + "/" + consistencyFileName);
				if(file.exists()) {
					oldPath = new FilePath(file);
				} 
			} else {
				logger.println("previous build is null, so copying the config file in default location");
				logger.println("checking in: ");
				oldPath = new FilePath(new File(build.getParent().getRootDir(), ("/" + consistencyFileName)));
			}
			
			File newFile = new File(build.getRootDir().getAbsolutePath() + "/" + consistencyFileName);
			FilePath newPath = new FilePath(newFile);
			
			//Stub: copy the config from old to new. 
			logger.println("old and new paths are: " + oldPath.toString() + " to " + newPath.toString());
//			oldPath.copyTo(newPath);
			
			ConsistencyChecksResult ccr = loadResults(oldPath, build, logger);
			ConsistencyChecksRunner crr = new ConsistencyChecksRunner(ccr, logger);
			crr.runChecks();
			crr.saveResults(newPath);
			
			
			logger.println("Consistency Checking: DONE");
			logger.println("Displaying Consistency Checking Results: START");
			
//			logger.println("checking for results in workspace: " + workspace.toString());
//			logger.println("checking for results in " + build.getRootDir().getAbsolutePath() + "/" + consistencyFileName);
			FilePath results = new FilePath(new File(build.getRootDir().getAbsolutePath() + "/" + consistencyFileName));
			logger.println("results path is: " + results.toString());

//			boolean filesSaved = saveReports(workspace, ConsistencyChecker.getReportsDirectory(build), results, logger);
//			if (!filesSaved) {
//				logger.println("Failed to save Consistency Check reports");
//				return Boolean.TRUE;
//			}

			ConsistencyChecksResult checksResult = null;
			try {
				logger.println("loading results");
				checksResult = loadResults(results, build, logger);
				logger.println("loaded results");
				checksResult.setShowOnlyFailures(this.getShowOnlyFailures());
				checksResult.tally();
			} catch (Throwable t) {
				/*
				 * don't fail build if TAP parser barfs. only print out the exception to
				 * console.
				 */
				t.printStackTrace(logger);
			}

//            TapTestResultAction trAction = build.getAction(TapTestResultAction.class);
//			boolean appending = false;
//    
//            if (trAction == null) {
//                appending = false;
//                logger.println("no taptestresultaction yet, appending false");
//                trAction = new TapTestResultAction(build, checksResult);
//            } else {
//                appending = true;
//                logger.println("taptestresultaction existing, appending true");
//                trAction.mergeResult(checksResult);
//            }
    
//            if (!appending) {
//                build.addAction(trAction);
//            }

			if (checksResult.getConfig().getEntries().size() > 0 || checksResult.getParseErrorTestSets().size() > 0) {
				// create an individual report for all of the results and add it to
				// the build

				TapBuildAction action = build.getAction(TapBuildAction.class);
				if (action == null) {
					logger.println("adding tapbuildaction");//TODO this seems to add the consistency check results and the now empty line to the menu on the left
					action = new TapBuildAction(build, checksResult);
					build.addAction(action);
				} else {
					logger.println("merging result NYI!");
//					appending = true;
//					action.mergeResult(checksResult); //TODO
				}

				if (checksResult.hasParseErrors()) {
					listener.getLogger().println("TAP parse errors found in the build. Marking build as UNSTABLE");
					build.setResult(Result.UNSTABLE);
				}

				if (checksResult.getFailed() > 0) {
					if (this.getFailIfChecksFail()) {
						listener.getLogger().println(
								"There are failed test cases and the job is configured to mark the build as failure. Marking build as FAILURE");
						build.setResult(Result.FAILURE);
					} else {
						listener.getLogger().println("There are failed test cases. Marking build as UNSTABLE");
						build.setResult(Result.UNSTABLE);
					}
				}

//				if (appending) {
					build.save();
//				}

			} else {
				logger.println("Found matching files but did not find any TAP results.");
				return Boolean.TRUE;
			}

			logger.println("Consistency Checking: FINISH");
		} else {
			logger.println("Build result is not better or equal unstable. Skipping TAP publisher.");
		}
		return Boolean.TRUE;		
	}

	/**
	 * Return {@code true} if the build is ongoing, if the user did not ask to fail
	 * when failed, or otherwise if the build result is not better or equal to
	 * unstable.
	 * 
	 * @param build Run
	 * @return whether to perform the publisher or not, based on user provided
	 *         configuration
	 */
	private boolean isPerformChecker(Run<?, ?> build) {
		Result result = build.getResult();
		// may be null if build is ongoing
		if (result == null) {
			return true;
		}

		return result.isBetterOrEqualTo(Result.UNSTABLE);
	}

	/**
	 * Iterates through the list of test sets and validates its plans and test
	 * results.
	 *
	 * @param testSets
	 * @return <true> if there are any test case that doesn't follow the plan
	 */
	private boolean validateNumberOfTests(List<TestSetMap> testSets) {
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

	/**
	 * 
	 * @param antPattern
	 * @param owner
	 * @param logger
	 * @return
	 */
	private ConsistencyChecksResult loadResults(FilePath results, Run owner, PrintStream logger) {
		ConsistencyChecksResult ccr;
		try {
			final ConsistencyChecksParser parser = new ConsistencyChecksParser();

			logger.println("parsing results from hardcoded location");
			
			final ConsistencyChecksResult result = parser.parse(results, owner, logger);
			result.setOwner(owner);
			return result;
		} catch (Exception e) {
			e.printStackTrace(logger);

			//TODO fix this, for now null since exception scenario anyway.
			ccr = new ConsistencyChecksResult("", null, owner, null);
			ccr.setOwner(owner);
			return ccr;
		}
	}

//    /**
//     * @param owner
//     * @param logger
//     * @return
//     */
//    private TapResult loadResults(String antPattern, Run owner, PrintStream logger) {
//        final FilePath tapDir = ConsistencyChecker.getReportsDirectory(owner);
//        FilePath[] results;
//        TapResult tr;
//        try {
//            results = tapDir.list(antPattern);
//            final TapParser parser = new TapParser(false, true, false, false, false, false, false, false, false, logger);
//
//            final TapResult result = parser.parse(results, owner);
//            result.setOwner(owner);
//            return result;
//        } catch (Exception e) {
//            e.printStackTrace(logger);
//
//            tr = new TapResult("", owner, Collections.<TestSetMap>emptyList(), false, false, false);
//            tr.setOwner(owner);
//            return tr;
//        }
//    }

	/**
	 * @param workspace
	 * @param tapDir
	 * @param reports
	 * @param logger
	 * @return
	 */
	private boolean saveReports(FilePath workspace, FilePath tapDir, FilePath report, PrintStream logger) {
		logger.println("Saving check config...");
		try {
			tapDir.mkdirs();
//			for (FilePath report : reports) {
				// FilePath dst = tapDir.child(report.getName());
				FilePath dst = getDistDir(workspace, tapDir, report);
				report.copyTo(dst);
//			}
		} catch (Exception e) {
			e.printStackTrace(logger);
			return false;
		}
		return true;
	}

	/**
	 * Used to maintain the directory structure when persisting to the tap-reports
	 * dir.
	 *
	 * @param workspace Jenkins WS
	 * @param tapDir    tap reports dir
	 * @param orig      original directory
	 * @return persisted directory virtual structure
	 */
	private FilePath getDistDir(FilePath workspace, FilePath tapDir, FilePath orig) {
		if (orig == null)
			return null;
		StringBuilder difference = new StringBuilder();
		FilePath parent = orig.getParent();
		do {
			if (parent.equals(workspace))
				break;
			difference.insert(0, parent.getName() + File.separatorChar);
		} while ((parent = parent.getParent()) != null);
		difference.append(orig.getName());
		return tapDir.child(difference.toString());
	}

	/**
	 * Checks that there are new report files.
	 *
	 * @param build
	 * @param reports
	 * @param logger
	 * @return
	 */
	private FilePath[] checkReports(Run build, FilePath[] reports, PrintStream logger) {
		List<FilePath> filePathList = new ArrayList<FilePath>(reports.length);

		for (FilePath report : reports) {
			/*
			 * Check that the file was created as part of this build and is not something
			 * left over from before.
			 *
			 * Checks that the last modified time of file is greater than the start time of
			 * the build
			 */
			try {
				/*
				 * dividing by 1000 and comparing because we want to compare secs and not
				 * milliseconds
				 */
				if (build.getTimestamp().getTimeInMillis() / 1000 <= report.lastModified() / 1000) {
					filePathList.add(report);
				} else {
					logger.println(
							report.getName() + " was last modified before " + "this build started. Ignoring it.");
				}
			} catch (IOException e) {
				// just log the exception
				e.printStackTrace(logger);
			} catch (InterruptedException e) {
				// just log the exception
				e.printStackTrace(logger);
			}
		}
		return filePathList.toArray(new FilePath[] {});
	}

	/**
	 * @param workspace
	 * @param testResults
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private FilePath[] locateReports(FilePath workspace, String testResults) throws IOException, InterruptedException {
		return workspace.list(testResults);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see hudson.tasks.BuildStep#getRequiredMonitorService()
	 */
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	// matrix jobs and test result aggregation support

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * hudson.matrix.MatrixAggregatable#createAggregator(hudson.matrix.MatrixBuild,
	 * hudson.Launcher, hudson.model.BuildListener)
	 */
	public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		return new TestResultAggregator(build, launcher, listener);
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public DescriptorImpl() {
			super(ConsistencyChecker.class);
			load();
		}

		@Override
		public String getDisplayName() {
			return "Run Consistency Checks";
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
