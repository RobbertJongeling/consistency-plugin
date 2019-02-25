package org.tap4j.plugin.model;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

public class Node implements Comparable<Node> {

		public String type;
		public String fqn;
		public String displayName;
		public String optional;
		public List<Node> children;

		Node() {
		  this("default", "default", "default");
		}
		
		
		public Node(String type, String fqn, String displayName) {
			this(type, fqn, displayName, "");
		}
		
		public Node(String type, String fqn, String displayName, String optional) {
			this.type = fix(type);
			this.fqn = fix(fqn);
			this.displayName = fix(displayName);
			this.optional = fix(optional);
			this.children = new LinkedList<Node>();
		}
		
		public Node(String type, String fqn, String displayName, List<Node> children) {
			this(type, fqn, displayName);			
			this.children.addAll(children);
		}
		
		public void addChild(Node child) {
			children.add(child);
		}
		
		public void addChildren(List<Node> children) {
			this.children.addAll(children);
		}
		
		public void removeChild(Node child) {
			children.remove(child);
		}

		@Override
		public int compareTo(Node o) {
			return this.displayName.compareTo(o.displayName);
		}	
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof Node) {
				Node n = (Node) o;
				if(n.type.equals(this.type) && n.displayName.equals(this.displayName)) {
					//ignoring children for now, since the type+name should uniquely identify a node anyway
					return true; 
				}
			}
			return false;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Node: " + displayName + ", of type: " + type + " with optional parameter: " + optional + ".");
			for(Node c : this.children) {
				sb.append(c.toString() + "\r\n");
			}
			return sb.toString(); 
		}
		
		public String fix(String toFix) {
			return toFix.replace(" ", "").replace("\n", "").replace("\r", "");
		}
		
		public List<String> toListOfString() {
			List<String> toReturn = new LinkedList<>();
			
			toReturn.add(fqn);
			for(Node c : this.children) {
				toReturn.addAll(c.toListOfString());
			}
			
			return toReturn;
		}
		
		public String toGraphviz() {
			StringBuilder sb = new StringBuilder();
			sb.append("digraph G { ");
			sb.append(toSubGraphviz());				
			sb.append(" }");
			
			return sb.toString();
		}
		
		public String toSubGraphviz() {
			StringBuilder sb = new StringBuilder();
			
			for(Node c : this.children) {
				sb.append("\"" + this.displayName + "\" -> \"" + c.displayName + "\" \r\n");
				sb.append(c.toSubGraphviz());
			}
			
			return sb.toString();
		}
		
		public List<String> toNumberChildrenList() {
			List<String> toReturn = new LinkedList<>();
			
			int a = this.children.size();
			//recursively get the list of numbers for the children, then prepend to all strings the nr on this level
			for(Node c : this.children) {
				toReturn.addAll(c.toNumberChildrenList());				
			}
			//if no childdren, then we reached the bottom and we initialize the list with a 0
			if(toReturn.size() == 0) {
				toReturn.add("0");
			} else {
				//else: prepend the nr of children on this level
				List<String> tmp = new LinkedList<>();
				tmp.addAll(toReturn);
				toReturn.clear();
				for(String s : tmp) {
					toReturn.add(a + s); 
				}
			}
			
			return toReturn;
		}

		//not absolute fqns, but qns starting from this node
		public List<String> toLeafFQNList() {
			List<String> toReturn = new LinkedList<>();
			toReturn.add(displayName);
			
			for(Node c : children) {
				toReturn.addAll(c.toLeafFQNList(displayName));
			}
			
			return toReturn;
		}
		
		private List<String> toLeafFQNList(String prefix) {
			List<String> toReturn = new LinkedList<>();
			String qn = prefix + "/" + displayName;
			toReturn.add(qn);
			for(Node c : children) {
				toReturn.addAll(c.toLeafFQNList(qn));
			}
			
			return toReturn;
		}
		
		//not absolute paths, but types starting from this node
		public List<String> toLeafTypeList() {
			List<String> toReturn = new LinkedList<>();
			toReturn.add(type);
			
			for(Node c : children) {
				toReturn.addAll(c.toLeafTypeList(type));
			}
			
			return toReturn;
		}
		
		private List<String> toLeafTypeList(String prefix) {
			List<String> toReturn = new LinkedList<>();
			String newPrefix = prefix + "/" + type;
			toReturn.add(newPrefix);
			for(Node c : children) {
				toReturn.addAll(c.toLeafTypeList(newPrefix));
			}
			
			return toReturn;
		}
}