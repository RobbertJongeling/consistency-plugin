package org.tap4j.plugin.transforms

import org.tap4j.plugin.model.Node
import java.io.File
import org.conqat.lib.commons.logging.SimpleLogger
import org.conqat.lib.simulink.model.SimulinkModel
import org.conqat.lib.simulink.builder.SimulinkModelBuilder
import org.conqat.lib.simulink.model.SimulinkBlock
import java.util.LinkedList
import java.util.List
import org.conqat.lib.commons.collections.UnmodifiableCollection

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
		
		var SimulinkModelBuilder builder = new SimulinkModelBuilder(file, new SimpleLogger())
		var SimulinkModel model = builder.buildModel()

		return getTree(model);
	}
	
	def Node getTree(SimulinkModel model) {
		if(fqn == "" || model.name == fqn) {		
			var Node root = new Node("model", model.name, model.name, "opt")
			for (SimulinkBlock block : model.subBlocks) {
				root.addChild(getTree(block, model.name))
			}
			return root
		} else {
			return getTreeFromTopBlock(model.subBlocks, model.name)
		}
	}
	
	def Node getTreeFromTopBlock(UnmodifiableCollection<SimulinkBlock> subblocks, String prefix) {
		if(subblocks !== null && subblocks.size > 0) {
			for(SimulinkBlock b : subblocks) {
				if(prefix + "/" + b.name == fqn) {
					return getTree(b, prefix);
				} else {
					var Node n = getTreeFromTopBlock(b.subBlocks, prefix + "/" + b.name)
					if(n !== null) {
						return n
					}
				}
			}
		} else {
			return null
		}
	}

	def Node getTree(SimulinkBlock block, String prefix) {
		var String name = prefix + "/" + block.name
//		var Node toReturn = new Node(block.type, name)
		var Node toReturn = new Node(block.type, name, block.name)

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
