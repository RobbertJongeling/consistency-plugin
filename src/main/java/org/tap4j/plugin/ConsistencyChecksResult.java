package org.tap4j.plugin;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.tap4j.plugin.TapProjectAction.Config;
import org.tap4j.plugin.TapProjectAction.ConsistencyRuleEntry;
import org.tap4j.plugin.TapProjectAction.TapProjectActionDescriptor;
import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.TestSetMap;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.scm.ChangeLogSet.Entry;
import hudson.util.FormApply;
import jenkins.model.Jenkins;

public class ConsistencyChecksResult implements ModelObject, Serializable, Describable<ConsistencyChecksResult> {
	
    private static final long serialVersionUID = 4343399327336076951L;

    private static final Logger LOGGER = Logger.getLogger(TapResult.class.getName());

	private String name;
	private XmlFile resultsFile;
	private Run owner;
	private Boolean showOnlyFailures;
	private List<CheckResult> checkResults;
	private Config config;

	public ConsistencyChecksResult(String name, XmlFile resultsFile, Run owner, List<CheckResult> checkResults) {
		this.name = name;
		this.owner = owner;
		this.checkResults = new LinkedList<CheckResult>();
		this.checkResults.addAll(checkResults);
		
		List<ConsistencyRuleEntry> allEntries = new LinkedList<ConsistencyRuleEntry>();
		for(CheckResult cr : checkResults) {
			allEntries.add(cr.getCRE());
		}
		this.config = new Config(allEntries);
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
		return "hello world";
	}

	public void setOwner(Run owner) {
		this.owner = owner;		
	}

	public void setShowOnlyFailures(Boolean showOnlyFailures) {
		// TODO Auto-generated method stub
		
	}

	public void tally() {
		// TODO Auto-generated method stub
		
	}

	public List<CheckResult> getCheckResults() {
		return checkResults;
	}

	public TreeMap<String, String> getParseErrorTestSets() {
		// TODO Auto-generated method stub
		return new TreeMap<String, String>();
	}

	public boolean hasParseErrors() {
		// TODO Auto-generated method stub
		return false;
	}

	public int getFailed() {
		// TODO Auto-generated method stub
		return 20;
	}

	public ConsistencyChecksResult copyWithExtraTestSets(List<CheckResult> testSets) {
		List<CheckResult> mergedTestSets = new ArrayList<CheckResult>(getCheckResults());
        mergedTestSets.addAll(testSets);

        return new ConsistencyChecksResult(this.getName(), this.getResultsFile(), this.getOwner(), mergedTestSets);
	}

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
	
	public XmlFile getResultsFile() {
		return resultsFile;
	}
	
	public Run getOwner() {
        return this.owner;
    }
	
	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}
	
	public HttpResponse doConfigSubmit(StaplerRequest req) throws ServletException, IOException {
		config = null; // otherwise bindJSON will never clear it once set
		req.bindJSON(this, req.getSubmittedForm());
		getResultsFile().write(this);
		return FormApply.success(".");
	}

    /*
     * (non-Javadoc)
     * 
     * @see hudson.model.ModelObject#getDisplayName()
     */
    public String getDisplayName() {
        return getName();
    }

	public int getPassed() {
		// TODO Auto-generated method stub
		return 60;
	}

	public int getSkipped() {
		// TODO Auto-generated method stub
		return 10;
	}

	public int getToDo() {
		// TODO Auto-generated method stub
		return 10;
	}

	public int getTotal() {
		// TODO Auto-generated method stub
		return 100;
	}

}
