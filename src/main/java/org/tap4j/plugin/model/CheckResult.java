package org.tap4j.plugin.model;

import org.tap4j.plugin.TapProjectAction.ConsistencyRuleEntry;
//
//public class CheckResult {
//	private ConsistencyRuleEntry cre;
//	private boolean pass;
//	private String comment;
//	
//	public CheckResult(ConsistencyRuleEntry cre, boolean pass, String comment) {
//		this.cre = cre;
//		this.pass = pass;
//		this.comment = comment;
//	}
//	
//	public ConsistencyRuleEntry getCRE() {
//		return cre;
//	}
//	
//	public boolean getPass() {
//		return pass;
//	}
//	
//	public String getComment() {
//		return comment;
//	}
//}

public enum CheckResult {
	FAIL, PASS, NYE;
}
