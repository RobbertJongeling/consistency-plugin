package org.tap4j.plugin.transforms;

import org.tap4j.plugin.model.Node;
import org.tap4j.plugin.transforms.Lang2Graph;

@SuppressWarnings("all")
public class Simulink2Graph implements Lang2Graph {
  @Override
  public Node doTransform() {
    Node n = new Node("testSimulinkType", "testSimulinkName", "testSimulinkOptional");
    Node m = new Node("testSimulinkTypeChild", "testSimulinkNameChild", "testSimulinkOptionalChild");
    n.addChild(m);
    return n;
  }
}
