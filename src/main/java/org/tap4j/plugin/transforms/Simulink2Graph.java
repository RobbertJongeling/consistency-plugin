package org.tap4j.plugin.transforms;

import com.google.common.base.Objects;
import java.io.File;
import java.io.PrintStream;
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
  
  private PrintStream logger;
  
  public Simulink2Graph(final PrintStream logger, final String filepath, final String fqn) {
    this.logger = logger;
    this.filePath = filepath;
    this.fqn = fqn;
  }
  
  @Override
  public Node doTransform() {
    try {
      File file = new File(this.filePath);
      SimpleLogger _simpleLogger = new SimpleLogger();
      SimulinkModelBuilder builder = new SimulinkModelBuilder(file, _simpleLogger);
      SimulinkModel model = builder.buildModel();
      return this.getTree(model);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public Node getTree(final SimulinkModel model) {
    if ((Objects.equal(this.fqn, "") || Objects.equal(model.getName(), this.fqn))) {
      String _name = model.getName();
      String _name_1 = model.getName();
      Node root = new Node("model", _name, _name_1, "opt");
      UnmodifiableCollection<SimulinkBlock> _subBlocks = model.getSubBlocks();
      for (final SimulinkBlock block : _subBlocks) {
        root.addChild(this.getTree(block, model.getName()));
      }
      return root;
    } else {
      return this.getTreeFromTopBlock(model.getSubBlocks(), model.getName());
    }
  }
  
  public Node getTreeFromTopBlock(final UnmodifiableCollection<SimulinkBlock> subblocks, final String prefix) {
    if (((subblocks != null) && (subblocks.size() > 0))) {
      for (final SimulinkBlock b : subblocks) {
        String _name = b.getName();
        String _plus = ((prefix + "/") + _name);
        String _fix = this.fix(_plus);
        boolean _equals = Objects.equal(_fix, this.fqn);
        if (_equals) {
          return this.getTree(b, prefix);
        } else {
          UnmodifiableCollection<SimulinkBlock> _subBlocks = b.getSubBlocks();
          String _name_1 = b.getName();
          String _plus_1 = ((prefix + "/") + _name_1);
          Node n = this.getTreeFromTopBlock(_subBlocks, _plus_1);
          if ((n != null)) {
            return n;
          }
        }
      }
      return null;
    } else {
      return null;
    }
  }
  
  public String fix(final String toFix) {
    return toFix.replace(" ", "").replace("\n", "").replace("\r", "");
  }
  
  public Node getTree(final SimulinkBlock block, final String prefix) {
    String _name = block.getName();
    String name = ((prefix + "/") + _name);
    String _xifexpression = null;
    boolean _endsWith = block.getType().endsWith("port");
    if (_endsWith) {
      _xifexpression = "Port";
    } else {
      _xifexpression = block.getType();
    }
    String type = _xifexpression;
    String _name_1 = block.getName();
    Node toReturn = new Node(type, name, _name_1);
    UnmodifiableCollection<SimulinkBlock> _subBlocks = block.getSubBlocks();
    for (final SimulinkBlock b : _subBlocks) {
      toReturn.addChild(this.getTree(b, name));
    }
    return toReturn;
  }
}
