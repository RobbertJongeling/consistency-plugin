package org.tap4j.plugin.transforms;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.PrintStream;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.papyrus.sysml14.deprecatedelements.FlowPort;
import org.eclipse.papyrus.sysml14.sysmlPackage;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPlugin;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.tap4j.plugin.model.Node;
import org.tap4j.plugin.transforms.Lang2Graph;

@SuppressWarnings("all")
public class SysML2Graph implements Lang2Graph {
  private ResourceSetImpl resourceSet;
  
  private String filePath;
  
  private String fqn;
  
  private PrintStream logger;
  
  public SysML2Graph(final PrintStream logger, final String filePath, final String fqn) {
    this.logger = logger;
    this.filePath = filePath;
    this.fqn = fqn;
    this.doResourceSetup();
  }
  
  public void doResourceSetup() {
    UMLResourcesUtil.init(this.resourceSet);
    ResourceSetImpl _resourceSetImpl = new ResourceSetImpl();
    this.resourceSet = _resourceSetImpl;
    this.resourceSet.getPackageRegistry().put(sysmlPackage.eNS_URI, sysmlPackage.eINSTANCE);
    final String prefix = "SysML.profile.uml";
    File file = new File(prefix);
    boolean _exists = file.exists();
    String _plus = ("file exists: " + Boolean.valueOf(_exists));
    String _plus_1 = (_plus + " file can be read: ");
    boolean _canRead = file.canRead();
    String _plus_2 = (_plus_1 + Boolean.valueOf(_canRead));
    this.logger.println(_plus_2);
    UMLPlugin.getEPackageNsURIToProfileLocationMap().put(sysmlPackage.eNS_URI, URI.createURI((prefix + "#SysML")));
    URI _get = UMLPlugin.getEPackageNsURIToProfileLocationMap().get(sysmlPackage.eNS_URI);
    String _plus_3 = ("locationmapped: " + _get);
    this.logger.println(_plus_3);
  }
  
  @Override
  public Node doTransform() {
    final Resource resource = this.resourceSet.getResource(URI.createURI(this.filePath), true);
    this.logger.println(((("created resource for uri: " + this.filePath) + ". resource: ") + resource));
    return this.getTree(resource);
  }
  
  /**
   * This method should return a list of serializations of the model in the resource
   * the top element should correspond to the topFQN.
   * The top element can be either model, package or class (block).
   */
  public Node getTree(final Resource resource) {
    Node toReturn = new Node("null", "null", "null");
    int _length = ((Object[])Conversions.unwrapArray(resource.getContents(), Object.class)).length;
    String _plus = ("resource contents length: " + Integer.valueOf(_length));
    this.logger.println(_plus);
    Iterable<Model> _filter = Iterables.<Model>filter(resource.getContents(), Model.class);
    for (final Model model : _filter) {
      if (true) {
        String _name = model.getName();
        String _plus_1 = ("model " + _name);
        String _plus_2 = (_plus_1 + "; applied stereotypes: ");
        String _string = model.getAppliedStereotypes().toString();
        String _plus_3 = (_plus_2 + _string);
        this.logger.println(_plus_3);
        String _name_1 = model.getName();
        String _plus_4 = ("model " + _name_1);
        String _plus_5 = (_plus_4 + "; applicable stereotypes: ");
        String _string_1 = model.allApplicableStereotypes().toString();
        String _plus_6 = (_plus_5 + _string_1);
        this.logger.println(_plus_6);
        EList<Stereotype> _ownedStereotypes = model.getOwnedStereotypes();
        for (final Stereotype st : _ownedStereotypes) {
          String _string_2 = st.toString();
          String _plus_7 = ("model st: " + _string_2);
          this.logger.println(_plus_7);
        }
        toReturn = this.getTree(model);
      } else {
        Iterable<org.eclipse.uml2.uml.Package> _filter_1 = Iterables.<org.eclipse.uml2.uml.Package>filter(model.allOwnedElements(), org.eclipse.uml2.uml.Package.class);
        for (final org.eclipse.uml2.uml.Package pkg : _filter_1) {
          String _fQN = this.getFQN(pkg);
          boolean _equals = Objects.equal(_fQN, this.fqn);
          if (_equals) {
            String _name_2 = pkg.getName();
            Node _node = new Node("Rootblock", _name_2);
            toReturn = _node;
            Iterable<org.eclipse.uml2.uml.Package> _filter_2 = Iterables.<org.eclipse.uml2.uml.Package>filter(pkg.getOwnedElements(), org.eclipse.uml2.uml.Package.class);
            for (final org.eclipse.uml2.uml.Package p : _filter_2) {
              toReturn.addChild(this.getTree(p, pkg.getName()));
            }
            Iterable<org.eclipse.uml2.uml.Class> _filter_3 = Iterables.<org.eclipse.uml2.uml.Class>filter(pkg.getOwnedElements(), org.eclipse.uml2.uml.Class.class);
            for (final org.eclipse.uml2.uml.Class c : _filter_3) {
              {
                String _name_3 = c.getName();
                String _plus_8 = ("class " + _name_3);
                String _plus_9 = (_plus_8 + "; stereotype: ");
                String _string_3 = c.getAppliedStereotypes().toString();
                String _plus_10 = (_plus_9 + _string_3);
                this.logger.println(_plus_10);
                Stereotype _appliedStereotype = c.getAppliedStereotype("SysML::Blocks::Block");
                boolean _tripleNotEquals = (_appliedStereotype != null);
                if (_tripleNotEquals) {
                  toReturn.addChild(this.getTree(c, pkg.getName()));
                }
              }
            }
          }
        }
        Iterable<org.eclipse.uml2.uml.Class> _filter_4 = Iterables.<org.eclipse.uml2.uml.Class>filter(model.allOwnedElements(), org.eclipse.uml2.uml.Class.class);
        for (final org.eclipse.uml2.uml.Class clazz : _filter_4) {
          String _fQN_1 = this.getFQN(clazz);
          boolean _equals_1 = Objects.equal(_fQN_1, this.fqn);
          if (_equals_1) {
            String _name_3 = clazz.getName();
            Node _node_1 = new Node("Rootblock", _name_3);
            toReturn = _node_1;
            Iterable<org.eclipse.uml2.uml.Class> _filter_5 = Iterables.<org.eclipse.uml2.uml.Class>filter(clazz.getOwnedElements(), org.eclipse.uml2.uml.Class.class);
            for (final org.eclipse.uml2.uml.Class c_1 : _filter_5) {
              {
                String _name_4 = c_1.getName();
                String _plus_8 = ("class " + _name_4);
                String _plus_9 = (_plus_8 + "; stereotype: ");
                String _string_3 = c_1.getAppliedStereotypes().toString();
                String _plus_10 = (_plus_9 + _string_3);
                this.logger.println(_plus_10);
                Stereotype _appliedStereotype = c_1.getAppliedStereotype("SysML::Blocks::Block");
                boolean _tripleNotEquals = (_appliedStereotype != null);
                if (_tripleNotEquals) {
                  toReturn.addChild(this.getTree(c_1, clazz.getName()));
                }
              }
            }
            Iterable<Port> _filter_6 = Iterables.<Port>filter(clazz.getOwnedElements(), Port.class);
            for (final Port p_1 : _filter_6) {
              {
                String _name_4 = p_1.getName();
                String _plus_8 = ("port " + _name_4);
                String _plus_9 = (_plus_8 + "; stereotype: ");
                String _string_3 = p_1.getAppliedStereotypes().toString();
                String _plus_10 = (_plus_9 + _string_3);
                this.logger.println(_plus_10);
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
                  String _plus_11 = (_firstUpper + "port");
                  String _name_5 = clazz.getName();
                  String _plus_12 = (_name_5 + "/");
                  String _name_6 = p_1.getName();
                  String _plus_13 = (_plus_12 + _name_6);
                  Node _node_2 = new Node(_plus_11, _plus_13, 
                    ("Bus: " + typename));
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
    final String name = model.getName();
    Node toReturn = new Node("Rootblock", name);
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
      {
        final Stereotype blockStereotype = c.getAppliedStereotype("SysML::Blocks::Block");
        if ((blockStereotype != null)) {
          final EObject block = c.getStereotypeApplication(blockStereotype);
          String _name_1 = c.getName();
          String _plus = (_name_1 + ": block stereotype: ");
          String _string = block.toString();
          String _plus_1 = (_plus + _string);
          this.logger.println(_plus_1);
        } else {
          String _name_2 = c.getName();
          String _plus_2 = (_name_2 + ": getStereotypeApplication is null");
          this.logger.println(_plus_2);
        }
        String _name_3 = c.getName();
        String _plus_3 = ("class " + _name_3);
        String _plus_4 = (_plus_3 + "; stereotype: ");
        String _string_1 = c.getAppliedStereotypes().toString();
        String _plus_5 = (_plus_4 + _string_1);
        this.logger.println(_plus_5);
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
        String _name_1 = p.getName();
        String _plus = ("port " + _name_1);
        String _plus_1 = (_plus + "; stereotype: ");
        String _string = p.getAppliedStereotypes().toString();
        String _plus_2 = (_plus_1 + _string);
        this.logger.println(_plus_2);
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
          String _plus_3 = (_firstUpper + "port");
          String _name_2 = p.getName();
          String _plus_4 = ((name + "/") + _name_2);
          String _plus_5 = (_plus_4 + "-Bus: ");
          String _plus_6 = (_plus_5 + typename);
          Node _node_1 = new Node(_plus_3, _plus_6);
          toReturn.addChild(_node_1);
        }
      }
    }
    Iterable<org.eclipse.uml2.uml.Class> _filter_1 = Iterables.<org.eclipse.uml2.uml.Class>filter(clazz.allOwnedElements(), org.eclipse.uml2.uml.Class.class);
    for (final org.eclipse.uml2.uml.Class c : _filter_1) {
      {
        String _name_1 = c.getName();
        String _plus = ("class " + _name_1);
        String _plus_1 = (_plus + "; stereotype: ");
        String _string = c.getAppliedStereotypes().toString();
        String _plus_2 = (_plus_1 + _string);
        this.logger.println(_plus_2);
        Stereotype _appliedStereotype = c.getAppliedStereotype("SysML::Blocks::Block");
        boolean _tripleNotEquals = (_appliedStereotype != null);
        if (_tripleNotEquals) {
          toReturn.addChild(this.getTree(c, name));
        }
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
