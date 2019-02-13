package org.tap4j.plugin.transforms;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.papyrus.sysml14.definition.SysmlPackage;
import org.eclipse.uml2.uml.UMLPlugin;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;
import org.tap4j.plugin.model.Node;

@SuppressWarnings("all")
public class SysML2Graph {
  private ResourceSetImpl resourceSet;
  
  private String filePath;
  
  private String fqn;
  
  public SysML2Graph(final String filePath, final String fqn) {
    this.filePath = filePath;
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
  
  public Node doTransform() {
    Node n = new Node("testSysMLType", "testSysMLName", "testSysMLOptional");
    Node m = new Node("testSysMLTypeChild", "testSysMLNameChild", "testSysMLOptionalChild");
    n.addChild(m);
    return n;
  }
}
