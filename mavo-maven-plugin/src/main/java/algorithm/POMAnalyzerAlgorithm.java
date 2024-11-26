package algorithm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.maven.model.Dependency;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cache.DependencyCache;
import common.Constants;
import common.dto.DependencyDTO;
import common.dto.POMDependencyDTO;
import common.dto.VersionDTO;
import pom.PomCreations;
import pom.PomOperations;
import recommendations.POMOptimization;
import recommendations.POMRecommendation;
import reporting.Reporting;
import versionmanagement.VersionFetcher;

public class POMAnalyzerAlgorithm {

	public static void pomAnalyzerAlgorithm(String originalPomPath, boolean excludeParentDependencies, 
			boolean groupRecommendations, boolean optimizeRecommendations, String patchRestriction)
	{
		
		System.out.println("\n----------------------------------- Step 1 - Exclude List -----------------------------------\n");
		String excludeListPath = originalPomPath+Constants.excludeListName;
		List<POMDependencyDTO> excludeList = getExcludeList(excludeListPath);
		
		for(POMDependencyDTO dependency : excludeList)
		{
			Constants.excludeList.put(dependency.getGroupId(), dependency.getArtifactId());
		}
		
		for (Map.Entry<String, String> entry : Constants.excludeList.entrySet()) {
			System.out.println("Exclude List - \n");
			System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
		}
		System.out.println("\n---------------------------------------------------------------------------------------------\n");

		
		System.out.println("\n----------------------------------- Step 2 - Cache ------------------------------------------\n");

		DependencyCache.createCache(originalPomPath);
		
		System.out.println("\n---------------------------------------------------------------------------------------------\n");

		
		System.out.println("\n----------------------------------- Step 3 - Parse Module -----------------------------------\n");
		
		parseModule(originalPomPath, patchRestriction);

		System.out.println("\n---------------------------------------------------------------------------------------------\n");

		
		System.out.println("\n----------------------------------- Step 4 - Vulnerability Module ---------------------------\n");
		
		vulnerabilityCheckModule(originalPomPath, originalPomPath, excludeParentDependencies, optimizeRecommendations);
		
		System.out.println("\n---------------------------------------------------------------------------------------------\n");

		
		updateDependencyMap(Constants.dependencyMap, Constants.keyModule);
		
		getDepMgmtRecommendations(originalPomPath);

		
		System.out.println("\n----------------------------------- Step 5 - Grouping Module --------------------------------\n");

		Map<String, List<POMDependencyDTO>> finalChanges = new HashMap<>();
		if(groupRecommendations)
		{
			finalChanges = getGroupRecommendations(Constants.dependencyMap, Constants.keyModule);			
		}
		else
		{
			finalChanges = getRecommendations(Constants.dependencyMap, Constants.keyModule);
		}
		
		System.out.println("\n---------------------------------------------------------------------------------------------\n");

		
		System.out.println("\n----------------------------------- Step 6 - Report Module ----------------------------------\n");

//		System.out.println("finalChanges - "+finalChanges);
		
		System.out.println("\n----------------------------------- Step 6a - Create Recommended POM -------------------------\n");

		PomCreations.createRecommendedPOM(originalPomPath, finalChanges);
		
		System.out.println("\n---------------------------------------------------------------------------------------------\n");

		System.out.println("\n----------------------------------- Step 6b - Create Report ---------------------------------\n");

		Reporting.createReport1(originalPomPath, originalPomPath);
		
		System.out.println("\n---------------------------------------------------------------------------------------------\n");

		
	}
	
	public static List<POMDependencyDTO> getExcludeList(String filePath)
	{
	    List<POMDependencyDTO> dependencies = new ArrayList<>();
	    try {
	        File xmlFile = new File(filePath);
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(xmlFile);
	
	        NodeList dependencyNodes = doc.getElementsByTagName("dependency");
	        for (int i = 0; i < dependencyNodes.getLength(); i++) {
	            Node node = dependencyNodes.item(i);
	            if (node.getNodeType() == Node.ELEMENT_NODE) {
	                Element element = (Element) node;
	
	                String groupId = element.getElementsByTagName("groupId").item(0).getTextContent();
	                String artifactId = element.getElementsByTagName("artifactId").item(0).getTextContent();
	                Node versionNode = element.getElementsByTagName("version").item(0);
	                String version = (versionNode != null) ? versionNode.getTextContent() : null;
	
	                dependencies.add(new POMDependencyDTO(groupId, artifactId, version));
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return dependencies;
	}
	
	public static void parseModule(String pomPath, String patchRestriction)
	{
		System.out.println("parse Module - "+pomPath);
		
		// temp pom creation and setup
		PomCreations.createAndSetupTempPOM(pomPath);
		
		// get properties
		String newPomFilePath = pomPath+Constants.tempPomFileDirectoryName+Constants.tempPomFileName;
		
		Map<String, String> propertiesMap = PomOperations.getPomProperties(newPomFilePath);
		Constants.globalpropertiesMap.putAll(propertiesMap);

		List<List<POMDependencyDTO>> dependencies = PomOperations.getSegregatedDependencies(newPomFilePath, true);
		
		List<POMDependencyDTO> parentDeps = dependencies.get(0);
		
		Constants.parentDependencies.addAll(parentDeps);
		List<POMDependencyDTO> externalDeps = dependencies.get(1);
		
		System.out.println("\n----------------------------------- Step 3a - Parent Upgrade --------------------------------\n");

		upgradeParentToLatestMajorPatch(parentDeps, newPomFilePath, patchRestriction);
		
		System.out.println("\n---------------------------------------------------------------------------------------------\n");
		System.out.println();

		String[] arr = pomPath.split("\\\\");
		String moduleName = arr[arr.length-1];
		PomOperations.addToDependencyMap(externalDeps, moduleName);
		
		List<String> modules = PomOperations.getPomModules(pomPath+Constants.pomFileName);
		for(String module : modules)
		{
			parseModule(pomPath+"\\"+module, patchRestriction);
		}
	}
	
	public static void vulnerabilityCheckModule(String originalPomPath, String pomPath, boolean excludeParentDependencies, boolean optimizeRecommendations)
	{
		System.out.println("Vulnerability Check Module - "+pomPath+"\n");
		
		String newPomFilePath = pomPath+Constants.tempPomFileDirectoryName+Constants.tempPomFileName;
		
		List<List<POMDependencyDTO>> dependencies = PomOperations.getSegregatedDependencies(newPomFilePath, false);
		
		List<POMDependencyDTO> externalDependencies = dependencies.get(1);
		System.out.println("ext dep count : "+externalDependencies.size());
		
		List<POMDependencyDTO> allDependencies = new ArrayList<>();

		if(excludeParentDependencies)
		{
			for(POMDependencyDTO dependency : externalDependencies)
			{
//				System.out.println("dependency - "+dependency.getArtifactId()+" - "+dependency.getVersion());
				boolean depInParent = false;
				for(POMDependencyDTO parDep : Constants.parentDependencies)
				{
//					System.out.println("parent - "+parDep.getArtifactId()+" - "+parDep.getVersion());
					if(parDep.getGroupId().equals(dependency.getGroupId()) && parDep.getVersion().equals(dependency.getVersion()))
					{
						depInParent = true;
						break;
					}
				}
				if(!depInParent)
				{
					allDependencies.add(dependency);
				}
			}
		}
		else
		{
			allDependencies.addAll(externalDependencies);			
		}
		
		System.out.println("all dependencies size - "+allDependencies.size());
		
		System.out.println("\n----------------------------------- Step 4a - Recommendation generation Module --------------\n");

		List<DependencyDTO> pomRecommendations = new ArrayList<>();
		for(POMDependencyDTO pomDependency : allDependencies)
		{
			
			//get recommendations for the pomDependency
			List<DependencyDTO> pomDependencyRecommendations = POMRecommendation.getRecommendationsForPOMDependency(pomDependency, originalPomPath);
			

			for(DependencyDTO p : pomDependencyRecommendations)
			{
				boolean pomFind = false;
				// this for loop is for removing the duplicate recommendations
				for(DependencyDTO p1 : pomRecommendations)
				{
					if(p1.getGroupId().equalsIgnoreCase(p.getGroupId()) && p1.getArtifactId().equalsIgnoreCase(p.getArtifactId()) && p1.getVersion().equalsIgnoreCase(p.getVersion()))
					{
						pomFind = true;
						break;
					}
				}
				if(!pomFind)
				{
					pomRecommendations.add(p);
				}
			}
		}
		
		System.out.println("\nRecommendations - \n");
		for(DependencyDTO d : pomRecommendations)
		{
			System.out.println(d.getGroupId());
			System.out.println(d.getArtifactId());
			System.out.println(d.getVersion());
			System.out.println();
		}
		
		System.out.println("\n---------------------------------------------------------------------------------------------\n");
		System.out.println("\n----------------------------------- Step 4b - Recommendation Optimization Module ------------\n");

		String tempPomFileDirectory = originalPomPath+Constants.tempPomFileDirectoryName;
		String tempPomFilePath = originalPomPath+Constants.tempPomFileDirectoryName+Constants.tempPomFileName;
		String tempPomDependencyCheckReportLocation = originalPomPath+Constants.tempPomFileDirectoryName+Constants.dependencyCheckReportName;
		
		Map<String, Integer> leastVulCountCombinationMap = new HashMap<>();
		if(pomRecommendations.size()>0)
		{
			if(optimizeRecommendations)
			{
				leastVulCountCombinationMap = POMOptimization.optimizePOMRecommendations(originalPomPath, externalDependencies, pomRecommendations, tempPomFilePath, tempPomFileDirectory, tempPomDependencyCheckReportLocation);
			}
			else
			{
				leastVulCountCombinationMap = POMOptimization.organizePOMRecommendations(originalPomPath, externalDependencies, pomRecommendations, tempPomFilePath, tempPomFileDirectory, tempPomDependencyCheckReportLocation);				
			}
		}
		
		System.out.println("Optimized Recommedations");
		for(String key : leastVulCountCombinationMap.keySet())
		{
			String[] arr = pomPath.split("\\\\");
			System.out.println(key+" : "+arr[arr.length-1]);
			Constants.keyModule.put(key, arr[arr.length-1]);
		}
		
		System.out.println("\n---------------------------------------------------------------------------------------------\n");

		List<String> modules = PomOperations.getPomModules(pomPath+Constants.pomFileName);
		
		for(String module : modules)
		{
			vulnerabilityCheckModule(originalPomPath, pomPath+"\\"+module, excludeParentDependencies, optimizeRecommendations);
		}
	}
	
	public static void updateDependencyMap(Map<String, List<String>> dependencyMap, Map<String, String> keyModule)
	{
		Map<String, String> changes = new HashMap<>();
		
		//update dependency Map
		for (Map.Entry<String, String> keymod : Constants.keyModule.entrySet()) {
//		    System.out.println("Key: " + keymod.getKey() + ", Value: " + keymod.getValue());
		    String key = keymod.getKey();
		    String value = keymod.getValue();
		    String[] dependencies = key.split(",");
		    for(String dependency : dependencies)
		    {
		    	String[] arr = dependency.split("#");
		    	String keyGroupId = arr[0];
		    	String keyArtifactId = arr[1];
		    	String keyVersion = arr[2];
		    	
				for (Map.Entry<String, List<String>> depMap : Constants.dependencyMap.entrySet()) {
					String mergedGIDVersion = depMap.getKey();
					String[] brr = mergedGIDVersion.split("_");
					String depMapGroupID = brr[0];
					String depMapVersion = brr[1];
					
					if(keyGroupId.equals(depMapGroupID))
					{
						long depMapTimeStamp = VersionFetcher.getTimeStampforDependency(depMapGroupID, keyArtifactId, depMapVersion);
						long keyTimeStamp = VersionFetcher.getTimeStampforDependency(keyGroupId, keyArtifactId, keyVersion);
						
						if(depMapTimeStamp<keyTimeStamp)
						{
							String newMergedGIDVersion = depMapGroupID+"_"+keyVersion;
							changes.put(mergedGIDVersion, newMergedGIDVersion);
						}
					}
				}
		    }
		}
		
		for (Map.Entry<String, String> change : changes.entrySet())
		{
			String oldkey = change.getKey();
			String newkey = change.getValue();
			
	        if (dependencyMap.containsKey(oldkey)) {
	        	List<String> artifactIds = dependencyMap.get(oldkey);
	        	dependencyMap.remove(oldkey);
	        	dependencyMap.put(newkey, artifactIds);
	        }
			
		}

//		System.out.println("########################## updated dependencyMap ########################");
//		for (Map.Entry<String, List<String>> entry : Constants.dependencyMap.entrySet()) {
//		    System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
//		}
	}
	
	public static void getDepMgmtRecommendations(String pomPath)
	{
//		System.out.println("Module - "+pomPath);
		String[] pomPathArr = pomPath.split("\\\\");
		String pomModule = pomPathArr[pomPathArr.length-1];
		
		List<Dependency> dependencies = PomOperations.getDepMgmtDepndenciesFromPOM(pomPath+Constants.pomFileName);
		
		for(Dependency dependency : dependencies)
		{
			String depGroupId = dependency.getGroupId();
			String depArtifactId = dependency.getArtifactId();
			String depversion = dependency.getVersion();
			if (depversion != null && depversion.startsWith("${") && depversion.endsWith("}")) {
                String propertyName = depversion.substring(2, depversion.length() - 1);
                depversion = Constants.globalpropertiesMap.get(propertyName);
			}
			
			boolean depFound = false;
			for (Map.Entry<String, List<String>> depMap : Constants.dependencyMap.entrySet()) {
				String mergedGIDVersion = depMap.getKey();
				String[] brr = mergedGIDVersion.split("_");
				String depMapGroupID = brr[0];
				String depMapVersion = brr[1];
				
				if(depGroupId.equals(depMapGroupID))
				{
					depFound = true;
					POMDependencyDTO newDep = new POMDependencyDTO(depGroupId, depArtifactId, depMapVersion);
					List<POMDependencyDTO> depList = new ArrayList<>();
					if(Constants.finalDepMgmtChanges.containsKey(pomModule))
					{
						depList = Constants.finalDepMgmtChanges.get(pomModule);
					}
					depList.add(newDep);
					Constants.finalDepMgmtChanges.put(pomModule, depList);
					break;
				}
			}
			if(depFound == false)
			{
				POMDependencyDTO newDep = new POMDependencyDTO(depGroupId, depArtifactId, depversion);
				List<POMDependencyDTO> depList = new ArrayList<>();
				if(Constants.finalDepMgmtChanges.containsKey(pomModule))
				{
					depList = Constants.finalDepMgmtChanges.get(pomModule);
				}
				depList.add(newDep);
				Constants.finalDepMgmtChanges.put(pomModule, depList);
			}
		}
		
		List<String> modules = PomOperations.getPomModules(pomPath+Constants.pomFileName);
		
		for(String module : modules)
		{
			getDepMgmtRecommendations(pomPath+"\\"+module);
		}
	}
	
	public static Map<String, List<POMDependencyDTO>> getRecommendations(Map<String, List<String>> dependencyMap, Map<String, String> keyModule)
	{
		Map<String, List<POMDependencyDTO>> finalChanges = new HashMap<>();
		for (Map.Entry<String, String> keymod : Constants.keyModule.entrySet()) {
//		    System.out.println("Key: " + keymod.getKey() + ", Value: " + keymod.getValue());
		    
		    String key = keymod.getKey();
		    String module = keymod.getValue();
		    String[] dependencies = key.split(",");
		    for(String dependency : dependencies)
		    {
		    	String[] arr = dependency.split("#");
		    	String keyGroupId = arr[0];
		    	String keyArtifactId = arr[1];
		    	String keyVersion = arr[2];
		    	
				POMDependencyDTO newDep = new POMDependencyDTO(keyGroupId, keyArtifactId, keyVersion);
				List<POMDependencyDTO> depList = new ArrayList<>();
				if(finalChanges.containsKey(module))
				{
					depList = finalChanges.get(module);
				}
				depList.add(newDep);
				finalChanges.put(module, depList);
		    }
		}
		return finalChanges;
	}
	
	public static Map<String, List<POMDependencyDTO>> getGroupRecommendations(Map<String, List<String>> dependencyMap, Map<String, String> keyModule)
	{
		Map<String, List<POMDependencyDTO>> finalChanges = new HashMap<>();
		for (Map.Entry<String, String> keymod : Constants.keyModule.entrySet()) {
//		    System.out.println("Key: " + keymod.getKey() + ", Value: " + keymod.getValue());
		    
		    String key = keymod.getKey();
		    String module = keymod.getValue();
		    String[] dependencies = key.split(",");
		    for(String dependency : dependencies)
		    {
		    	String[] arr = dependency.split("#");
		    	String keyGroupId = arr[0];
		    	String keyArtifactId = arr[1];
		    	String keyVersion = arr[2];
		    	
		    	boolean depFound = false;
				for (Map.Entry<String, List<String>> depMap : Constants.dependencyMap.entrySet()) {
					String mergedGIDVersion = depMap.getKey();
					String[] brr = mergedGIDVersion.split("_");
					String depMapGroupID = brr[0];
					String depMapVersion = brr[1];
					
					if(keyGroupId.equals(depMapGroupID))
					{
						depFound = true;
						POMDependencyDTO newDep = new POMDependencyDTO(keyGroupId, keyArtifactId, depMapVersion);
						List<POMDependencyDTO> depList = new ArrayList<>();
						if(finalChanges.containsKey(module))
						{
							depList = finalChanges.get(module);
						}
						depList.add(newDep);
						finalChanges.put(module, depList);
					}
				}
				if(depFound == false)
				{
					POMDependencyDTO newDep = new POMDependencyDTO(keyGroupId, keyArtifactId, keyVersion);
					List<POMDependencyDTO> depList = new ArrayList<>();
					if(finalChanges.containsKey(module))
					{
						depList = finalChanges.get(module);
					}
					depList.add(newDep);
					finalChanges.put(module, depList);
				}
		    }
		}
		return finalChanges;
	}
	
	public static void upgradeParentToLatestMajorPatch(List<POMDependencyDTO> parentDependencies, String newPomFilePath, String patchRestriction)
	{
		System.out.println("Parent Upgrades - \n");
		for(POMDependencyDTO depMgmtParentDependency : parentDependencies)
		{
			List<VersionDTO> versionList = VersionFetcher.fetchAllVersions(depMgmtParentDependency.getGroupId(), depMgmtParentDependency.getArtifactId(), depMgmtParentDependency.getVersion(), patchRestriction);
			if(versionList.size()>0)
			{
				long maxTimestamp = 0L;
				String maxVersion = "";
				for(VersionDTO v : versionList)
				{
					if(v.getTimestamp()>maxTimestamp)
					{
						maxTimestamp = v.getTimestamp();
						maxVersion = v.getVersion();
					}
				}
				String updatedVerion = maxVersion;
				if(!updatedVerion.equalsIgnoreCase(depMgmtParentDependency.getVersion()))
				{
					PomOperations.updatePomProperties(newPomFilePath, depMgmtParentDependency.getGroupId(), depMgmtParentDependency.getArtifactId(), updatedVerion);

					PomOperations.updateParentDependencies(depMgmtParentDependency.getGroupId(), depMgmtParentDependency.getArtifactId(), updatedVerion);
					
					System.out.println(depMgmtParentDependency.getGroupId());
					System.out.println(depMgmtParentDependency.getArtifactId());
					System.out.println(updatedVerion);

				}
			}
		}
		System.out.println();
	}
}
