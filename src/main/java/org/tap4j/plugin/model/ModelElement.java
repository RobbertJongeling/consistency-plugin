package org.tap4j.plugin.model;

public class ModelElement {
	private String modelType;
	private String file;
	private String fqn;
	
	public ModelElement(String modelType, String file, String fqn) {
		this.setModelType(modelType);
		this.setFile(file);
		this.setFqn(fqn);
	}

	public String getModelType() {
		return modelType;
	}
	
	public void setModelType(String modelType) {
		this.modelType = modelType;
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
	
	@Override
	public String toString() {
		return "model of type: " + modelType + ", defined in file: " + file + ", top-level-FQN: " + fqn; 
	}
	
}
