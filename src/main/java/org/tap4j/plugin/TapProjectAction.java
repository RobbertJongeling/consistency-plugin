/*
 * The MIT License
 *
 * Copyright (c) 2011 Bruno P. Kinoshita
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

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.XmlFile;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.util.FormApply;
import hudson.util.ListBoxModel;
import hudson.util.RunList;
import hudson.util.XStream2;
import jenkins.model.Jenkins;

import org.jfree.chart.JFreeChart;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.ModelElement;
import org.tap4j.plugin.util.GraphHelper;
import org.tap4j.plugin.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

/**
 * A TAP Project action, with a graph and a list of builds.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapProjectAction implements Action, Describable<TapProjectAction> { // extends AbstractTapProjectAction {

	public final Job<?, ?> job;

	protected class Result {
		public int numPassed;
		public int numFailed;
		public int numSkipped;
		public int numToDo;

		public Result() {
			numPassed = 0;
			numFailed = 0;
			numSkipped = 0;
			numToDo = 0;
		}

		public void add(Result r) {
			numPassed += r.numPassed;
			numFailed += r.numFailed;
			numSkipped += r.numSkipped;
			numToDo += r.numToDo;
		}
	}

	/**
	 * Used to figure out if we need to regenerate the graphs or not. Only used in
	 * newGraphNotNeeded() method. Key is the request URI and value is the number of
	 * builds for the project.
	 */
	private transient Map<String, Integer> requestMap = new HashMap<String, Integer>();

	public TapProjectAction(Job<?, ?> job) {
		this.job = job;
	}

	protected Class<TapBuildAction> getBuildActionClass() {
		return TapBuildAction.class;
	}

	public TapBuildAction getLastBuildAction() {
		TapBuildAction action = null;
		final Run<?, ?> lastBuild = this.getLastBuildWithTap();

		if (lastBuild != null) {
			action = lastBuild.getAction(TapBuildAction.class);
		}

		return action;
	}

	/**
	 * @return
	 */
	private Run<?, ?> getLastBuildWithTap() {
		Run<?, ?> lastBuild = this.job.getLastBuild();
		while (lastBuild != null && lastBuild.getAction(TapBuildAction.class) == null) {
			lastBuild = lastBuild.getPreviousBuild();
		}
		return lastBuild;
	}

	/**
	 * This is run when the page is loaded, it redirects to nodata(.jelly) in case
	 * there is no data yet
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public void doIndex(final StaplerRequest request, final StaplerResponse response) throws IOException {
		Run<?, ?> lastBuild = this.getLastBuildWithTap();
		if (lastBuild == null) {
			response.sendRedirect2("nodata");
		} else {
			int buildNumber = lastBuild.getNumber();
			response.sendRedirect2(String.format("../%d/%s", buildNumber, TapBuildAction.URL_NAME));
		}
	}

	/**
	 * Generates the graph that shows test pass/fail ratio.
	 *
	 * @param req Stapler request
	 * @param rsp Stapler response
	 * @throws IOException if it fails to create the graph image and serve it
	 */
	public void doGraph(final StaplerRequest req, StaplerResponse rsp) throws IOException {
		if (newGraphNotNeeded(req, rsp)) {
			return;
		}

		final DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dataSetBuilder = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

		populateDataSetBuilder(dataSetBuilder);
		new hudson.util.Graph(-1, getGraphWidth(), getGraphHeight()) {
			protected JFreeChart createGraph() {
				return GraphHelper.createChart(req, dataSetBuilder.build());
			}
		}.doPng(req, rsp);
	}

	public void doGraphMap(final StaplerRequest req, StaplerResponse rsp) throws IOException {
		if (newGraphNotNeeded(req, rsp)) {
			return;
		}

		final DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dataSetBuilder = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

		// TODO: optimize by using cache
		populateDataSetBuilder(dataSetBuilder);
		new hudson.util.Graph(-1, getGraphWidth(), getGraphHeight()) {
			protected JFreeChart createGraph() {
				return GraphHelper.createChart(req, dataSetBuilder.build());
			}
		}.doMap(req, rsp);
	}

	/**
	 * Returns <code>true</code> if there is a graph to plot.
	 * 
	 * @return value for property 'graphAvailable'
	 */
	public boolean isGraphActive() {
		Run<?, ?> build = this.job.getLastBuild();
		// in order to have a graph, we must have at least two points.
		int numPoints = 0;
		while (numPoints < 2) {
			if (build == null) {
				return false;
			}
			if (this.job instanceof MatrixProject) {
				MatrixProject mp = (MatrixProject) this.job;

				for (Job j : mp.getAllJobs()) {
					if (j != mp) { // getAllJobs includes the parent job too, so skip that
						Run<?, ?> sub = j.getBuild(build.getId());
						if (sub != null) {
							// Not all builds are on all sub-projects
							if (sub.getAction(getBuildActionClass()) != null) {
								// data for at least 1 sub-job on this build
								numPoints++;
								break; // go look at the next build now
							}
						}
					}
				}
			} else {
				if (build.getAction(getBuildActionClass()) != null) {
					numPoints++;
				}
			}
			build = build.getPreviousBuild();
		}
		return true;
	}

	/**
	 * If number of builds hasn't changed and if checkIfModified() returns true, no
	 * need to regenerate the graph. Browser should reuse it's cached image
	 * 
	 * @param req Stapler request
	 * @param rsp Stapler response
	 * @return true, if new image does NOT need to be generated, false otherwise
	 */
	private boolean newGraphNotNeeded(final StaplerRequest req, StaplerResponse rsp) {
		Calendar t = this.job.getLastCompletedBuild().getTimestamp();
		Integer prevNumBuilds = requestMap.get(req.getRequestURI());
		int numBuilds = 0;
		RunList<?> builds = this.job.getBuilds();
		Iterator<?> it = builds.iterator();
		while (it.hasNext()) {
			it.next();
			numBuilds += 1;
		}

		prevNumBuilds = prevNumBuilds == null ? 0 : prevNumBuilds;
		if (prevNumBuilds != numBuilds) {
			requestMap.put(req.getRequestURI(), numBuilds);
		}

		if (requestMap.keySet().size() > 10) {
			// keep map size in check
			requestMap.clear();
		}

		if (prevNumBuilds == numBuilds && req.checkIfModified(t, rsp)) {
			/*
			 * checkIfModified() is after '&&' because we want it evaluated only if number
			 * of builds is different
			 */
			return true;
		}

		return false;
	}

	protected void populateDataSetBuilder(DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dataset) {

		Job<?, ?> p = this.job;

		for (Run<?, ?> build = this.job.getLastBuild(); build != null; build = build.getPreviousBuild()) {

			/*
			 * The build has most likely failed before any TAP data was recorded.
			 *
			 * If we don't exclude such builds, we'd have to account for that in
			 * GraphHelper. Besides that, it's not consistent with JUnit graph behaviour
			 * where builds without test results are not included in graph.
			 */
			if (build.getAction(TapBuildAction.class) == null) {
				continue;
			}

			ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel((Run) build);

			Result r = new Result();

			if (p instanceof MatrixProject) {
				MatrixProject mp = (MatrixProject) p;

				for (Job j : mp.getAllJobs()) {
					if (j != mp) { // getAllJobs includes the parent job too, so skip that
						Run<?, ?> sub = j.getBuild(build.getId());
						if (sub != null) {
							// Not all builds are on all sub-projects
							r.add(summarizeBuild(sub));
						}
					}
				}
			} else {
				r = summarizeBuild(build);
			}

			dataset.add(r.numPassed, "Passed", label);
			dataset.add(r.numFailed, "Failed", label);
			dataset.add(r.numSkipped, "Skipped", label);
			dataset.add(r.numToDo, "ToDo", label);
		}
	}

	protected Result summarizeBuild(Run<?, ?> b) {
		Result r = new Result();

		TapBuildAction action = b.getAction(getBuildActionClass());
		if (action != null) {
			ConsistencyChecksResult report = action.getResult();
			report.tally();

			r.numPassed = report.getPassed();
			r.numFailed = report.getFailed();
			r.numSkipped = report.getSkipped();
			r.numToDo = report.getToDo();
		}

		return r;
	}

	/**
	 * Getter for property 'graphWidth'.
	 * 
	 * @return Value for property 'graphWidth'.
	 */
	public int getGraphWidth() {
		return 500;
	}

	/**
	 * Getter for property 'graphHeight'.
	 * 
	 * @return Value for property 'graphHeight'.
	 */
	public int getGraphHeight() {
		return 200;
	}

	// My hacking from here

	public static final String URL_NAME = "consistencyChecks";
	public static final String ICON_NAME = "/plugin/consistency/icons/tap-24.png";

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.Action#getDisplayName()
	 */
	public String getDisplayName() {
		return "Consistency Checks";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.Action#getIconFileName()
	 */
	public String getIconFileName() {
		return ICON_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.Action#getUrlName()
	 */
	public String getUrlName() {
		return URL_NAME;
	}

	public String getSearchUrl() {
		return URL_NAME;
	}

	public static class TapProjectActionDescriptor extends Descriptor<TapProjectAction> {
	}

	@Extension
	public static class DescriptorImpl extends TapProjectActionDescriptor {
	}

	@Override
	public Descriptor<TapProjectAction> getDescriptor() {
		return Jenkins.getInstance().getDescriptor(getClass());
	}

//	public TapProjectAction() throws IOException {
//		this.job = null;
//		XmlFile xml = getConfigFile();
//		if (xml.exists()) {
//			xml.unmarshal(this);
//		}
//	}

	public String getDescription() {
		return "Show a heterogeneous list of subitems with different data bindings for radio buttons and checkboxes";
	}

	public XmlFile getConfigFile() {
		return new XmlFile(new File(job.getRootDir(), "/consistencyChecks.xml"));
	}

	public List<String> getConsistencyChecks() {
		List<String> toReturn = new LinkedList<String>();
		if (config != null && config.entries != null) {
			for (Entry e : config.entries) {
				if (e instanceof ConsistencyRuleEntry) {
					ConsistencyRuleEntry cre = ((ConsistencyRuleEntry) e);
					toReturn.add(cre.toString());
				}
			}
		} else {
			XmlFile configFile = getConfigFile();
			if(configFile.exists()) {
				TapProjectAction tpa = null;
				try {
					tpa = (TapProjectAction) configFile.unmarshal(tpa);
					config = tpa.getConfig();
					//TODO fix this uglyness
					return getConsistencyChecks();
				} catch (IOException e) {
					toReturn.add("config is not a valid file");
				}
			} else {
				toReturn.add("config is null");
			}
		}
		return toReturn;
	}

	private Config config;

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public HttpResponse doConfigSubmit(StaplerRequest req) throws ServletException, IOException {
		config = null; // otherwise bindJSON will never clear it once set
		req.bindJSON(this, req.getSubmittedForm());
		getConfigFile().write(this);
		return FormApply.success(".");
	}

	public static final class Config extends AbstractDescribableImpl<Config> {

		private final List<Entry> entries;

		@DataBoundConstructor
		public Config(List<Entry> entries) {
			this.entries = entries != null ? new ArrayList<Entry>(entries) : Collections.<Entry>emptyList();
		}

		public List<Entry> getEntries() {
			return Collections.unmodifiableList(entries);
		}

		@Extension
		public static class DescriptorImpl extends Descriptor<Config> {
			@Override
			public String getDisplayName() {
				return "";
			}
		}
	}

	 public static abstract class Entry extends AbstractDescribableImpl<Entry> {}

	/*public static abstract class Entry implements ExtensionPoint, Describable<Entry> {
		protected int id;

		protected Entry(int id) {
			this.id = id;
		}

		public Descriptor<Entry> getDescriptor() {
			return Jenkins.getInstance().getDescriptor(getClass());
		}
	}*/

	public static class EntryDescriptor extends Descriptor<Entry> {
		public EntryDescriptor(Class<? extends Entry> clazz) {
			super(clazz);
		}

		public String getDisplayName() {
			return clazz.getSimpleName();
		}
	}

	public static final class ConsistencyRuleEntry extends Entry {

		private ModelElement a;
		private ModelElement b;
		private String strictness;
		private boolean mute;
		private boolean skip;
		private CheckResult result;
		private String resultText;
		
//		@DataBoundConstructor
		public ConsistencyRuleEntry(ModelElement a, ModelElement b, String strictness, boolean mute, boolean skip, CheckResult result, String resultText) {
			this.a = a;
			this.b = b;
			this.strictness = strictness;
			this.mute = mute;
			this.skip = skip;
			this.result = result;
			this.resultText = resultText;
		}
		
		@DataBoundConstructor
		public ConsistencyRuleEntry(String fileA, String fqnA, String fileB, String fqnB, String strictness, boolean mute, boolean skip, String result, String resultText) {
			this.a = new ModelElement(fileA, fqnA);
			this.b = new ModelElement(fileB, fqnB);
			this.strictness = strictness;
			this.mute = mute;
			this.skip = skip;
			
			switch(result) {
			case "PASS":
				this.result = CheckResult.PASS;
				break;
			case "FAIL":
				this.result = CheckResult.FAIL;
				break;
			case "NYE":
				this.result = CheckResult.NYE;
				break;
			case "SKIP":
				this.result = CheckResult.SKIP;
				break;
			case "MUTE":
				this.result = CheckResult.MUTE;
				break;
			default: 
				this.result = CheckResult.NYE;
				break;
			}

			this.resultText = resultText;
		}

		public ModelElement getA() {
			return a;
		}

		public ModelElement getB() {
			return b;
		}
		
		public String getFileA() {
			return a.getFile();
		}
		
		public String getFqnA() {
			return a.getFqn();
		}
		
		public String getFileB() {
			return b.getFile();
		}
		
		public String getFqnB() {
			return a.getFqn();
		}

		public String getStrictness() {
			return strictness;
		}

		public Boolean getMute() {
			return mute;
		}

		public boolean getSkip() {
			return skip;
		}
		
		public void setResult(CheckResult newResult) {
			this.result = newResult;
		}
		
		public CheckResult getResult() {
			return result;
		}
		
		public void setResultText(String newResultText) {
			this.resultText = newResultText;
		}
		
		public String getResultText() {
			return resultText;
		}

		public boolean hasResult() {
			return result == CheckResult.PASS || result == CheckResult.FAIL;
		}
		
		@Extension
		public static final class DescriptorImpl extends Descriptor<Entry> {
			@Override
			public String getDisplayName() {
				return "Consistency Check";
			}

			public ListBoxModel doFillStrictnessItems() {
				return new ListBoxModel().add("strict").add("medium").add("loose");
			}
			
			//TODO technical debt. fix this uglyness of code duplication
			public ListBoxModel doFillFileAItems() {
				ListBoxModel toReturn = new ListBoxModel();
				List<String> allModelFiles = Util.getAllModelFiles();
				for(String s : allModelFiles) {
					toReturn.add(s);
				}
				return toReturn;
			}
			
			public ListBoxModel doFillFileBItems() {
				return doFillFileAItems();
			}
			
			public ListBoxModel doFillFqnAItems(@QueryParameter String fileA) {
				ListBoxModel toReturn = new ListBoxModel();
				
				for(String s : Arrays.asList("A", "B", "C")) {
					toReturn.add(fileA + ":" + s);
				}
				
				return toReturn;
			}
			
			public ListBoxModel doFillFqnBItems(@QueryParameter String fileB) {
				ListBoxModel toReturn = new ListBoxModel();
				
				for(String s : Arrays.asList("A", "B", "C")) {
					toReturn.add(fileB + ":" + s);
				}
				
				return toReturn;
			}
		}
		
		@Override
		public String toString() {
			return a.getFqn() + " from file " + a.getFile() + " " + strictness + " consistent with " + b.getFqn() + " from file " + b.getFile() + ". Check is "
			+ (mute ? "" : "not ") + "muted and " + (skip ? "" : "not ") + "skipped.";
		}
	}
}
