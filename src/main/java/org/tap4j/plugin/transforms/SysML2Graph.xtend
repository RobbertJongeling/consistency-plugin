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
		val prefix = "jar:file:Sysml2Text.jar!/SysML.profile.uml";//TODO fix this reference
		UMLPlugin.EPackageNsURIToProfileLocationMap.put(SysmlPackage.eNS_URI, URI.createURI(prefix + "#SysML"))
	}
	
  	def Node doTransform() {
  		//TODO implement
  		//stub
//  		var Node n = new Node("testSysMLType", "testSysMLName", "testSysMLOptional")
//  		var Node m = new Node("testSysMLTypeChild", "testSysMLNameChild", "testSysMLOptionalChild")
//  		n.addChild(m)
//  		return toReturn
  		
  		val resource = resourceSet.getResource(URI.createURI(filePath), true)
  		return getTree(resource);
  	}
  	
  	/**
  	 * This method should return a list of serializations of the model in the resource
  	 * the top element should correspond to the topFQN.
  	 * The top element can be either model, package or class (block).
  	 */
  	def Node getTree(Resource resource) {
  		var Node toReturn
  		
	 	for (model : resource.contents.filter(org.eclipse.uml2.uml.Model)) {
	 		if(model.name == fqn) { //== maps to Object.equals in Xtend
	 			toReturn = getTree(model)
	 		} else {
	 			//check if there is a package or class that is the top element.
	 			for (pkg : model.allOwnedElements.filter(org.eclipse.uml2.uml.Package)) {
					if(getFQN(pkg) == fqn) {
						toReturn = new Node("Rootblock", pkg.name)
						
						//recursively check for containing packages
						for(p : pkg.ownedElements.filter(org.eclipse.uml2.uml.Package)) {
							toReturn.addChild(getTree(p, pkg.name))
						}
						
						//serialize containing classes
						for(c : pkg.ownedElements.filter(org.eclipse.uml2.uml.Class)) {
							if(c.getAppliedStereotype("SysML::Blocks::Block") !== null) {
								toReturn.addChild(getTree(c, pkg.name))
							}
						}
					}
				}
			    for (clazz : model.allOwnedElements.filter(org.eclipse.uml2.uml.Class)) {
					if(getFQN(clazz) == fqn) {
						//TODO call this rootblock? or something like rootclass
						toReturn = new Node("Rootblock", clazz.name) 
						
						//serialize containing classes
						for(c : clazz.ownedElements.filter(org.eclipse.uml2.uml.Class)) {
							if(c.getAppliedStereotype("SysML::Blocks::Block") !== null) {
								toReturn.addChild(getTree(c, clazz.name))
							}
						}
						
						//add all ports
						for(p : clazz.ownedElements.filter(org.eclipse.uml2.uml.Port)) {
							val fps = p.getAppliedStereotype("SysML::DeprecatedElements::FlowPort")
							if(fps !== null) {
								val fp = p.getStereotypeApplication(fps) as FlowPort
								val typename = if (p.type === null) "" else p.type.name
								toReturn.addChild(new Node(toFirstUpper(fp.direction.getName()) + "port", clazz.name + "/" + p.name, "Bus: " + typename))
							}
						}
					}
				}	 			
	 		}
		}
  		
  		return toReturn
  	}
  	
  	/**
  	 * 
  	 */
  	def Node getTree(org.eclipse.uml2.uml.Model model) {
  		var Node toReturn
  		
  		val name = model.name
		toReturn = new Node("Rootblock", name)
		
		for(p : model.ownedElements.filter(org.eclipse.uml2.uml.Package)) {
			toReturn.addChild(getTree(p, name))
		}
		
  		return toReturn
  	}
  	
  	/**
  	 * 
  	 */
  	def Node getTree(org.eclipse.uml2.uml.Package pkg, String prefix) {
  		var Node toReturn
  		
  		val name = prefix + "/" + pkg.name
  		toReturn = new Node("SubSystem", name)
		
		//recursively check for containing packages
		for(p : pkg.ownedElements.filter(org.eclipse.uml2.uml.Package)) {
			toReturn.addChild(getTree(p, name))
		}
		
		//serialize containing classes
		for(c : pkg.ownedElements.filter(org.eclipse.uml2.uml.Class)) {
			if(c.getAppliedStereotype("SysML::Blocks::Block") !== null) {
				toReturn.addChild(getTree(c, name))
			}
		}
		
  		return toReturn
  	}
  	
  	/**
  	 * This method should return a list of serializations of all elements in the model
  	 */
  	def Node getTree(org.eclipse.uml2.uml.Class clazz, String prefix) {
  		var Node toReturn
  		
  		val name = prefix + "/" + clazz.name
  		toReturn = new Node("SubSystem", name)
	 	
  		//add all ports
		for(p : clazz.ownedElements.filter(org.eclipse.uml2.uml.Port)) {
			val fps = p.getAppliedStereotype("SysML::DeprecatedElements::FlowPort")
			if(fps !== null) {
				val fp = p.getStereotypeApplication(fps) as FlowPort
				val typename = if (p.type === null) "" else p.type.name
				toReturn.addChild(new Node(toFirstUpper(fp.direction.getName()) + "port", name + "/" + p.name + "-Bus: " + typename))
			}
		}
  		
  		//recursively add all subsystems and subsubsystems etc.
		for(c : clazz.allOwnedElements.filter(org.eclipse.uml2.uml.Class)) {
			if(c.getAppliedStereotype("SysML::Blocks::Block") !== null) {
				toReturn.addChild(getTree(c, name))
			}
		}
		
  		return toReturn
  	}
  	
  	
  	def String getFQN(org.eclipse.uml2.uml.Element e) {
  		if (e instanceof org.eclipse.uml2.uml.Class) {
  			return getFQN(e as org.eclipse.uml2.uml.Class)
  		}
  		if (e instanceof org.eclipse.uml2.uml.Package) {
  			return getFQN(e as org.eclipse.uml2.uml.Package)
  		}
  		return ""
  	}
  	
  	def String getFQN(org.eclipse.uml2.uml.Package pkg) {
  		return if(pkg.owner === null) pkg.name else getFQN(pkg.owner) + "/" + pkg.name  		
  	}
  	
  	def String getFQN(org.eclipse.uml2.uml.Class clazz) {
  		return if(clazz.owner === null) clazz.name else getFQN(clazz.owner) + "/" + clazz.name
  	}
  	
  	def String toFirstUpper(String s) {
  		if(s !== null && s.length > 0) {
  			return s.substring(0,1).toUpperCase + s.substring(1,s.length-1)
  		} else { 
  			return "";
  		}
  	}
}
