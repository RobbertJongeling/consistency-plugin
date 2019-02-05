package org.tap4j.plugin;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.tap4j.plugin.TapProjectAction.Config;
import org.tap4j.plugin.TapProjectAction.ConsistencyRuleEntry;
import org.tap4j.plugin.TapProjectAction.TapProjectActionDescriptor;
import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.plugin.TapProjectAction.Entry;

import hudson.Extension;
import hudson.FilePath;
import hudson.XmlFile;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.util.FormApply;
import jenkins.model.Jenkins;

public class ConsistencyChecksResult implements ModelObject, Serializable, Describable<ConsistencyChecksResult> {
	
    private static final long serialVersionUID = 4343399327336076951L;

    private static final Logger LOGGER = Logger.getLogger(ConsistencyChecksResult.class.getName());

	private String name;
	private String resultsFilePath;
	private Run<?, ?>  build;
	private Boolean showOnlyFailures;
	//private List<ConsistencyRuleEntry> checkResults;
	private Config config;
	
	private int nrPassed;
	private int nrFailed;
	private int nrSkipped;
	private int nrTodo;
	private int total;

	public ConsistencyChecksResult(String name, String resultsFilePath, Run<?, ?> build, Config config /*List<ConsistencyRuleEntry> checkResults*/) {
		this.name = name;
		this.resultsFilePath = resultsFilePath;
		this.build = build;
//		this.checkResults = new LinkedList<ConsistencyRuleEntry>();
//		this.checkResults.addAll(checkResults);
//		this.config = new Config(this.checkResults);
		this.config = config;
	}
	
	public static class ConsistencyChecksResultDescriptor extends Descriptor<ConsistencyChecksResult> {
	}

	@Extension
	public static class DescriptorImpl extends ConsistencyChecksResultDescriptor {
	}

	@Override
	public Descriptor<ConsistencyChecksResult> getDescriptor() {
		return Jenkins.getInstance().getDescriptor(getClass());
	}
	
	public String getConsistencyChecks() {
		//TODO? 
		return "hello world";
	}

	public void setOwner(Run<?, ?> build) {
		this.build = build;		
	}

	public void setShowOnlyFailures(Boolean showOnlyFailures) {
		// TODO Auto-generated method stub
		
	}

//	public List<ConsistencyRuleEntry> getCheckResults() {
//		return checkResults;
//	}

	public TreeMap<String, String> getParseErrorTestSets() {
		// TODO Auto-generated method stub
		return new TreeMap<String, String>();
	}

	public boolean hasParseErrors() {
		// TODO Auto-generated method stub
		return false;
	}

//	public ConsistencyChecksResult copyWithExtraTestSets(List<Entry> testSets) {
////		List<ConsistencyRuleEntry> mergedTestSets = new ArrayList<ConsistencyRuleEntry>(getCheckResults());
//		Config newConfig = config;
//		List<Entry> mergedTestSets = new ArrayList<Entry>(config.getEntries());		
//        mergedTestSets.addAll(testSets);
//        newConfig.setterForEntries(mergedTestSets);
//
//        return new ConsistencyChecksResult(this.getName(), this.getResultsFilePath(), this.getOwner(), newConfig);
//	}

	private Object getIncludeCommentDiagnostics() {
		// TODO Auto-generated method stub
		return null;
	}

	private Object getValidateNumberOfTests() {
		// TODO Auto-generated method stub
		return null;
	}

	private Object getTodoIsFailure() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
        return name;
    }
	
	public void setName(String name) {
		this.name = name;
	}
	
	public XmlFile getResultsFile() {
//		return resultsFile;
		return new XmlFile(new File(this.getResultsFilePath()));
	}
	
	public String getResultsFilePath() {
		if(resultsFilePath == null || resultsFilePath.equals("")) {
			//TODO?
			LOGGER.log(Level.SEVERE, "resultsFilePath is null or empty String, can not save ");
			resultsFilePath = "";
		}
		
		return resultsFilePath;
	}
	
	public Run<?, ?> getOwner() {
        return this.build;
    }
	
	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}
	
	public void setResultsFilePath(String rfp) {
		this.resultsFilePath = rfp;
	}
	
	public void setResultsFilePath(FilePath rfp) {
		this.resultsFilePath = rfp.getRemote();
	}
	
	public HttpResponse doConfigSubmit(StaplerRequest req) throws ServletException, IOException {
//		Config temp = config; //since config needs to be null for submitting, but if we leave it null then after save everything is gone.
		config = null; // otherwise bindJSON will never clear it once set
		req.bindJSON(this, req.getSubmittedForm());
		getResultsFile().write(this);
//		config = temp;
		return FormApply.success(".");
	}

	public boolean saveConfig(FilePath dest) {
		boolean success;
		XmlFile destFile = new XmlFile(new File(dest.getRemote()));
		try {
			destFile.write(this);
			success = true;
		} catch (IOException e) {
			success = false;
		}
		return success;
	}
//	
//	XmlFile ccFile = new XmlFile(new File(results.getRemote()));
//	
//	// This reads the XML file into the checksResult object.
//	if(ccFile.exists()) {
//		try {
//			checksResult = (ConsistencyChecksResult) ccFile.unmarshal(checksResult);
//		} catch (Exception ex) {// could also be TapProjectAction.
//			TapProjectAction tpa = null;
//			tpa = (TapProjectAction) ccFile.unmarshal(tpa);
//			checksResult = new ConsistencyChecksResult("Consistency Checks Results", results.getRemote(), build, tpa.getConfig());
//		}
//	}
	
    /*
     * (non-Javadoc)
     * 
     * @see hudson.model.ModelObject#getDisplayName()
     */
    public String getDisplayName() {
        return getName();
    }

    public void tally() {
    	int passes = 0;
    	int fails = 0;
    	int skips = 0;
    	int todos = 0;
		for(Entry e : config.getEntries()) {
			if(e instanceof ConsistencyRuleEntry) {
				ConsistencyRuleEntry cre = (ConsistencyRuleEntry)e;
				switch(cre.getResult()) {
				case PASS:
					passes++;
					break;
				case FAIL:
					fails++;
					break;
				case SKIP:
					skips++;
					break;
				case MUTE:
					//on purpose. This is used in summaries, so there essentially skip==mute.
					skips++; 
					break;
				case NYE:
					todos++;
					break;
				}
			}
		}
		this.nrPassed = passes;
		this.nrFailed = fails;
		this.nrSkipped = skips;
		this.nrTodo = todos;
		this.total = this.nrPassed + this.nrFailed + this.nrSkipped + this.nrTodo;
    }
    
	public int getPassed() {
		return this.nrPassed;
	}
	
	public int getFailed() {
		return this.nrFailed;
	}

	public int getSkipped() {
		return this.nrSkipped;
	}

	public int getToDo() {
		return this.nrTodo;
	}

	public int getTotal() {
		return this.total;
	}

}
