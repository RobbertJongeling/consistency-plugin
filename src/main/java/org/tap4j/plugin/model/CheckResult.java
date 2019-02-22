package org.tap4j.plugin.model;

public class CheckResult {
	private CheckResultEnum result;
	private String message;
	
	public CheckResult(CheckResultEnum result, String message) {
		this.setResult(result);
		this.setMessage(message);
	}

	public CheckResultEnum getResult() {
		return result;
	}

	public void setResult(CheckResultEnum result) {
		this.result = result;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
