package org.tap4j.plugin.transforms

import org.tap4j.plugin.model.Node

class Simulink2Graph implements Lang2Graph {
	
	//somehow, this should maybe create a script that is ran inside matlab?
	//let's google if there are alternatives
	//https://www.cqse.eu/en/products/simulink-library-for-java/overview/ is the way to go
	//using it the way these guys do it: https://github.com/cqse/test-analyzer (by creating a local maven repository)
	
	override doTransform() {
		//TODO implement
  		//stub
  		var Node n = new Node("testSimulinkType", "testSimulinkName", "testSimulinkOptional")
  		var Node m = new Node("testSimulinkTypeChild", "testSimulinkNameChild", "testSimulinkOptionalChild")
  		n.addChild(m)
  		return n
	}
}
