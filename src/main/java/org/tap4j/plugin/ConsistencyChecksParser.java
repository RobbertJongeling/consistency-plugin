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
import org.tap4j.plugin.TapProjectAction.ConsistencyRuleEntry;
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
	private List<CheckResult> checkResults;
	
	private boolean parseErrors, hasFailedTests;
	private PrintStream logger;
	private static final Logger log = Logger.getLogger(ConsistencyChecksParser.class.getName());

	public ConsistencyChecksResult parse(FilePath results, Run build, PrintStream logger) {
		this.parseErrors = false;
		this.hasFailedTests = false;
		this.logger = logger;
		final List<CheckResult> checkSets = new LinkedList<CheckResult>();
		
		if(results == null) {
			log("No Consistency Checks found. Returning empty checks results.");
		} else {
			//this should be called only once I suppose,
			//since we save everything in this one consistencyChecks.xml 
			//but let's keep it generic
			//for (FilePath path : results) { 
			
			log("Getting this hardocded xml file");
//			XmlFile ccFile = new XmlFile(new File(path.getRemote()));
//			XmlFile ccFile = new XmlFile(new File(Jenkins.getInstance().getRootDir(), "consistencyChecks.xml"));
			XmlFile ccFile = new XmlFile(new File(results.getRemote()));
			
			try {
				log("printing xmlfile as string " + ccFile.getFile().getAbsolutePath());
				log(ccFile.asString());
			} catch (IOException e) {
				log("logging ccFile as String failed miserably. Just give up all hope.");
			}
			//}
		}
		
		log("adding dummy check result for testing");//TODO remove ofc.
		checkSets.add(new CheckResult(new ConsistencyRuleEntry("test1", "test2", "strict", false, false ), true, "it works"));
		checkSets.add(new CheckResult(new ConsistencyRuleEntry("test3", "test4", "loose", false, false ), false, "it doesn't work"));
		
		final ConsistencyChecksResult checksResult = new ConsistencyChecksResult("Consistency Checks Results", build, checkSets);
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
