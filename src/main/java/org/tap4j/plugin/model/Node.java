package org.tap4j.plugin.model;

import java.util.LinkedList;
import java.util.List;

public class Node {

		public String type;
		public String name;
		public String optional;
		public List<Node> children;

		Node() {
		  this("default", "default");
		}
		
		
		public Node(String type, String name) {
			this(type, name, "");
		}
		
		public Node(String type, String name, String optional) {
			this.type = type;
			this.name = name;
			this.optional = optional;
			this.children = new LinkedList<Node>();
		}
		
		public Node(String type, String name, List<Node> children) {
			this(type, name);			
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
		public boolean equals(Object o) {
			if(o instanceof Node) {
				Node n = (Node) o;
				if(n.type.equals(this.type) && n.name.equals(this.name)) {
					//ignoring children for now, since the type+name should uniquely identify a node anyway
					return true; 
				}
			}
			return false;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Node: " + name + ", of type: " + type + " with optional parameter: " + optional + "." + "\r\n");
			for(Node c : this.children) {
				sb.append(c.toString() + "\r\n");
			}
			return sb.toString(); 
		}
		
		/**
		 * 
		 * @param other
		 * @return empty list if no inconsistencies found, else a list of them.
		 */
		public List<String> getInconsistenciesWith(Node other) {
			List<String> toReturn = new LinkedList<String>();
			
			if(this.type != other.type) {
				toReturn.add("the type of " + this.toString() + " and " + other.toString() + " are not equal.");
			}
			
			//Now check children
			for(Node child : this.children) {
				boolean found = false;
				for(Node otherChild : other.children) {
					if(child.type == otherChild.type) {
						found = true;
					}
				}
				if(!found) {
					toReturn.add("the children of " + other.toString() + " do not contain a node of type " + child.type);
				}
			}
			
			//Check children symmetrically the other way
			for(Node child : this.children) {
				boolean found = false;
				for(Node otherChild : other.children) {
					if(child.type == otherChild.type) {
						found = true;
					}
				}
				if(!found) {
					toReturn.add("the children of " + other.toString() + " do not contain a node of type " + child.type);
				}
			}
			
			//now recursively check matched children somehow?
						
			
			return toReturn;
		}		
}
