package pom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.ObjectMapper;

import common.Constants;
import common.dto.DependencyDTO;

public class DependencyOperations {

	//generate the dependency tree
	public static void generateDependencyTreeWithPath(String pomFileLocation, String pomLocation, String dependencyTreeLocation)
	{
		try {

			String[] dependencyTreeCommandWithPath = { Constants.mvnPath+"\\mvn.cmd", 
					"-f",
					pomFileLocation,
					"dependency:tree", 
					"-DoutputType=json", 
					"-DoutputFile="+dependencyTreeLocation };

			ProcessBuilder processBuilder = new ProcessBuilder(dependencyTreeCommandWithPath);
			processBuilder.directory(new java.io.File(pomLocation));
			Process process = processBuilder.start();

			// Read the output (optional, since output is redirected to a file)
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
//				System.out.println(line);
			}

			// Wait for the process to complete
			int exitCode = process.waitFor();
//			System.out.println("Exited with code: " + exitCode);

			// Verify the output file
			java.io.File outputFile = new java.io.File(dependencyTreeLocation);
			if (outputFile.exists()) {
//				System.out.println("Dependency tree successfully written to " + outputFile.getAbsolutePath());
			} else {
//				System.out.println("Failed to create dependency tree file.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static DependencyDTO getDependencyTree(String dependencyTreeLocation)
	{
		//		System.out.println("inside getDependencyTree");
		ObjectMapper objectMapper = new ObjectMapper();
		DependencyDTO rootDependency = new DependencyDTO();
		try {
			// Read JSON file and convert it to a List of User objects
			rootDependency = objectMapper.readValue(new File(dependencyTreeLocation), DependencyDTO.class);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return rootDependency;
	}

	public static void generateDependencyCheckReportWithPath(String pomFileLocation, String pomLocation, String dependencyCheckReportLocation)
	{
		//		System.out.println("inside generateDependencyCheckReportWithPath");
		try {

			String[] dependencyCheckCommand = { Constants.mvnPath+"\\mvn.cmd", 
					"-f",
					pomFileLocation,
					"dependency-check:check" };
			
//			System.out.println(dependencyCheckCommand);

			// Start the process
			ProcessBuilder processBuilder = new ProcessBuilder(dependencyCheckCommand);
			processBuilder.directory(new java.io.File(pomLocation));
			Process process = processBuilder.start();

			// Read the output (optional, since output is redirected to a file)
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
//				System.out.println(line);
			}

			// Wait for the process to complete
			int exitCode = process.waitFor();
//			System.out.println("Exited with code: " + exitCode);

			// Verify the output file
			java.io.File outputFile = new java.io.File(dependencyCheckReportLocation);
			if (outputFile.exists()) {
				//				System.out.println("Dependency tree successfully written to " + outputFile.getAbsolutePath());
			} else {
				System.out.println("Failed to create dependency tree file.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addDependency(String pomFilePath, String groupId, String artifactId, String version){

		try {
			File pomFile = new File(pomFilePath);

			// Parse the existing pom.xml
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(pomFile);
			doc.getDocumentElement().normalize();

			// Check if the dependency already exists with the same groupId and artifactId
			NodeList dependenciesList = doc.getElementsByTagName("dependency");
			boolean dependencyExists = false;

			for (int i = 0; i < dependenciesList.getLength(); i++) {
				Element dependency = (Element) dependenciesList.item(i);

				String currentGroupId = dependency.getElementsByTagName("groupId").item(0).getTextContent();
				String currentArtifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();

				// If the groupId and artifactId match, check version
				if (currentGroupId.equals(groupId) && currentArtifactId.equals(artifactId)) {
					String currentVersion = dependency.getElementsByTagName("version").item(0).getTextContent();

					if (!currentVersion.equals(version)) {
						// If the version is different, update the version
						dependency.getElementsByTagName("version").item(0).setTextContent(version);
//						System.out.println("Updated dependency version from " + currentVersion + " to " + version);
					} else {
//						System.out.println("Dependency already exists with the same version.");
					}

					// Dependency with same groupId and artifactId found, no need to add a duplicate
					dependencyExists = true;
					break;
				}
			}

			// If no matching dependency is found, add a new one
			if (!dependencyExists) {
				// Find or create the <dependencies> element
				NodeList dependenciesListRoot = doc.getElementsByTagName("dependencies");
				Element dependencies;
				if (dependenciesListRoot.getLength() > 0) {
					dependencies = (Element) dependenciesListRoot.item(0);
				} else {
					// Create <dependencies> block if it doesn't exist
					dependencies = doc.createElement("dependencies");
					doc.getDocumentElement().appendChild(dependencies);
				}

				// Create the new dependency element
				Element newDependency = doc.createElement("dependency");

				Element groupIdElem = doc.createElement("groupId");
				groupIdElem.appendChild(doc.createTextNode(groupId));
				newDependency.appendChild(groupIdElem);

				Element artifactIdElem = doc.createElement("artifactId");
				artifactIdElem.appendChild(doc.createTextNode(artifactId));
				newDependency.appendChild(artifactIdElem);

				Element versionElem = doc.createElement("version");
				versionElem.appendChild(doc.createTextNode(version));
				newDependency.appendChild(versionElem);

				// Add the new dependency
				dependencies.appendChild(newDependency);
//				System.out.println("Dependency added successfully to pom.xml.");
			}
			else
			{
				updateDependency(pomFilePath, groupId, artifactId, version);
			}

			removeWhitespaceNodes(doc.getDocumentElement());

			// Save the updated document back to the pom.xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(pomFile);
			transformer.transform(source, result);
		}catch (Exception e) {
			System.out.println(e);
		}
	}

	public static void updateDependency(String pomFileLocation, String groupId, String artifactId, String version) {
		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new File(pomFileLocation));

			// Normalize the XML structure
			doc.getDocumentElement().normalize();

			Node dependency = doc.getElementsByTagName("dependency").item(0);
			if (dependency.getNodeType() == Node.ELEMENT_NODE) {
				Element dependencyElement = (Element) dependency;

				Node groupIdNode = dependencyElement.getElementsByTagName("groupId").item(0);
				Node artifactIdNode = dependencyElement.getElementsByTagName("artifactId").item(0);
				Node versionNode = dependencyElement.getElementsByTagName("version").item(0);

				groupIdNode.setTextContent(groupId);
				artifactIdNode.setTextContent(artifactId);
				versionNode.setTextContent(version);
			}

			// Write the updated document back to the file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(pomFileLocation));
			//            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(source, result);
//			System.out.println("POM file updated successfully to - "+groupId+" - "+artifactId+" - "+version);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateDependencyVersion(String pomFileLocation, String groupId, String artifactId, String newVersion) {
		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new File(pomFileLocation));

			// Normalize the XML structure
			doc.getDocumentElement().normalize();

			// Get the list of dependencies
			NodeList dependencies = doc.getElementsByTagName("dependency");

			// Iterate through the dependencies to find the correct one
			for (int i = 0; i < dependencies.getLength(); i++) {
				Node dependency = dependencies.item(i);

				if (dependency.getNodeType() == Node.ELEMENT_NODE) {
					Element dependencyElement = (Element) dependency;

					String currentGroupId = dependencyElement.getElementsByTagName("groupId").item(0).getTextContent();
					String currentArtifactId = dependencyElement.getElementsByTagName("artifactId").item(0).getTextContent();

					if (currentGroupId.equals(groupId) && currentArtifactId.equals(artifactId)) {
						// Found the dependency, now update the version
						Node versionNode = dependencyElement.getElementsByTagName("version").item(0);
						if (versionNode != null) {
							versionNode.setTextContent(newVersion);
//                            System.out.println("Updated version to " + newVersion);
						} else {
							// If no version tag exists, add it
							Element versionElement = doc.createElement("version");
							versionElement.appendChild(doc.createTextNode(newVersion));
							dependencyElement.appendChild(versionElement);
//                            System.out.println("Added version " + newVersion);
						}
						break; // Dependency found and updated; exit loop
					}
				}
			}

			// Write the updated document back to the file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(pomFileLocation));
			//            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(source, result);

//			System.out.println("POM file updated successfully to version - "+newVersion);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void clearAllDependencies(String pomFilePath){
		try
		{
			FileReader reader = new FileReader(pomFilePath);

			MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
			Model model = xpp3Reader.read(reader);
			
			DependencyManagement depMgmt = model.getDependencyManagement();
			if(depMgmt!=null)
			{
				depMgmt.getDependencies().clear();
			}
			model.getDependencies().clear();			
			
            try (FileWriter writer = new FileWriter(pomFilePath)) {
                MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
                mavenWriter.write(writer, model);
            }
		}catch (Exception e) {
			System.out.println(e);
		}
	}

	public static void removeElementsByTagName(Document document, String tagName) {
		NodeList elements = document.getElementsByTagName(tagName);
		for (int i = elements.getLength() - 1; i >= 0; i--) {
			Node node = elements.item(i);
			node.getParentNode().removeChild(node);
		}
	}
	
	public static void removeWhitespaceNodes(Node node) {
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
    public static Node getOrCreateBuildSection(Document doc) {
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
    public static Node createDependencyPlugin(Document doc, String newPomFileDirectory) {
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
    public static Node createOwaspPlugin(Document doc, String newPomFileDirectory) {
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
    public static Node createVersionsPlugin(Document doc) {
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
    public static void addOrUpdatePlugin(Document doc, Node buildNode, Node newPlugin) {
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
    public static Node getOrCreatePluginsNode(Document doc, Node buildNode) {
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
    public static Node getFirstElementByTagName(Node node, String tagName) {
        NodeList list = ((Element) node).getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0);
        }
        return null;
    }
}