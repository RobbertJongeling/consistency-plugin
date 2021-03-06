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
		case TYPE:
			return GraphComparer.doCompareTypeEquivalence(logger, a, b);
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
		case TYPE:
			return GraphComparer.doCompareTypeRefinement(logger, a, b);
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
			
			logger.println("Children tree A:");
			for(Node n : a.children) {
				logger.println(n.fqn);
			}
			logger.println("Children tree B:");
			for(Node n : b.children) {
				logger.println(n.fqn);
			}
			
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
	
	/**
	 * a and b are strictly equivalent iff their names are equal and they have equivalent children
	 * @param a
	 * @param b
	 * @return
	 */
	private static CheckResult doCompareTypeEquivalence(PrintStream logger, Node a, Node b) {
		if(!a.type.equals(b.type)) {
			return new CheckResult(CheckResultEnum.FAIL, a.fqn + "(" + a.type + ") has different type than: " + b.fqn + "(" + b.type + ")");
		} else {
			List<String> leafTypesA = a.toLeafTypeList();
			List<String> leafTypesB = b.toLeafTypeList();
			
			logger.println("leaf Types tree A:");
			for(String s : leafTypesA) {
				logger.println(s);
			}
			logger.println("leaf Types tree B:");
			for(String s : leafTypesB) {
				logger.println(s);
			}
			
			Collections.sort(leafTypesA);
			Collections.sort(leafTypesB);
			if(!(leafTypesA.equals(leafTypesB))) {
				return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.fqn + " is not type equivalent to Node: " + b.fqn + " see Console Output for more details");
			} else {
				return new CheckResult(CheckResultEnum.PASS, "Node: " + a.fqn + " is tye equivalent to Node: " + b.fqn);			
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
			List<String> nrChildrenTreeA = a.toNumberChildrenList();
			List<String> nrChildrenTreeB = b.toNumberChildrenList();
			
			logger.println("nr children tree A:");
			for(String s : nrChildrenTreeA) {
				logger.println(s);
			}
			logger.println("nr children tree B:");
			for(String s : nrChildrenTreeB) {
				logger.println(s);
			}
			
			Collections.sort(nrChildrenTreeA);
			Collections.sort(nrChildrenTreeB);
			if(!(nrChildrenTreeA.equals(nrChildrenTreeB))) {
				return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.fqn + " and Node: " + b.fqn + " are not loosely equivalent."  + " See Console Output for more details");
			}
		}
		return new CheckResult(CheckResultEnum.PASS, "Node: " + a.fqn + " and Node: " + b.fqn + " are loosely equivalent.");
	}
	
	// the idea here is to get the fqn of each leaf, then check if a contains all of b.
	// similar to the compare loose refinement, and less precise in error reporting than strict equivalence 
	// (which could have been done in the same way but didn't to enhance error reporting)
	private static CheckResult doCompareStrictRefinement(PrintStream logger, Node a, Node b) {
		if(!a.displayName.equals(b.displayName)) {
			return new CheckResult(CheckResultEnum.FAIL, a.fqn + "(" + a.displayName + ") is unequal to: " + b.fqn + "(" + b.displayName + ")");
		} else {
			List<String> leafFqnsA = a.toLeafFQNList();
			List<String> leafFqnsB = b.toLeafFQNList();
			
			logger.println("leaf FQNs tree A:");
			for(String s : leafFqnsA) {
				logger.println(s);
			}
			logger.println("leaf FQNs tree B:");
			for(String s : leafFqnsB) {
				logger.println(s);
			}
			
			if(!(leafFqnsA.containsAll(leafFqnsB))) {
				leafFqnsB.removeAll(leafFqnsA);
				return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.fqn + " does not strictly refine Node: " + b.fqn +". Missing: "+ leafFqnsB.toString());
			} else {
				return new CheckResult(CheckResultEnum.PASS, "Node: " + a.fqn + " strictly refines Node: " + b.fqn);			
			}
		}	
	}
	
	// the idea here is to get the fqn of each leaf, then check if a contains all of b.
	// similar to the compare loose refinement, and less precise in error reporting than strict equivalence 
	// (which could have been done in the same way but didn't to enhance error reporting)
	private static CheckResult doCompareTypeRefinement(PrintStream logger, Node a, Node b) {
		if(!a.type.equals(b.type)) {
			return new CheckResult(CheckResultEnum.FAIL, a.fqn + "(" + a.type + ") has different type than: " + b.fqn + "(" + b.type + ")");
		} else {
			List<String> leafTypesA = a.toLeafTypeList();
			List<String> leafTypesB = b.toLeafTypeList();
			
			logger.println("leaf Types tree A:");
			for(String s : leafTypesA) {
				logger.println(s);
			}
			logger.println("leaf Types tree B:");
			for(String s : leafTypesB) {
				logger.println(s);
			}
			
			//only contains is not enough, we should also count 
			if(!(leafTypesA.size() >= leafTypesB.size() && leafTypesA.containsAll(leafTypesB))) {
				return new CheckResult(CheckResultEnum.FAIL, "Node: " + a.fqn + " is not a type refinement of Node: " + b.fqn + " see Console Output for more details");
			} else {
				return new CheckResult(CheckResultEnum.PASS, "Node: " + a.fqn + " is a type refinement of Node: " + b.fqn);			
			}
		}
	}
	
	//if a refines b, then a should contain at least all children of b. 
	//in terms of these nrChildrentrees, it means that the numbers in a should be >= those in b
	private static CheckResult doCompareLooseRefinement(PrintStream logger, Node a, Node b) {
		//TODO implement, but maybe not worth doing at all, since it is so vague that it probably doesn't help anyone 
		return new CheckResult(CheckResultEnum.PASS, "Stub");			
	}
}
