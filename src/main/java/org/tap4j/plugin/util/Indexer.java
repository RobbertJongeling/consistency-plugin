package org.tap4j.plugin.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.tap4j.plugin.ConsistencyChecksResult;
import org.tap4j.plugin.GraphTransformer;
import org.tap4j.plugin.model.ModelElement;
import org.tap4j.plugin.model.Node;

import hudson.FilePath;
import hudson.XmlFile;
import hudson.model.Run;
import jenkins.model.Jenkins;

public final class Indexer {
	

	private static final Logger LOGGER = Logger.getLogger(ConsistencyChecksResult.class.getName());

	public static void indexFilesAndElements(Run<?, ?> build) throws IOException {
		XmlFile allModels = new XmlFile(new File(Jenkins.getInstance().getRootDir(), ("/allModels.xml")));
		List<String> allModelsList = getAllModelFiles(build);
		allModels.write(allModelsList);
		XmlFile allModelElements = new XmlFile(new File(Jenkins.getInstance().getRootDir(), ("/allModelElements.xml")));
		allModelElements.write(getAllModelElements(allModelsList, build));
	}

	/**
     * should return all model files in the workspace 
     *TODO implement
     * @return
     */
    public static List<String> getAllModelFiles(Run<?, ?> build) {
    	List<String> toReturn = new LinkedList<String>();
    	
    	if(build == null) {
        	toReturn.add("file1");
        	toReturn.add("file2");
        	toReturn.add("file3");    		
    	} else {
    		//TODO extend to other file extensions
    		String location = build.getParent().getRootDir() + "/workspace/";
    		File workspace = new File(location);
    		for(File f : FileUtils.listFiles(workspace, new RegexFileFilter("(^.*uml)|(^.*slx)|(^.*mdl)"), DirectoryFileFilter.DIRECTORY)) {
    			toReturn.add(f.getAbsolutePath().substring(location.length()));
    		}	
    	}
    	
    	return toReturn;
    }
    
    /**
     * should return all modelelements in the given model files in the workspace 
     *TODO implement
     * @return
     */
    public static Map<String, List<String>> getAllModelElements(List<String> modelFiles, Run<?, ?> build) {
    	Map<String, List<String>> toReturn = new HashMap<>();    	
    	
    	if(build == null) {
	    	for(String mf : modelFiles) {
	    		toReturn.put(mf, Arrays.asList(mf+":element a", mf+":element b", mf+":element c"));
	    	}
    	} else {
    		for(String mf : modelFiles) {
    			String modelType = mf.endsWith("uml") ? "SysML" : "Simulink"; //TODO fix ugly hardcoded
    			ModelElement m = new ModelElement(modelType, mf, "");
    			LOGGER.log(Level.INFO, "INDEXER: Going to transform: " + build.getParent().getRootDir() + "/workspace/" + m.toString());
    			Node transformed = GraphTransformer.transform(build.getParent().getRootDir() + "/workspace/", m);
    			LOGGER.log(Level.INFO, "INDEXER: Done transforming: " + transformed.toGraphviz());
    			toReturn.put(mf, transformed.toListOfString());
	    	}
    	}
    	
    	return toReturn;
    }
}
