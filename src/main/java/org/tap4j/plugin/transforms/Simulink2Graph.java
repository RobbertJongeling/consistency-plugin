package org.tap4j.plugin.transforms;

import java.io.File;
import org.conqat.lib.commons.collections.UnmodifiableCollection;
import org.conqat.lib.commons.logging.SimpleLogger;
import org.conqat.lib.simulink.builder.SimulinkModelBuilder;
import org.conqat.lib.simulink.model.SimulinkBlock;
import org.conqat.lib.simulink.model.SimulinkModel;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.tap4j.plugin.model.Node;
import org.tap4j.plugin.transforms.Lang2Graph;

@SuppressWarnings("all")
public class Simulink2Graph implements Lang2Graph {
  private String filePath;
  
  private String fqn;
  
  public Simulink2Graph(final String filepath, final String fqn) {
    this.filePath = filepath;
    this.fqn = fqn;
  }
  
  @Override
  public Node doTransform() {
    try {
      File file = new File(this.filePath);
      Node root = new Node("null", "null", "null");
      SimpleLogger _simpleLogger = new SimpleLogger();
      SimulinkModelBuilder builder = new SimulinkModelBuilder(file, _simpleLogger);
      SimulinkModel model = builder.buildModel();
      String _name = model.getName();
      Node _node = new Node("model", _name, "opt");
      root = _node;
      UnmodifiableCollection<SimulinkBlock> _subBlocks = model.getSubBlocks();
      for (final SimulinkBlock block : _subBlocks) {
        String _name_1 = block.getName();
        Node _node_1 = new Node("Block", _name_1);
        root.addChild(_node_1);
      }
      return root;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public Node getTree() {
    return null;
  }
}
