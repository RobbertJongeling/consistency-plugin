package org.tap4j.plugin;

import java.io.PrintStream;

import org.tap4j.plugin.TapProjectAction.Config;
import org.tap4j.plugin.TapProjectAction.ConsistencyRuleEntry;
import org.tap4j.plugin.TapProjectAction.Entry;
import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.CheckResultEnum;
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
	private String workspace;

	public ConsistencyChecksRunner(String workspace, ConsistencyChecksResult ccr, PrintStream logger) {
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
			Node treeA = GraphTransformer.transform(logger, workspace, cre.getA());
			logger.println("done transforming: " + cre.getA().getFile());
//			if(treeA != null) {				
//				logger.println(treeA.toGraphviz());
//			}
			Node treeB = GraphTransformer.transform(logger, workspace, cre.getB());
			logger.println("done transforming: " + cre.getB().getFile());
//			if(treeB != null) {	
//				logger.println(treeB.toGraphviz());
//			}
			logger.println("Completed transformations, running comparisons");			
						
			CheckResult thisresult = GraphComparer.doCompare(logger, treeA, treeB, cre.getChecktype(), cre.getStrictness());
			logger.println("Completed comparisons");	

			cre.setResult(thisresult.getResult());

			// if the result was the same and the test was muted, then actually mute
			if (cre.getMute()) {
				logger.println("check muted, old text: " + cre.getResultText());
				if (cre.getResultText().equals(thisresult.getMessage())) {
					logger.println("Setting check result to Mute");
					cre.setResult(CheckResultEnum.MUTE);
				}
			}

			cre.setResultText(thisresult.getMessage());

		} else {
			cre.setResult(CheckResultEnum.SKIP);
			cre.setResultText("Test was skipped");
		}
	}
}
