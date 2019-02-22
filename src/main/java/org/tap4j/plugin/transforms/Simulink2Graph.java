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
        root.addChild(this.getTree(block, model.getName()));
      }
      return root;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public Node getTree(final SimulinkBlock block, final String prefix) {
    String _name = block.getName();
    String name = ((prefix + "/") + _name);
    String _type = block.getType();
    String _name_1 = block.getName();
    Node toReturn = new Node(_type, _name_1);
    UnmodifiableCollection<SimulinkBlock> _subBlocks = block.getSubBlocks();
    for (final SimulinkBlock b : _subBlocks) {
      toReturn.addChild(this.getTree(b, name));
    }
    return toReturn;
  }
}
