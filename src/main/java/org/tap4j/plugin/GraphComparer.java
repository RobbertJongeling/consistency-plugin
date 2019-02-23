package org.tap4j.plugin;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.CheckResultEnum;
import org.tap4j.plugin.model.CheckStrictness;
import org.tap4j.plugin.model.CheckType;
import org.tap4j.plugin.model.Node;

public class GraphComparer {
	
	public static CheckResult doCompare(PrintStream logger, Node a, Node b, CheckType ct, CheckStrictness cs) {
		if(a == null || b == null) {
			return new CheckResult(CheckResultEnum.NYE, "One of the FQNs could not be resolved, transformation resulted in a null tree");
		}
		switch (ct) {
		case EQUIVALENCE:
			return doCompareEquivalence(logger, a, b, cs);
		case REFINEMENT:
			return doCompareRefinement(logger, a, b, cs);
		default:
			return new CheckResult(CheckResultEnum.NYE, "Not Yet Executed");
		}
	}

	private static CheckResult doCompareEquivalence(PrintStream logger, Node a, Node b, CheckStrictness cs) {
		switch (cs) {
		case STRICT:
			return GraphComparer.doCompareStrictEquivalence(logger, a, b);
		case LOOSE:
			return GraphComparer.doCompareLooseEquivalence(logger, a, b);
		default:
			return new CheckResult(CheckResultEnum.NYE, "Not Yet Executed");
		}
	}

	private static CheckResult doCompareRefinement(PrintStream logger, Node a, Node b, CheckStrictness cs) {
		switch (cs) {
		case STRICT:
			return GraphComparer.doCompareStrictRefinement(logger, a, b);
		case LOOSE:
			return GraphComparer.doCompareLooseRefinement(logger, a, b);
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
	private static CheckResult doCompareStrictEquivalence(PrintStream logger, Node a, Node b) {
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
					CheckResult recRes = doCompareStrictEquivalence(logger, a.children.get(i), b.children.get(i));
					if(recRes.getResult() != CheckResultEnum.PASS) {
						return recRes;
					}
				}
				return new CheckResult(CheckResultEnum.PASS, "Node: " + a.fqn + " is strictly equivalent to Node: " + b.fqn);
			}
		}		
	}	

	//the idea is that we transform the tree with root a into a list of numbers, 
	// each number consisting of a single digit for each level in the tree denoting the number of children at that level.
	// then, we compare both lists, if the are equal, the trees are loosely equivalent.
	private static CheckResult doCompareLooseEquivalence(PrintStream logger, Node a, Node b) {
		if(a.children.size() != b.children.size()) {
			return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.fqn + " and Node: " + b.fqn + " have unequal amount of children");			
		} else {
			List<String> nrChildrenTreeA = a.toNumberChildrenList(logger);
			List<String> nrChildrenTreeB = b.toNumberChildrenList(logger);
			
			logger.println("nr children tree A:");
			for(String s : nrChildrenTreeA) {
				logger.println(s);
			}
			logger.println("nr children tree B:");
			for(String s : nrChildrenTreeB) {
				logger.println(s);
			}
			
			if(!(nrChildrenTreeA.containsAll(nrChildrenTreeB) && nrChildrenTreeB.containsAll(nrChildrenTreeA))) {
				return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.fqn + " and Node: " + b.fqn + " are not loosely equivalent.");
			}
		}
		return new CheckResult(CheckResultEnum.PASS, "Node: " + a.fqn + " and Node: " + b.fqn + " are loosely equivalent.");
	}
	
	private static CheckResult doCompareStrictRefinement(PrintStream logger, Node a, Node b) {
		//TODO implement
		return new CheckResult(CheckResultEnum.PASS, "stub");
	}
	
	private static CheckResult doCompareLooseRefinement(PrintStream logger, Node a, Node b) {
		//TODO implement
		return new CheckResult(CheckResultEnum.PASS, "stub");
	}	
}
