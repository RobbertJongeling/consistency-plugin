package org.tap4j.plugin.transforms

import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil
import org.eclipse.uml2.uml.UMLPlugin
//import org.eclipse.papyrus.sysml14.definition.SysmlPackage
import org.eclipse.papyrus.sysml14.sysmlPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.papyrus.sysml14.deprecatedelements.FlowPort
import org.tap4j.plugin.model.Node
import java.io.PrintStream
import org.eclipse.papyrus.sysml14.sysmlPackage.Literals
import java.io.File

class SysML2Graph implements Lang2Graph {

	var ResourceSetImpl resourceSet
	var String filePath
	var String fqn
	var PrintStream logger

	new(PrintStream logger, String filePath, String fqn) {
		this.logger = logger
		this.filePath = filePath
		this.fqn = fqn
		doResourceSetup()
	}

	def doResourceSetup() {
		UMLResourcesUtil.init(resourceSet)
		resourceSet = new ResourceSetImpl()

		resourceSet.packageRegistry.put(sysmlPackage.eNS_URI, sysmlPackage.eINSTANCE)

//		val prefix = "jar:file:/Applications/Eclipse-P2.app/Contents/Eclipse/plugins/org.eclipse.papyrus.sysml14_1.3.0.jar!/resources/profile/SysML.profile.uml";		
		val prefix = "SysML.profile.uml";
		var File file = new File(prefix)
		logger.println("file exists: " + file.exists + " file can be read: " + file.canRead)

		UMLPlugin.EPackageNsURIToProfileLocationMap.put(sysmlPackage.eNS_URI, URI.createURI(prefix + "#SysML"))

		logger.println("locationmapped: " + UMLPlugin.EPackageNsURIToProfileLocationMap.get(sysmlPackage.eNS_URI))
	}

	override Node doTransform() {
		// stub
//  	var Node n = new Node("testSysMLType", "testSysMLName", "testSysMLOptional")
//  	var Node m = new Node("testSysMLTypeChild", "testSysMLNameChild", "testSysMLOptionalChild")
//  	n.addChild(m)
// 		return n
		val resource = resourceSet.getResource(URI.createURI(filePath), true)
		logger.println("created resource for uri: " + filePath + ". resource: " + resource)
		return resource.tree
	}

	/**
	 * This method should return a list of serializations of the model in the resource
	 * the top element should correspond to the topFQN.
	 * The top element can be either model, package or class (block).
	 */
	def Node getTree(Resource resource) {
		var Node toReturn = new Node("null", "null", "null")

		logger.println("resource contents length: " + resource.contents.length)

		for (model : resource.contents.filter(org.eclipse.uml2.uml.Model)) {
			if (true /*model.name == fqn*/ ) { // == maps to Object.equals in Xtend //FOR TESTING, ALWAYS MATCH MODEL TOTO FIX
				logger.println("model " + model.name + "; applied stereotypes: " + model.appliedStereotypes.toString)
				logger.println("model " + model.name + "; applicable stereotypes: " +
					model.allApplicableStereotypes.toString)

				for (st : model.ownedStereotypes) {
					logger.println("model st: " + st.toString)
				}

				toReturn = getTree(model)
			} else {
				// check if there is a package or class that is the top element.
				for (pkg : model.allOwnedElements.filter(org.eclipse.uml2.uml.Package)) {
					if (getFQN(pkg) == fqn) {
						toReturn = new Node("Rootblock", pkg.name)

						// recursively check for containing packages
						for (p : pkg.ownedElements.filter(org.eclipse.uml2.uml.Package)) {
							toReturn.addChild(getTree(p, pkg.name))
						}

						// serialize containing classes
						for (c : pkg.ownedElements.filter(org.eclipse.uml2.uml.Class)) {
							logger.println("class " + c.name + "; stereotype: " + c.appliedStereotypes.toString)
							if (c.getAppliedStereotype("SysML::Blocks::Block") !== null) {
								toReturn.addChild(getTree(c, pkg.name))
							}
						}
					}
				}
				for (clazz : model.allOwnedElements.filter(org.eclipse.uml2.uml.Class)) {
					if (getFQN(clazz) == fqn) {
						// TODO call this rootblock? or something like rootclass
						toReturn = new Node("Rootblock", clazz.name)

						// serialize containing classes
						for (c : clazz.ownedElements.filter(org.eclipse.uml2.uml.Class)) {
							logger.println("class " + c.name + "; stereotype: " + c.appliedStereotypes.toString)
							if (c.getAppliedStereotype("SysML::Blocks::Block") !== null) {
								toReturn.addChild(getTree(c, clazz.name))
							}
						}

						// add all ports
						for (p : clazz.ownedElements.filter(org.eclipse.uml2.uml.Port)) {
							logger.println("port " + p.name + "; stereotype: " + p.appliedStereotypes.toString)
							val fps = p.getAppliedStereotype("SysML::DeprecatedElements::FlowPort")
							if (fps !== null) {
								val fp = p.getStereotypeApplication(fps) as FlowPort
								val typename = if(p.type === null) "" else p.type.name
								toReturn.addChild(
									new Node(toFirstUpper(fp.direction.getName()) + "port", clazz.name + "/" + p.name,
										"Bus: " + typename))
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
		val name = model.name
		var Node toReturn = new Node("Rootblock", name)

		for (p : model.ownedElements.filter(org.eclipse.uml2.uml.Package)) {
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

		// recursively check for containing packages
		for (p : pkg.ownedElements.filter(org.eclipse.uml2.uml.Package)) {
			toReturn.addChild(getTree(p, name))
		}

		// serialize containing classes
		for (c : pkg.ownedElements.filter(org.eclipse.uml2.uml.Class)) {

			val blockStereotype = c.getAppliedStereotype("SysML::Blocks::Block")
			if (blockStereotype !== null) {
				val block = c.getStereotypeApplication(blockStereotype)
				logger.println(c.name + ": block stereotype: " + block.toString())
			} else {
				logger.println(c.name + ": getStereotypeApplication is null")
			}
			logger.println("class " + c.name + "; stereotype: " + c.appliedStereotypes.toString)
//			if(c.getAppliedStereotype("SysML::Blocks::Block") !== null) {
			toReturn.addChild(getTree(c, name))
//			}
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

		// add all ports
		for (p : clazz.ownedElements.filter(org.eclipse.uml2.uml.Port)) {
			logger.println("port " + p.name + "; stereotype: " + p.appliedStereotypes.toString)
			val fps = p.getAppliedStereotype("SysML::DeprecatedElements::FlowPort")
			if (fps !== null) {
				val fp = p.getStereotypeApplication(fps) as FlowPort
				val typename = if(p.type === null) "" else p.type.name
				toReturn.addChild(
					new Node(toFirstUpper(fp.direction.getName()) + "port", name + "/" + p.name + "-Bus: " + typename))
			}
		}

		// recursively add all subsystems and subsubsystems etc.
		for (c : clazz.allOwnedElements.filter(org.eclipse.uml2.uml.Class)) {
			logger.println("class " + c.name + "; stereotype: " + c.appliedStereotypes.toString)
			if (c.getAppliedStereotype("SysML::Blocks::Block") !== null) {
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
		if (s !== null && s.length > 0) {
			return s.substring(0, 1).toUpperCase + s.substring(1, s.length - 1)
		} else {
			return "";
		}
	}
}