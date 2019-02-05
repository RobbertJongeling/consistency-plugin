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

	public ConsistencyChecksResult parse(FilePath results, Run build, PrintStream logger) throws IOException {
		this.parseErrors = false;
		this.hasFailedTests = false;
		this.logger = logger;
		
		ConsistencyChecksResult checksResult = null;
		
		if(results == null) {
			log("No Consistency Checks found. Returning empty checks results.");
			checksResult = new ConsistencyChecksResult("", "", build, new Config(new LinkedList<Entry>()));
		} else {
			XmlFile ccFile = new XmlFile(new File(results.getRemote()));
									
			// This reads the XML file into the checksResult object.
			if(ccFile.exists()) {
				try {
					checksResult = (ConsistencyChecksResult) ccFile.unmarshal(checksResult);
				} catch (Exception ex) {// could also be TapProjectAction.
					TapProjectAction tpa = null;
					tpa = (TapProjectAction) ccFile.unmarshal(tpa);
					checksResult = new ConsistencyChecksResult("Consistency Checks Results", results.getRemote(), build, tpa.getConfig());
				}
			}
		}
		
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
