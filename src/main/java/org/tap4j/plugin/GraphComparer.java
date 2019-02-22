package org.tap4j.plugin;

import java.util.Collections;

import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.CheckResultEnum;
import org.tap4j.plugin.model.CheckStrictness;
import org.tap4j.plugin.model.CheckType;
import org.tap4j.plugin.model.Node;

public class GraphComparer {
	
	public static CheckResult doCompare(Node a, Node b, CheckType ct, CheckStrictness cs) {
		switch (ct) {
		case EQUIVALENCE:
			return doCompareEquivalence(a, b, cs);
		case REFINEMENT:
			return doCompareRefinement(a, b, cs);
		default:
			return new CheckResult(CheckResultEnum.NYE, "Not Yet Executed");
		}
	}

	public static CheckResult doCompareEquivalence(Node a, Node b, CheckStrictness cs) {
		switch (cs) {
		case STRICT:
			return GraphComparer.doCompareStrictEquivalence(a, b);
		case LOOSE:
			return GraphComparer.doCompareLooseEquivalence(a, b);
		default:
			return new CheckResult(CheckResultEnum.NYE, "Not Yet Executed");
		}
	}

	public static CheckResult doCompareRefinement(Node a, Node b, CheckStrictness cs) {
		switch (cs) {
		case STRICT:
			return GraphComparer.doCompareStrictRefinement(a, b);
		case LOOSE:
			return GraphComparer.doCompareLooseRefinement(a, b);
		default:
			return new CheckResult(CheckResultEnum.NYE, "Not Yet Executed");
		}
	}
	
	public static CheckResult doCompareStrictEquivalence(Node a, Node b) {
		if(!a.name.equals(b.name)) {
			return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.toString() + " is unequal to Node: " + b.toString());
		} else {
			//compare the children, but we don't know the order, so sort first
			Collections.sort(a.children);
			Collections.sort(b.children);
			
			if(a.children.size() != b.children.size()) {
				return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.toString() + " and Node: " + b.toString() + " have unequal amount of children");
			} else {
				for(int i = 0; i<a.children.size(); i++) {
					CheckResult recRes = doCompareStrictEquivalence(a.children.get(i), b.children.get(i));
					if(recRes.getResult() != CheckResultEnum.PASS) {
						return recRes;
					}
				}
				return new CheckResult(CheckResultEnum.PASS, "Node: " + a.toString() + " is strictly equivalent to Node: " + b.toString());
			}
		}		
	}
	
	public static CheckResult doCompareLooseEquivalence(Node a, Node b) {
		//TODO implement
		return new CheckResult(CheckResultEnum.PASS, "stub");
	}
	
	public static CheckResult doCompareStrictRefinement(Node a, Node b) {
		//TODO implement
		return new CheckResult(CheckResultEnum.PASS, "stub");
	}
	
	public static CheckResult doCompareLooseRefinement(Node a, Node b) {
		//TODO implement
		return new CheckResult(CheckResultEnum.PASS, "stub");
	}	
}
