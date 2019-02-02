package org.tap4j.plugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.TestSetMap;

import hudson.model.ModelObject;
import hudson.model.Run;

public class ConsistencyChecksResult implements ModelObject, Serializable {
	
    private static final long serialVersionUID = 4343399327336076951L;

    private static final Logger LOGGER = Logger.getLogger(TapResult.class.getName());

	private String name;
	private Run owner;
	private Boolean showOnlyFailures;
	private List<CheckResult> checkResults;

	public ConsistencyChecksResult(String name, Run owner, List<CheckResult> checkResults) {
		this.name = name;
		this.owner = owner;
		this.checkResults = new LinkedList<CheckResult>();
		this.checkResults.addAll(checkResults);
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

        return new ConsistencyChecksResult(this.getName(), this.getOwner(), mergedTestSets);
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
	
	public Run getOwner() {
        return this.owner;
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
