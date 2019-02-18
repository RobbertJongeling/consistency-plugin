package org.tap4j.plugin;

import java.io.PrintStream;

import org.tap4j.plugin.TapProjectAction.Config;
import org.tap4j.plugin.TapProjectAction.ConsistencyRuleEntry;
import org.tap4j.plugin.TapProjectAction.Entry;
import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.CheckStrictness;
import org.tap4j.plugin.model.CheckType;
import org.tap4j.plugin.model.ModelElement;
import org.tap4j.plugin.model.Node;
import org.tap4j.plugin.transforms.Simulink2Graph;
import org.tap4j.plugin.transforms.SysML2Graph;

import hudson.FilePath;
import jenkins.model.Jenkins;

public class ConsistencyChecksRunner {

	private ConsistencyChecksResult ccr;
	private PrintStream logger;
	private FilePath workspace;

	public ConsistencyChecksRunner(FilePath workspace, ConsistencyChecksResult ccr, PrintStream logger) {
		this.workspace = workspace;
		this.ccr = ccr;
		this.logger = logger;
	}

	public void runChecks() {
		Config config = ccr.getConfig();

		if (config != null) {
			for (Entry e : config.getEntries()) {
				if (e instanceof ConsistencyRuleEntry) {
					ConsistencyRuleEntry cre = (ConsistencyRuleEntry) e;

					execute(cre);
				}
			}
		}

		// now that we are done, set the ccr's config to the new one, s.t. we are ready
		// to copy
		ccr.setConfig(config);
	}

	public void saveResults(FilePath newPath) {
		if (this.ccr.saveConfig(newPath)) {
			logger.println("Check results saved OK");
		} else {
			logger.println("ERROR in saving Consistency Checks Results");
		}
	}

	/**
	 * executes this ConsistencyRuleEntry and sets the result appropriately.
	 * 
	 * @param cre
	 */
	private void execute(ConsistencyRuleEntry cre) {
		// stub: set result of each test to pass, except skips
		if (!cre.getSkip()) {
			logger.println("Running CRE: " + cre.toString());

			logger.println("Running transformations.");
			Node treeA = transform(cre.getA());
			Node treeB = transform(cre.getB());
			logger.println("Completed transformations, running comparisons");			
			
			GraphComparator gc = new GraphComparator(treeA, treeB);
			gc.doCompare(cre.getChecktype(), cre.getStrictness());
			logger.println("Completed comparisons");	
						
			CheckResult result = gc.getResult();
			String resultText = gc.getResultText();

			cre.setResult(result);

			// if the result was the same and the test was muted, then actually mute
			if (cre.getMute()) {
				logger.println("check muted, old text: " + cre.getResultText());
				if (cre.getResultText().equals(resultText)) {
					logger.println("Setting check result to Mute");
					cre.setResult(CheckResult.MUTE);
				}
			}

			cre.setResultText(resultText);

		} else {
			cre.setResult(CheckResult.SKIP);
			cre.setResultText("Test was skipped");
		}
	}
	
	private Node transform(ModelElement m) {
		Node toReturn = null;
		logger.println("transforming model element of type: " + m.getModelType());
		switch(m.getModelType()) {
		case "SysML":
			logger.println("getFile of modelElement: " + m.getFile());
			transformSysML(workspace + "/" + m.getFile(), m.getFqn());
			break;
		case "Simulink":
			transformSimulink(workspace + "/" + m.getFile(), m.getFqn());
			break;
		}
		
		return toReturn;
	}
	
	private Node transformSysML(String filepath, String fqn) {
		SysML2Graph s2g = new SysML2Graph(filepath, fqn);
		Node toReturn = s2g.doTransform();
		logger.println("transformed sysml: " + toReturn.name + ":" + toReturn.type + "-(" + toReturn.optional + ")");
		return toReturn;
	}
	
	private Node transformSimulink(String filepath, String fqn) {
		Simulink2Graph s2g = new Simulink2Graph(filepath, fqn);
		Node toReturn = s2g.doTransform();
		logger.println("transformed simulink: " + toReturn.name + ":" + toReturn.type + "-(" + toReturn.optional + ")");
		return toReturn;
	}
}
