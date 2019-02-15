package org.tap4j.plugin.transforms;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.papyrus.sysml14.definition.SysmlPackage;
import org.eclipse.papyrus.sysml14.deprecatedelements.FlowPort;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPlugin;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;
import org.tap4j.plugin.model.Node;
import org.tap4j.plugin.transforms.Lang2Graph;

@SuppressWarnings("all")
public class SysML2Graph implements Lang2Graph {
  private ResourceSetImpl resourceSet;
  
  private String filePath;
  
  private String fqn;
  
  public SysML2Graph(final String filePath, final String fqn) {
    this.filePath = filePath;
    this.fqn = fqn;
    this.doResourceSetup();
  }
  
  public URI doResourceSetup() {
    URI _xblockexpression = null;
    {
      UMLResourcesUtil.init(this.resourceSet);
      ResourceSetImpl _resourceSetImpl = new ResourceSetImpl();
      this.resourceSet = _resourceSetImpl;
      this.resourceSet.getPackageRegistry().put(SysmlPackage.eNS_URI, SysmlPackage.eINSTANCE);
      final String prefix = "jar:file:Sysml2Text.jar!/SysML.profile.uml";
      _xblockexpression = UMLPlugin.getEPackageNsURIToProfileLocationMap().put(SysmlPackage.eNS_URI, URI.createURI((prefix + "#SysML")));
    }
    return _xblockexpression;
  }
  
  @Override
  public Node doTransform() {
    final Resource resource = this.resourceSet.getResource(URI.createURI(this.filePath), true);
    return this.getTree(resource);
  }
  
  /**
   * This method should return a list of serializations of the model in the resource
   * the top element should correspond to the topFQN.
   * The top element can be either model, package or class (block).
   */
  public Node getTree(final Resource resource) {
    Node toReturn = null;
    Iterable<Model> _filter = Iterables.<Model>filter(resource.getContents(), Model.class);
    for (final Model model : _filter) {
      String _name = model.getName();
      boolean _equals = Objects.equal(_name, this.fqn);
      if (_equals) {
        toReturn = this.getTree(model);
      } else {
        Iterable<org.eclipse.uml2.uml.Package> _filter_1 = Iterables.<org.eclipse.uml2.uml.Package>filter(model.allOwnedElements(), org.eclipse.uml2.uml.Package.class);
        for (final org.eclipse.uml2.uml.Package pkg : _filter_1) {
          String _fQN = this.getFQN(pkg);
          boolean _equals_1 = Objects.equal(_fQN, this.fqn);
          if (_equals_1) {
            String _name_1 = pkg.getName();
            Node _node = new Node("Rootblock", _name_1);
            toReturn = _node;
            Iterable<org.eclipse.uml2.uml.Package> _filter_2 = Iterables.<org.eclipse.uml2.uml.Package>filter(pkg.getOwnedElements(), org.eclipse.uml2.uml.Package.class);
            for (final org.eclipse.uml2.uml.Package p : _filter_2) {
              toReturn.addChild(this.getTree(p, pkg.getName()));
            }
            Iterable<org.eclipse.uml2.uml.Class> _filter_3 = Iterables.<org.eclipse.uml2.uml.Class>filter(pkg.getOwnedElements(), org.eclipse.uml2.uml.Class.class);
            for (final org.eclipse.uml2.uml.Class c : _filter_3) {
              Stereotype _appliedStereotype = c.getAppliedStereotype("SysML::Blocks::Block");
              boolean _tripleNotEquals = (_appliedStereotype != null);
              if (_tripleNotEquals) {
                toReturn.addChild(this.getTree(c, pkg.getName()));
              }
            }
          }
        }
        Iterable<org.eclipse.uml2.uml.Class> _filter_4 = Iterables.<org.eclipse.uml2.uml.Class>filter(model.allOwnedElements(), org.eclipse.uml2.uml.Class.class);
        for (final org.eclipse.uml2.uml.Class clazz : _filter_4) {
          String _fQN_1 = this.getFQN(clazz);
          boolean _equals_2 = Objects.equal(_fQN_1, this.fqn);
          if (_equals_2) {
            String _name_2 = clazz.getName();
            Node _node_1 = new Node("Rootblock", _name_2);
            toReturn = _node_1;
            Iterable<org.eclipse.uml2.uml.Class> _filter_5 = Iterables.<org.eclipse.uml2.uml.Class>filter(clazz.getOwnedElements(), org.eclipse.uml2.uml.Class.class);
            for (final org.eclipse.uml2.uml.Class c_1 : _filter_5) {
              Stereotype _appliedStereotype_1 = c_1.getAppliedStereotype("SysML::Blocks::Block");
              boolean _tripleNotEquals_1 = (_appliedStereotype_1 != null);
              if (_tripleNotEquals_1) {
                toReturn.addChild(this.getTree(c_1, clazz.getName()));
              }
            }
            Iterable<Port> _filter_6 = Iterables.<Port>filter(clazz.getOwnedElements(), Port.class);
            for (final Port p_1 : _filter_6) {
              {
                final Stereotype fps = p_1.getAppliedStereotype("SysML::DeprecatedElements::FlowPort");
                if ((fps != null)) {
                  EObject _stereotypeApplication = p_1.getStereotypeApplication(fps);
                  final FlowPort fp = ((FlowPort) _stereotypeApplication);
                  String _xifexpression = null;
                  Type _type = p_1.getType();
                  boolean _tripleEquals = (_type == null);
                  if (_tripleEquals) {
                    _xifexpression = "";
                  } else {
                    _xifexpression = p_1.getType().getName();
                  }
                  final String typename = _xifexpression;
                  String _firstUpper = this.toFirstUpper(fp.getDirection().getName());
                  String _plus = (_firstUpper + "port");
                  String _name_3 = clazz.getName();
                  String _plus_1 = (_name_3 + "/");
                  String _name_4 = p_1.getName();
                  String _plus_2 = (_plus_1 + _name_4);
                  Node _node_2 = new Node(_plus, _plus_2, ("Bus: " + typename));
                  toReturn.addChild(_node_2);
                }
              }
            }
          }
        }
      }
    }
    return toReturn;
  }
  
  public Node getTree(final Model model) {
    Node toReturn = null;
    final String name = model.getName();
    Node _node = new Node("Rootblock", name);
    toReturn = _node;
    Iterable<org.eclipse.uml2.uml.Package> _filter = Iterables.<org.eclipse.uml2.uml.Package>filter(model.getOwnedElements(), org.eclipse.uml2.uml.Package.class);
    for (final org.eclipse.uml2.uml.Package p : _filter) {
      toReturn.addChild(this.getTree(p, name));
    }
    return toReturn;
  }
  
  public Node getTree(final org.eclipse.uml2.uml.Package pkg, final String prefix) {
    Node toReturn = null;
    String _name = pkg.getName();
    final String name = ((prefix + "/") + _name);
    Node _node = new Node("SubSystem", name);
    toReturn = _node;
    Iterable<org.eclipse.uml2.uml.Package> _filter = Iterables.<org.eclipse.uml2.uml.Package>filter(pkg.getOwnedElements(), org.eclipse.uml2.uml.Package.class);
    for (final org.eclipse.uml2.uml.Package p : _filter) {
      toReturn.addChild(this.getTree(p, name));
    }
    Iterable<org.eclipse.uml2.uml.Class> _filter_1 = Iterables.<org.eclipse.uml2.uml.Class>filter(pkg.getOwnedElements(), org.eclipse.uml2.uml.Class.class);
    for (final org.eclipse.uml2.uml.Class c : _filter_1) {
      Stereotype _appliedStereotype = c.getAppliedStereotype("SysML::Blocks::Block");
      boolean _tripleNotEquals = (_appliedStereotype != null);
      if (_tripleNotEquals) {
        toReturn.addChild(this.getTree(c, name));
      }
    }
    return toReturn;
  }
  
  /**
   * This method should return a list of serializations of all elements in the model
   */
  public Node getTree(final org.eclipse.uml2.uml.Class clazz, final String prefix) {
    Node toReturn = null;
    String _name = clazz.getName();
    final String name = ((prefix + "/") + _name);
    Node _node = new Node("SubSystem", name);
    toReturn = _node;
    Iterable<Port> _filter = Iterables.<Port>filter(clazz.getOwnedElements(), Port.class);
    for (final Port p : _filter) {
      {
        final Stereotype fps = p.getAppliedStereotype("SysML::DeprecatedElements::FlowPort");
        if ((fps != null)) {
          EObject _stereotypeApplication = p.getStereotypeApplication(fps);
          final FlowPort fp = ((FlowPort) _stereotypeApplication);
          String _xifexpression = null;
          Type _type = p.getType();
          boolean _tripleEquals = (_type == null);
          if (_tripleEquals) {
            _xifexpression = "";
          } else {
            _xifexpression = p.getType().getName();
          }
          final String typename = _xifexpression;
          String _firstUpper = this.toFirstUpper(fp.getDirection().getName());
          String _plus = (_firstUpper + "port");
          String _name_1 = p.getName();
          String _plus_1 = ((name + "/") + _name_1);
          String _plus_2 = (_plus_1 + "-Bus: ");
          String _plus_3 = (_plus_2 + typename);
          Node _node_1 = new Node(_plus, _plus_3);
          toReturn.addChild(_node_1);
        }
      }
    }
    Iterable<org.eclipse.uml2.uml.Class> _filter_1 = Iterables.<org.eclipse.uml2.uml.Class>filter(clazz.allOwnedElements(), org.eclipse.uml2.uml.Class.class);
    for (final org.eclipse.uml2.uml.Class c : _filter_1) {
      Stereotype _appliedStereotype = c.getAppliedStereotype("SysML::Blocks::Block");
      boolean _tripleNotEquals = (_appliedStereotype != null);
      if (_tripleNotEquals) {
        toReturn.addChild(this.getTree(c, name));
      }
    }
    return toReturn;
  }
  
  public String getFQN(final Element e) {
    if ((e instanceof org.eclipse.uml2.uml.Class)) {
      return this.getFQN(((org.eclipse.uml2.uml.Class) e));
    }
    if ((e instanceof org.eclipse.uml2.uml.Package)) {
      return this.getFQN(((org.eclipse.uml2.uml.Package) e));
    }
    return "";
  }
  
  public String getFQN(final org.eclipse.uml2.uml.Package pkg) {
    String _xifexpression = null;
    Element _owner = pkg.getOwner();
    boolean _tripleEquals = (_owner == null);
    if (_tripleEquals) {
      _xifexpression = pkg.getName();
    } else {
      String _fQN = this.getFQN(pkg.getOwner());
      String _plus = (_fQN + "/");
      String _name = pkg.getName();
      _xifexpression = (_plus + _name);
    }
    return _xifexpression;
  }
  
  public String getFQN(final org.eclipse.uml2.uml.Class clazz) {
    String _xifexpression = null;
    Element _owner = clazz.getOwner();
    boolean _tripleEquals = (_owner == null);
    if (_tripleEquals) {
      _xifexpression = clazz.getName();
    } else {
      String _fQN = this.getFQN(clazz.getOwner());
      String _plus = (_fQN + "/");
      String _name = clazz.getName();
      _xifexpression = (_plus + _name);
    }
    return _xifexpression;
  }
  
  public String toFirstUpper(final String s) {
    if (((s != null) && (s.length() > 0))) {
      String _upperCase = s.substring(0, 1).toUpperCase();
      int _length = s.length();
      int _minus = (_length - 1);
      String _substring = s.substring(1, _minus);
      return (_upperCase + _substring);
    } else {
      return "";
    }
  }
}
