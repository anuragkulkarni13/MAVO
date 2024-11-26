package pom;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import common.Constants;
import common.dto.POMDependencyDTO;

public class PomCreations {

	public static void createRecommendedPOM(String pomPath, Map<String, List<POMDependencyDTO>> finalChanges)
	{
		System.out.println("RecommendedPom.xml Module - "+pomPath);
		createModuleRecommendedPOM(pomPath, finalChanges);
		
		List<String> modules = PomOperations.getPomModules(pomPath+Constants.pomFileName);
		
		for(String module : modules)
		{
			createRecommendedPOM(pomPath+"\\"+module, finalChanges);
		}
	}
	
	public static void createModuleRecommendedPOM(String pomPath, Map<String, List<POMDependencyDTO>> finalChanges)
	{
		File originalPom = new File(pomPath+Constants.pomFileName);
		File temporaryPom = new File(pomPath+Constants.recommendedPomFileName);
		
		try {
			Files.copy(originalPom.toPath(), temporaryPom.toPath(), StandardCopyOption.REPLACE_EXISTING);
			System.out.println("\nFile cloned and renamed to temporaryPom.xml successfully in location ."+pomPath+Constants.recommendedPomFileName);

			PomOperations.clearAllDependencies(pomPath+Constants.recommendedPomFileName);

			
			List<Dependency> originalDepMgmtDependencies = new ArrayList<>();
			List<Dependency> originalNormalDependencies = new ArrayList<>();
			
			FileReader reader = new FileReader(pomPath+Constants.pomFileName);

			MavenXpp3Reader mavenReader = new MavenXpp3Reader();
			Model model = mavenReader.read(reader);
			
			DependencyManagement depManagement = model.getDependencyManagement();
			if (depManagement != null) {
				originalDepMgmtDependencies = depManagement.getDependencies();
			}
			
			originalNormalDependencies = model.getDependencies();
			
			
			List<POMDependencyDTO> depMgmtRecommendations = new ArrayList<>();
			List<POMDependencyDTO> depRecommendations = new ArrayList<>();

			String[] arr = pomPath.split("\\\\");
			String pomModule = arr[arr.length-1];
			
			if(Constants.finalDepMgmtChanges.containsKey(pomModule))
			{
				depMgmtRecommendations = Constants.finalDepMgmtChanges.get(pomModule);
			}

			if(finalChanges.containsKey(pomModule))
			{
				depRecommendations = finalChanges.get(pomModule);
			}
			
			ObjectMapper objectMapper = new ObjectMapper();
			List<Dependency> dependencyManagementRecommendationDeps = objectMapper.convertValue(
					depMgmtRecommendations,
					new TypeReference<List<Dependency>>() {}
					);

			List<Dependency> normalRecommendationDeps = objectMapper.convertValue(
					depRecommendations,
					new TypeReference<List<Dependency>>() {}
					);
			
			for(Dependency recDep : dependencyManagementRecommendationDeps)
			{
				boolean depFound = false;
				for(Dependency orgDep : originalDepMgmtDependencies)
				{
					if(orgDep.getGroupId().equals(recDep.getGroupId()) && orgDep.getArtifactId().equals(recDep.getArtifactId()))
					{
						String depversion = orgDep.getVersion();
						if(depversion != null)
						{
							if (depversion.startsWith("${") && depversion.endsWith("}")) {
				                String propertyName = depversion.substring(2, depversion.length() - 1);
				                Constants.globalpropertiesMap.put(propertyName, recDep.getVersion().toString());
							}
						}
						orgDep.setVersion(recDep.getVersion());
						depFound = true;
					}
				}
				if(depFound == false)
				{
					originalDepMgmtDependencies.add(recDep);
				}
			}
			
			System.out.println("\nDep Mgmt Recommendations in recommendedPom.xml");
			for(Dependency d : originalDepMgmtDependencies)
			{
				System.out.println(d);
			}
			System.out.println();
			
			
			for(Dependency recDep : normalRecommendationDeps)
			{
				boolean depFound = false;
				for(Dependency orgDep : originalNormalDependencies)
				{
					if(orgDep.getGroupId().equals(recDep.getGroupId()) && orgDep.getArtifactId().equals(recDep.getArtifactId()))
					{
						String depversion = orgDep.getVersion();
						if(depversion != null)
						{
							if (depversion.startsWith("${") && depversion.endsWith("}")) {
				                String propertyName = depversion.substring(2, depversion.length() - 1);
				                Constants.globalpropertiesMap.put(propertyName, recDep.getVersion().toString());
							}
						}
						orgDep.setVersion(recDep.getVersion());
						depFound = true;
					}
				}
				if(depFound == false)
				{
					originalNormalDependencies.add(recDep);
				}
			}
			
			System.out.println("\nDep Recommendations in recommendedPom.xml");
			for(Dependency d : originalNormalDependencies)
			{
				System.out.println(d);
			}
			System.out.println();
			
			
			FileReader reader1 = new FileReader(pomPath+Constants.recommendedPomFileName);

			MavenXpp3Reader mavenReader1 = new MavenXpp3Reader();
			Model model1 = mavenReader1.read(reader1);
			
			
			// Add dependency management dependencies
			if (!originalDepMgmtDependencies.isEmpty()) {
				DependencyManagement depManagement1 = model1.getDependencyManagement();
				//          System.out.println(depManagement);
				if (depManagement1 == null) {
					depManagement1 = new DependencyManagement();
					model1.setDependencyManagement(depManagement1);
				}
				depManagement1.getDependencies().addAll(originalDepMgmtDependencies);
			}

			model1.getDependencies().addAll(originalNormalDependencies);
			
			
//			for (Map.Entry<String, String> entry : Constants.globalpropertiesMap.entrySet()) {
//				System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
//			}
			
			Properties prop = model1.getProperties();
			Set<Object> keys = prop.keySet();
			for(Object key : keys)
			{
				if(Constants.globalpropertiesMap.containsKey(key.toString()))
				{
					prop.setProperty(key.toString(), Constants.globalpropertiesMap.get(key.toString()));
				}
			}

//			System.out.println(prop);
			
			model1.setProperties(prop);

			// Write changes back to POM
			try (FileWriter writer = new FileWriter(pomPath+Constants.recommendedPomFileName)) {
				MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
				mavenWriter.write(writer, model1);
				System.out.println("\nDependencies and properties added successfully in recommededPom.xml.\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	public static void createModuleRecommendedPOM(String pomPath, Map<String, List<POMDependencyDTO>> finalChanges)
//	{
//        File originalPom = new File(pomPath+Constants.pomFileName);
//        File temporaryPom = new File(pomPath+Constants.recommendedPomFileName);
//        
//        try {
//            Files.copy(originalPom.toPath(), temporaryPom.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            System.out.println("File cloned and renamed to temporaryPom.xml successfully.");
//            
//            PomOperations.clearAllDependencies(pomPath+Constants.recommendedPomFileName);
//            
//            FileReader reader = new FileReader(pomPath+Constants.recommendedPomFileName);
//            
//            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
//            Model model = mavenReader.read(reader);
//            
//    		String[] arr = pomPath.split("\\\\");
//    		String pomModule = arr[arr.length-1];
//    		
//    		List<POMDependencyDTO> depMgmtRecommendations = new ArrayList<>();
//    		List<POMDependencyDTO> depRecommendations = new ArrayList<>();
//    				
//    		if(Constants.finalDepMgmtChanges.containsKey(pomModule))
//    		{
//    			depMgmtRecommendations = Constants.finalDepMgmtChanges.get(pomModule);
//    		}
//    		
//    		if(finalChanges.containsKey(pomModule))
//    		{
//    			depRecommendations = finalChanges.get(pomModule);
//    		}
//            
//            ObjectMapper objectMapper = new ObjectMapper();
//            List<Dependency> dependencyManagementDeps = objectMapper.convertValue(
//            		depMgmtRecommendations,
//                    new TypeReference<List<Dependency>>() {}
//            );
//            
//            List<Dependency> normalDeps = objectMapper.convertValue(
//            		depRecommendations,
//                    new TypeReference<List<Dependency>>() {}
//            );
//            // Add dependency management dependencies
//            if (!dependencyManagementDeps.isEmpty()) {
//                DependencyManagement depManagement = model.getDependencyManagement();
////                System.out.println(depManagement);
//                if (depManagement == null) {
//                    depManagement = new DependencyManagement();
//                    model.setDependencyManagement(depManagement);
//                }
//                depManagement.getDependencies().addAll(dependencyManagementDeps);
//            }
//            
//            model.getDependencies().addAll(normalDeps);
//
//            Properties prop = model.getProperties();
//            Set<Object> keys = prop.keySet();
//			for(Object key : keys)
//			{
////				System.out.println("key - "+key.toString());
////				System.out.println("global props - "+Constants.globalpropertiesMap.get(key.toString()));
//				if(Constants.globalpropertiesMap.containsKey(key.toString()))
//				{
//					prop.setProperty(key.toString(), Constants.globalpropertiesMap.get(key.toString()));
//				}
//			}
//			
//			model.setProperties(prop);
//            
//            
//            // Write changes back to POM
//            try (FileWriter writer = new FileWriter(pomPath+Constants.recommendedPomFileName)) {
//                MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
//                mavenWriter.write(writer, model);
//                System.out.println("Dependencies and properties added successfully.");
//            }
//            
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//	}
	
	public static void createAndSetupTempPOM(String pomPath)
	{
//		System.out.println("******* inside createAndSetupTempPOM");
//		String originalPomPath = Constants.originalPomFileLocation; // Replace with the actual path
//		String newPomFileDirectory = Constants.newTempPomLocation;
//		String newPomPath = Constants.newTempPomFileLocation;

		
		String originalPomFilePath = pomPath+Constants.pomFileName;
		String newPomFileDirectory = pomPath+Constants.tempPomFileDirectoryName;
		String newPomFilePath = newPomFileDirectory+Constants.tempPomFileName;
		
		try {

	        Path directoryPath = Paths.get(newPomFileDirectory);

	        // Check if the directory exists
	        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
//	            System.out.println("Directory does not exist: " + newPomFileDirectory);
				Files.createDirectories(Paths.get(newPomFileDirectory));
	        } else {
//	            System.out.println("Directory exists: " + newPomFileDirectory);
	        }
	        
            // Check if the file exists	        
	        Path filePath = Paths.get(newPomFilePath);

	        if (Files.exists(filePath)) {
//	            System.out.println("File already exists: " + filePath + ". Replacing it.");
	            Files.delete(filePath); // Delete the existing file
	        }

	        // Now create the new file
        	Files.copy(Paths.get(originalPomFilePath), Paths.get(newPomFilePath));
//	        System.out.println("New file created: " + filePath);
			
			// Step 2: Parse the new POM file=
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new File(newPomFilePath));

			// Step 3: Remove all dependencies
//			removeElementsByTagName(document, "dependencies");
			removeElementsByTagName(document, "parent");
//			removeElementsByTagName(document, "plugins");
			removeElementsByTagName(document, "modules");
			
			document.getDocumentElement().normalize();

            // Ensure the build section exists
            Node buildNode = getOrCreateBuildSection(document);

            // Add or update each plugin
            addOrUpdatePlugin(document, buildNode, createDependencyPlugin(document, newPomFileDirectory));
            addOrUpdatePlugin(document, buildNode, createOwaspPlugin(document, newPomFileDirectory));
            addOrUpdatePlugin(document, buildNode, createVersionsPlugin(document));
			
            removeWhitespaceNodes(document.getDocumentElement());

			// Step 5: Save the changes to the new POM file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(new File(newPomFilePath));
			transformer.transform(source, result);

//			System.out.println("POM file modified and saved successfully.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void removeElementsByTagName(Document document, String tagName) {
		NodeList elements = document.getElementsByTagName(tagName);
		for (int i = elements.getLength() - 1; i >= 0; i--) {
			Node node = elements.item(i);
			node.getParentNode().removeChild(node);
		}
	}
	
	protected static void removeWhitespaceNodes(Node node) {
	    NodeList childNodes = node.getChildNodes();
	    for (int i = 0; i < childNodes.getLength(); i++) {
	        Node child = childNodes.item(i);
	        if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().trim().isEmpty()) {
	            node.removeChild(child);
	            i--; // Adjust the index after removing a child node
	        } else if (child.hasChildNodes()) {
	            removeWhitespaceNodes(child); // Recursively process child nodes
	        }
	    }
	}
	
    // Helper method to check if a section exists and create it if it doesn't
    private static Node getOrCreateBuildSection(Document doc) {
        NodeList buildNodes = doc.getElementsByTagName("build");
        Node buildNode;
        if (buildNodes.getLength() == 0) {
            buildNode = doc.createElement("build");
            doc.getDocumentElement().appendChild(buildNode);
        } else {
            buildNode = buildNodes.item(0);
        }
        return buildNode;
    }
    
    // Helper method to create the maven-dependency-plugin
    private static Node createDependencyPlugin(Document doc, String newPomFileDirectory) {
        Element plugin = doc.createElement("plugin");

        Element groupId = doc.createElement("groupId");
        groupId.appendChild(doc.createTextNode("org.apache.maven.plugins"));
        plugin.appendChild(groupId);

        Element artifactId = doc.createElement("artifactId");
        artifactId.appendChild(doc.createTextNode("maven-dependency-plugin"));
        plugin.appendChild(artifactId);

        Element version = doc.createElement("version");
        version.appendChild(doc.createTextNode(Constants.mavenDependencyPluginVersion));
        plugin.appendChild(version);

        Element executions = doc.createElement("executions");
        Element execution = doc.createElement("execution");
        Element id = doc.createElement("id");
        id.appendChild(doc.createTextNode("write-dependency-tree"));
        execution.appendChild(id);

        Element phase = doc.createElement("phase");
        phase.appendChild(doc.createTextNode("verify"));
        execution.appendChild(phase);

        Element goals = doc.createElement("goals");
        Element goal = doc.createElement("goal");
        goal.appendChild(doc.createTextNode("tree"));
        goals.appendChild(goal);
        execution.appendChild(goals);

        Element configuration = doc.createElement("configuration");
        Element outputFile = doc.createElement("outputFile");
        outputFile.appendChild(doc.createTextNode(newPomFileDirectory+"\\dependency-tree.json"));
        configuration.appendChild(outputFile);

        Element outputType = doc.createElement("outputType");
        outputType.appendChild(doc.createTextNode("json"));
        configuration.appendChild(outputType);

        execution.appendChild(configuration);
        executions.appendChild(execution);
        plugin.appendChild(executions);

        return plugin;
    }

    // Helper method to create the dependency-check-maven plugin
    private static Node createOwaspPlugin(Document doc, String newPomFileDirectory) {
        Element plugin = doc.createElement("plugin");

        Element groupId = doc.createElement("groupId");
        groupId.appendChild(doc.createTextNode("org.owasp"));
        plugin.appendChild(groupId);

        Element artifactId = doc.createElement("artifactId");
        artifactId.appendChild(doc.createTextNode("dependency-check-maven"));
        plugin.appendChild(artifactId);

        Element version = doc.createElement("version");
        version.appendChild(doc.createTextNode(Constants.dependencyCheckMavenVersion));
        plugin.appendChild(version);

        Element executions = doc.createElement("executions");
        Element execution = doc.createElement("execution");
        Element goals = doc.createElement("goals");
        Element goal = doc.createElement("goal");
        goal.appendChild(doc.createTextNode("check"));
        goals.appendChild(goal);
        execution.appendChild(goals);

        Element configuration = doc.createElement("configuration");

        Element formats = doc.createElement("formats");
        Element format = doc.createElement("format");
        format.appendChild(doc.createTextNode("JSON"));
        formats.appendChild(format);
        configuration.appendChild(formats);

        Element outputDirectory = doc.createElement("outputDirectory");
        outputDirectory.appendChild(doc.createTextNode(newPomFileDirectory));
        configuration.appendChild(outputDirectory);

        Element assemblyAnalyzerEnabled = doc.createElement("assemblyAnalyzerEnabled");
        assemblyAnalyzerEnabled.appendChild(doc.createTextNode("false"));
        configuration.appendChild(assemblyAnalyzerEnabled);

        Element golangDepEnabled = doc.createElement("golangDepEnabled");
        golangDepEnabled.appendChild(doc.createTextNode("false"));
        configuration.appendChild(golangDepEnabled);

        Element nuspecAnalyzerEnabled = doc.createElement("nuspecAnalyzerEnabled");
        nuspecAnalyzerEnabled.appendChild(doc.createTextNode("false"));
        configuration.appendChild(nuspecAnalyzerEnabled);

        executions.appendChild(execution);
        plugin.appendChild(executions);
        plugin.appendChild(configuration);


        return plugin;
    }

    // Helper method to create the versions-maven-plugin
    private static Node createVersionsPlugin(Document doc) {
        Element plugin = doc.createElement("plugin");

        Element groupId = doc.createElement("groupId");
        groupId.appendChild(doc.createTextNode("org.codehaus.mojo"));
        plugin.appendChild(groupId);

        Element artifactId = doc.createElement("artifactId");
        artifactId.appendChild(doc.createTextNode("versions-maven-plugin"));
        plugin.appendChild(artifactId);

        Element version = doc.createElement("version");
        version.appendChild(doc.createTextNode(Constants.versionsMavenPluginVersion));
        plugin.appendChild(version);

        return plugin;
    }

    // Helper method to add or update a plugin in the POM
    private static void addOrUpdatePlugin(Document doc, Node buildNode, Node newPlugin) {
        Node pluginsNode = getOrCreatePluginsNode(doc, buildNode);

        // Check if the plugin exists, update it if found, else add it
        NodeList pluginList = pluginsNode.getChildNodes();
        boolean pluginExists = false;
        for (int i = 0; i < pluginList.getLength(); i++) {
            Node existingPlugin = pluginList.item(i);
            if (existingPlugin.getNodeType() == Node.ELEMENT_NODE) {
                Node groupIdNode = getFirstElementByTagName(existingPlugin, "groupId");
                Node artifactIdNode = getFirstElementByTagName(existingPlugin, "artifactId");

                // Compare groupId and artifactId to see if the plugin already exists
                if (groupIdNode != null && artifactIdNode != null &&
                    groupIdNode.getTextContent().equals(getFirstElementByTagName(newPlugin, "groupId").getTextContent()) &&
                    artifactIdNode.getTextContent().equals(getFirstElementByTagName(newPlugin, "artifactId").getTextContent())) {
                    // Plugin exists, replace it
                    pluginsNode.replaceChild(newPlugin, existingPlugin);
                    pluginExists = true;
                    break;
                }
            }
        }
        if (!pluginExists) {
            // Plugin doesn't exist, add it
            pluginsNode.appendChild(newPlugin);
        }
    }

    // Helper method to ensure the plugins section exists in the build section
    private static Node getOrCreatePluginsNode(Document doc, Node buildNode) {
        NodeList pluginsNodes = ((Element) buildNode).getElementsByTagName("plugins");
        Node pluginsNode;
        if (pluginsNodes.getLength() == 0) {
            pluginsNode = doc.createElement("plugins");
            buildNode.appendChild(pluginsNode);
        } else {
            pluginsNode = pluginsNodes.item(0);
        }
        return pluginsNode;
    }

    // Helper method to get the first element by tag name
    private static Node getFirstElementByTagName(Node node, String tagName) {
        NodeList list = ((Element) node).getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0);
        }
        return null;
    }
}
