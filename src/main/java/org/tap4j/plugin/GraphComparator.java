package org.tap4j.plugin;

import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.CheckStrictness;
import org.tap4j.plugin.model.CheckType;
import org.tap4j.plugin.model.Node;

public class GraphComparator {
	private Node a;
	private Node b;
	private CheckResult result;
	private String resultText;
	
	public GraphComparator(Node a, Node b) {
		this.a = a;
		this.b = b;
		this.result = CheckResult.NYE;
		this.resultText = "NYE";
	}
		
	public void doCompare(CheckType ct, CheckStrictness cs) {
		switch(ct) {
		case EQUIVALENCE:
			doCompareEquivalence(cs);
			break;
		case REFINEMENT:
			doCompareRefinement(cs);
			break;
		}
		
		// stubbing
		this.result = CheckResult.PASS;
		this.resultText = "Stub: for now setting everything to pass, unless the test is skipped";
	}
	
	private void doCompareEquivalence(CheckStrictness cs) {
		switch(cs) {
		case STRICT:
			doCompareStrictEquivalence();
			break;
		case LOOSE:
			doCompareLooseEquivalence();
		}
	}
	
	private void doCompareRefinement(CheckStrictness cs) {
		switch(cs) {
		case STRICT:
			doCompareStrictRefinement();
			break;
		case LOOSE:
			doCompareLooseRefinement();
		}
	}
	
	private void doCompareStrictEquivalence() {
		//TODO implement
	}
	
	private void doCompareLooseEquivalence() {
		//TODO implement
	}
	
	private void doCompareStrictRefinement() {
		//TODO implement
	}
	
	private void doCompareLooseRefinement() {
		//TODO implement
	}	
	
	public CheckResult getResult() {
		return this.result;
	}
	
	public String getResultText() {
		return this.resultText;
	}
	
}
