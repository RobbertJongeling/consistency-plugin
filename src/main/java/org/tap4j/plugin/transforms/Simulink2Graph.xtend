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
import java.util.logging.Level
import java.util.logging.Logger
import org.tap4j.plugin.ConsistencyChecksResult
import java.io.PrintStream

class Simulink2Graph implements Lang2Graph {

	var String filePath
	var String fqn
	var PrintStream logger

	new(PrintStream logger, String filepath, String fqn) {
		this.logger = logger
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
		logger.println("getTreeFromTopBlock " + prefix + " sbs lenght: " + subblocks.size)
		if(subblocks !== null && subblocks.size > 0) {
			for(SimulinkBlock b : subblocks) {
				logger.println("checking for: " + b.name)
				if(fix(prefix + "/" + b.name) == fqn) {
					logger.println("found the topBlock!")
					return getTree(b, prefix)
				} else {
					logger.println(prefix + "/" + b.name + "!==" + fqn)
					logger.println("getting tree for subBlock")
					var Node n = getTreeFromTopBlock(b.subBlocks, prefix + "/" + b.name)
					if(n !== null) {
						logger.println("tree from subBlock is not null, found the correct top block!")
						return n
					} else {
						logger.println("tree from subBlock is null")
					}
				}
			}
			return null
		} else {
			return null
		}
	}
	
	//quick fix, copied from Node
	def fix(String toFix) {
		return toFix.replace(" ", "").replace("\n", "").replace("\r", "")
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
