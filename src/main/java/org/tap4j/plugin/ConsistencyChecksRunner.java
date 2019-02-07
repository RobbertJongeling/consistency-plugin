package org.tap4j.plugin;

import java.io.PrintStream;

import org.tap4j.plugin.TapProjectAction.Config;
import org.tap4j.plugin.TapProjectAction.ConsistencyRuleEntry;
import org.tap4j.plugin.TapProjectAction.Entry;
import org.tap4j.plugin.model.CheckResult;

import hudson.FilePath;

public class ConsistencyChecksRunner {

	private ConsistencyChecksResult ccr;
	private PrintStream logger;

	public ConsistencyChecksRunner(ConsistencyChecksResult ccr, PrintStream logger) {
		this.ccr = ccr;
		this.logger = logger;
	}

	public void runChecks() {
		Config config = ccr.getConfig();
		
		for(Entry e : config.getEntries()) {
			if (e instanceof ConsistencyRuleEntry) {
				ConsistencyRuleEntry cre = (ConsistencyRuleEntry) e;

				execute(cre);
			}
		}
		
		//now that we are done, set the ccr's config to the new one, s.t. we are ready to copy
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
	 * @param cre
	 */
	private void execute(ConsistencyRuleEntry cre) {
		//stub: set result of each test to pass, except skips
		if(! cre.getSkip()) {
			cre.setResult(CheckResult.PASS);
			cre.setResultText("Stub: for now setting everything to pass, unless the test is skipped");
		} else {
			cre.setResult(CheckResult.SKIP);
			cre.setResultText("Stub: for now just writing this here if test was skipped");
		}
	}
}
