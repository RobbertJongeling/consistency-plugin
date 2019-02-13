package org.tap4j.plugin.transforms

import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil
import org.eclipse.uml2.uml.UMLPlugin
import org.eclipse.papyrus.sysml14.definition.SysmlPackage
import org.eclipse.uml2.uml.util.UMLUtil;
import java.util.List
import java.util.LinkedList
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.papyrus.sysml14.deprecatedelements.FlowPort
import java.io.FileWriter
import java.io.BufferedWriter
import org.tap4j.plugin.model.Node
import org.eclipse.papyrus.sysml14.sysmlPackage.Literals

class SysML2Graph {
		
	var ResourceSetImpl resourceSet
	var String filePath
	var String fqn
	
	new(String filePath, String fqn) {
		this.filePath = filePath;
		doResourceSetup();
	}		
	
	def doResourceSetup() {
		UMLResourcesUtil.init(resourceSet)
		resourceSet = new ResourceSetImpl()
		
		resourceSet.packageRegistry.put(SysmlPackage.eNS_URI, SysmlPackage.eINSTANCE)
		val prefix = "jar:file:Sysml2Text.jar!/SysML.profile.uml";
		UMLPlugin.EPackageNsURIToProfileLocationMap.put(SysmlPackage.eNS_URI, URI.createURI(prefix + "#SysML"))
	}
	
  	def Node doTransform() {
  		//TODO implement
  		var Node n = new Node("testSysMLType", "testSysMLName", "testSysMLOptional")
  		var Node m = new Node("testSysMLTypeChild", "testSysMLNameChild", "testSysMLOptionalChild")
  		n.addChild(m)
  		return n
  	}
}
