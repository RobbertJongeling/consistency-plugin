package org.tap4j.plugin.transforms

import org.tap4j.plugin.model.Node
import java.io.File
import org.conqat.lib.commons.logging.SimpleLogger
import org.conqat.lib.simulink.model.SimulinkModel
import org.conqat.lib.simulink.builder.SimulinkModelBuilder
import org.conqat.lib.simulink.model.SimulinkBlock

class Simulink2Graph implements Lang2Graph {

	// somehow, this should maybe create a script that is ran inside matlab?
	// let's google if there are alternatives
	// https://www.cqse.eu/en/products/simulink-library-for-java/overview/ is the way to go
	// using it the way these guys do it: https://github.com/cqse/test-analyzer (by creating a local maven repository)
	var String filePath
	var String fqn

	new(String filepath, String fqn) {
		this.filePath = filepath
		this.fqn = fqn
	}

	override doTransform() {
		var File file = new File(filePath)
		var Node root = new Node("null", "null", "null")
		
		var SimulinkModelBuilder builder = new SimulinkModelBuilder(file, new SimpleLogger())
		var SimulinkModel model = builder.buildModel()

		root = new Node("model", model.name, "opt")
			
		for (SimulinkBlock block : model.subBlocks) {
			root.addChild(getTree(block, model.name))
		}
		
		return root
	}

	def Node getTree(SimulinkBlock block, String prefix) {
		var String name = prefix + "/" + block.name
//		var Node toReturn = new Node(block.type, name)
		var Node toReturn = new Node(block.type, block.name)

//		for (ip : block.inPorts) {
//			toReturn.addChild(new Node("inport", prefix + "/" + ip.index))
//		}
//		
//		for(op : block.outPorts) {
//			toReturn.addChild(new Node("outport", prefix + "/" +  op.index))
//		}
//		
		for (b : block.subBlocks) {
			toReturn.addChild(getTree(b, name))
		}
		
		return toReturn
	}
}
