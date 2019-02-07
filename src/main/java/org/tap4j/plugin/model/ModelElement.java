package org.tap4j.plugin.model;

public class ModelElement {
	private String file;
	private String fqn;
	
	public ModelElement(String file, String fqn) {
		this.setFile(file);
		this.setFqn(fqn);
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getFqn() {
		return fqn;
	}

	public void setFqn(String fqn) {
		this.fqn = fqn;
	}
	
	
}
