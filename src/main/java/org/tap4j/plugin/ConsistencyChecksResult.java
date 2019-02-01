package org.tap4j.plugin;

import java.util.List;
import java.util.TreeMap;

import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.TestSetMap;

import hudson.model.Run;

public class ConsistencyChecksResult {
	
	private Run owner;
	private Boolean showOnlyFailures;
	private List<CheckResult> checkResults;

	public ConsistencyChecksResult(String string, Run owner, List<CheckResult> checkResults) {
		// TODO Auto-generated constructor stub
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
		return null;
	}

	public boolean hasParseErrors() {
		// TODO Auto-generated method stub
		return false;
	}

	public int getFailed() {
		// TODO Auto-generated method stub
		return 20;
	}

	public ConsistencyChecksResult copyWithExtraTestSets(Object testSets) {
		// TODO Auto-generated method stub
		return null;
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
