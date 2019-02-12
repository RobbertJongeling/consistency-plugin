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

			// stubbing
			CheckResult result = CheckResult.PASS;
			String resultText = "Stub: for now setting everything to pass, unless the test is skipped";

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
}
