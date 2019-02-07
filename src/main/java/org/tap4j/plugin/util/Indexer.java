package org.tap4j.plugin.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import hudson.XmlFile;
import hudson.model.Run;
import jenkins.model.Jenkins;

public final class Indexer {

	public static void indexFilesAndElements() throws IOException {
		XmlFile allModels = new XmlFile(new File(Jenkins.getInstance().getRootDir(), ("/allModels.xml")));
		List<String> allModelsList = getAllModelFiles();
		allModels.write(allModelsList);
		XmlFile allModelElements = new XmlFile(new File(Jenkins.getInstance().getRootDir(), ("/allModelElements.xml")));
		allModelElements.write(getAllModelElements(allModelsList));
		
	}

	/**
     * should return all model files in the workspace 
     *TODO implement
     * @return
     */
    public static List<String> getAllModelFiles() {
    	List<String> toReturn = new LinkedList<String>();
    	toReturn.add("file1");
    	toReturn.add("file2");
    	toReturn.add("file3");
    	return toReturn;
    }
    
    /**
     * should return all modelelements in the given model files in the workspace 
     *TODO implement
     * @return
     */
    public static Map<String, List<String>> getAllModelElements(List<String> modelFiles) {
    	Map<String, List<String>> toReturn = new HashMap<>();    	
    	
    	for(String mf : modelFiles) {
    		toReturn.put(mf, Arrays.asList(mf+":element a", mf+":element b", mf+":element c"));
    	}
    	
    	return toReturn;
    }
}
