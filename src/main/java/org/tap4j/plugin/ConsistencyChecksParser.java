package org.tap4j.plugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.tap4j.model.TestSet;
import org.tap4j.parser.ParserException;
import org.tap4j.parser.Tap13Parser;
import org.tap4j.plugin.TapProjectAction.Config;
import org.tap4j.plugin.TapProjectAction.ConsistencyRuleEntry;
import org.tap4j.plugin.TapProjectAction.Entry;
import org.tap4j.plugin.model.CheckResult;
import org.tap4j.plugin.model.ParseErrorTestSetMap;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.plugin.util.Constants;
import org.tap4j.plugin.util.Util;

import hudson.FilePath;
import hudson.XmlFile;
import hudson.model.Run;
import jenkins.model.Jenkins;

public class ConsistencyChecksParser {
	
	//TODO CheckResult can contain a ConsistencyRuleEntry and a ConsistencyResult (which would be just pass/fail with optional comments)
	private List<ConsistencyRuleEntry> checkResults;
	
	private boolean parseErrors, hasFailedTests;
	private PrintStream logger;
	private static final Logger log = Logger.getLogger(ConsistencyChecksParser.class.getName());

	public ConsistencyChecksResult parse(FilePath results, Run build, PrintStream logger) {
		this.parseErrors = false;
		this.hasFailedTests = false;
		this.logger = logger;
		final List<Entry> checkSets = new LinkedList<Entry>();
		
		String ccFilePath = "";
		
		if(results == null) {
			log("No Consistency Checks found. Returning empty checks results.");
		} else {
			ccFilePath = results.getRemote();
			XmlFile ccFile = new XmlFile(new File(results.getRemote()));
			
			try {
				log("printing xmlfile as string " + ccFile.getFile().getAbsolutePath());
				log(ccFile.asString());
			} catch (IOException e) {
				log("logging ccFile as String failed miserably. Just give up all hope.");
			}
		}
		
		log("adding dummy check result for testing");//TODO remove ofc.
//		checkSets.add(new CheckResult(new ConsistencyRuleEntry("test1", "test2", "strict", false, false ), true, "it works"));
//		checkSets.add(new CheckResult(new ConsistencyRuleEntry("test3", "test4", "loose", false, false ), false, "it doesn't work"));
		
		checkSets.add(new ConsistencyRuleEntry("test1", "test2", "strict", false, false, CheckResult.PASS, "it works" ));
		checkSets.add(new ConsistencyRuleEntry("test3", "test4", "loose", false, true, CheckResult.FAIL, "it doesn't work"));
		checkSets.add(new ConsistencyRuleEntry("test5", "test6", "medium", true, false, CheckResult.NYE, "not yet executed"));
		checkSets.add(new ConsistencyRuleEntry("test7", "test8", "medium", true, true, CheckResult.PASS, "it works"));
		
		final ConsistencyChecksResult checksResult = new ConsistencyChecksResult("Consistency Checks Results", ccFilePath, build, new Config(checkSets));
		return checksResult;
	}

	private void log(String str) {
        if (logger != null) {
            logger.println(str);
        }
    }

    private void log(Exception ex) {
        if (logger != null) {
            ex.printStackTrace(logger);
        } else {
            log.severe(ex.toString());
        }
    }
	
}
