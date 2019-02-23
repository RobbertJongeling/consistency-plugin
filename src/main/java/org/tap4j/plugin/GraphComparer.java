package org.tap4j.plugin;

import java.util.Collections;
import java.util.List;

import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.CheckResultEnum;
import org.tap4j.plugin.model.CheckStrictness;
import org.tap4j.plugin.model.CheckType;
import org.tap4j.plugin.model.Node;

public class GraphComparer {
	
	public static CheckResult doCompare(Node a, Node b, CheckType ct, CheckStrictness cs) {
		if(a == null || b == null) {
			return new CheckResult(CheckResultEnum.NYE, "One of the FQNs could not be resolved, transformation resulted in a null tree");
		}
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
	
	/**
	 * a and b are strictly equivalent iff their names are equal and they have equivalent children
	 * @param a
	 * @param b
	 * @return
	 */
	public static CheckResult doCompareStrictEquivalence(Node a, Node b) {
		if(!a.displayName.equals(b.displayName)) {
			return new CheckResult(CheckResultEnum.FAIL, a.fqn + "(" + a.displayName + ") is unequal to: " + b.fqn + "(" + b.displayName + ")");
		} else {
			//compare the children, but we don't know the order, so sort first
			Collections.sort(a.children);
			Collections.sort(b.children);
			
			if(a.children.size() != b.children.size()) {
				return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.fqn + " and Node: " + b.fqn + " have unequal amount of children");
			} else {
				for(int i = 0; i<a.children.size(); i++) {
					CheckResult recRes = doCompareStrictEquivalence(a.children.get(i), b.children.get(i));
					if(recRes.getResult() != CheckResultEnum.PASS) {
						return recRes;
					}
				}
				return new CheckResult(CheckResultEnum.PASS, "Node: " + a.fqn + " is strictly equivalent to Node: " + b.fqn);
			}
		}		
	}
	
	public static CheckResult doCompareLooseEquivalence(Node a, Node b) {
		if(a.children.size() != b.children.size()) {
			return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.fqn + " and Node: " + b.fqn + " have unequal amount of children");			
		} else {
			//the idea is that we transform the tree with root a into a list of numbers, 
			// each number consisting of a single digit for each level in the tree denoting the number of children at that level.
			// then, we compare both lists, if the are equal, the trees are loosely equivalent.
			List<String> nrChildrenTreeA = a.toNumberChildrenList();
			List<String> nrChildrenTreeB = b.toNumberChildrenList();
			
			if(!(nrChildrenTreeA.containsAll(nrChildrenTreeB) && nrChildrenTreeB.containsAll(nrChildrenTreeA))) {
				return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.fqn + " and Node: " + b.fqn + " are not loosely equivalent.");
			}
		}
		return new CheckResult(CheckResultEnum.PASS, "Node: " + a.fqn + " and Node: " + b.fqn + " are loosely equivalent.");
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
