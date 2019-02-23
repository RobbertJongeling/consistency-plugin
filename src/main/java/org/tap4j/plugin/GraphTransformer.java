package org.tap4j.plugin;

import java.io.PrintStream;

import org.tap4j.plugin.model.ModelElement;
import org.tap4j.plugin.model.Node;
import org.tap4j.plugin.transforms.Simulink2Graph;
import org.tap4j.plugin.transforms.SysML2Graph;

public class GraphTransformer {

	public static Node transform(String workspace, ModelElement m) {
		return transform(System.out, workspace, m);
	}
	
	public static Node transform(PrintStream logger, String workspace, ModelElement m) {
		logger.println("transforming " + m.getFile());
		Node toReturn = new Node("","","");
		switch(m.getModelType()) {
		case "SysML":
			toReturn = transformSysML(workspace + "/" + m.getFile(), m.getFqn());
			break;
		case "Simulink":
			toReturn = transformSimulink(logger, workspace + "/" + m.getFile(), m.getFqn());
			break;
		}		
		return toReturn;
	}
	
	private static Node transformSysML(String filepath, String fqn) {
		SysML2Graph s2g = new SysML2Graph(filepath, fqn);
		Node toReturn = s2g.doTransform();
//		logger.println("transformed sysml: " + toReturn.name + ":" + toReturn.type + "-(" + toReturn.optional + ")");
//		logger.println(toReturn.toString());
//		logger.println(toReturn.toGraphviz());
		return toReturn;
	}
	
	private static Node transformSimulink(PrintStream logger, String filepath, String fqn) {
		Simulink2Graph s2g = new Simulink2Graph(logger, filepath, fqn);
		Node toReturn = s2g.doTransform();
//		logger.println("transformed simulink: " + toReturn.name + ":" + toReturn.type + "-(" + toReturn.optional + ")");
//		logger.println(toReturn.toString());
//		logger.println(toReturn.toGraphviz());
		return toReturn;
	}
}
