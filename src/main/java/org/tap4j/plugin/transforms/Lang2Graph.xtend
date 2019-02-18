package org.tap4j.plugin.transforms

import org.tap4j.plugin.model.Node


interface Lang2Graph {
	def Node doTransform()
}
