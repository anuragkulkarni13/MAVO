package algorithm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.Dependency;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import common.Constants;
import common.Utils;
import common.dto.DependencyDTO;
import common.dto.VersionDTO;
import common.dto.VulnerabilityDTO;
import db.DependencyCache;
import db.DependencyDB;
import pom.DependencyOperations;
import pom.PomOperations;
import versionmanagement.VersionFetcher;
import visualization.graphVisual;
import vulnerability.VulnerabilityAnalyzer;

public class POMAnalyzerAlgorithm {

	public static int createTableInstances(String originalPomPath) {
        int dependencyTableStatus = DependencyDB.createDependencyTable(originalPomPath);
        int versionTableStatus = DependencyDB.createVersionTable(originalPomPath);
        
        if(dependencyTableStatus == 0 && versionTableStatus == 0)
        {
        	return 0;
        }
        return 1;
	}
	
	public static int createCacheInstances(String originalPomPath) {
        int vulnerabilityCacheStatus = DependencyCache.createVulnerabilityCache(originalPomPath);
        int combinationCacheStatus = DependencyCache.createCombinationCache(originalPomPath);
        
        if(vulnerabilityCacheStatus== 0 && combinationCacheStatus == 0)
        {
        	return 0;
        }
        return 1;
	}
	
	public static void parsePOMModules(String originalPomPath, String pomPath) {
		
		System.out.println("parse Module - "+pomPath);
		
		String pomPathFile = pomPath+"\\"+Constants.pomFileName;
		
		String[] pomPathSplit = pomPath.split("\\\\");
		String moduleName = pomPathSplit[pomPathSplit.length-1];
		
	    try {
			String DB_URL = Utils.get_DB_URL(originalPomPath);
			Connection connection = DriverManager.getConnection(DB_URL);
			
			Map<String, String> propertiesMap = PomOperations.getPomProperties(pomPathFile);
			DependencyDB.putPOMPropertiesInVersionTable(connection, originalPomPath, propertiesMap);

			Map<String, List<Dependency>> segregatedDependencies = PomOperations.getSegregatedPomDependencies(pomPathFile);
			DependencyDB.putDependenciesInDependencyTable(connection, originalPomPath, segregatedDependencies, moduleName);
			
			connection.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

		
		
		List<String> modules = PomOperations.getPomModules(pomPathFile);
		for(String module : modules)
		{
			parsePOMModules(originalPomPath, pomPath+"\\"+module);
		}
	}
	
	public static void updateExcludeList(String pomPath) {
		
		String excludeListFile = pomPath+"\\"+Constants.excludeListFileName;
		
	    try {
			String DB_URL = Utils.get_DB_URL(pomPath);
			Connection connection = DriverManager.getConnection(DB_URL);
	        File xmlFile = new File(excludeListFile);
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
	                DependencyDB.updateExcludeListInDependencyTable(connection, groupId, artifactId);
	            }
	        }
	        connection.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	public static void updateParentToLatestPatch(String pomPath, String patchRestriction) {
		System.out.println("start parent upgrade");
	    try {
	    	
	    	String newRecommendedVersion = "";
	    	
			String DB_URL = Utils.get_DB_URL(pomPath);
			Connection connection = DriverManager.getConnection(DB_URL);
			
			List<DependencyDTO> parentDependencies = DependencyDB.getAllParentDependencies(connection);
			
			for(DependencyDTO parentDependency : parentDependencies)
			{
            	newRecommendedVersion = parentDependency.getVersion();
    			List<VersionDTO> versionList = VersionFetcher.fetchAllVersions(parentDependency.getGroupId(), parentDependency.getArtifactId(), parentDependency.getVersion(), 
    					patchRestriction);
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
    				newRecommendedVersion = maxVersion;
    			}
    			DependencyDB.updateRecommendedVersionForParentInDependencyTable(connection, parentDependency.getGroupId(), parentDependency.getArtifactId(), parentDependency.getVersion(), newRecommendedVersion);
			
    			DependencyDB.updateParentDependentDependencies(connection, parentDependency.getGroupId(), newRecommendedVersion);
			}
			
	        connection.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    System.out.println("end parent upgrade");
	}

	public static void createAndSetupTempPOM(String pomPath)
	{
//		System.out.println("******* inside createAndSetupTempPOM");
//		String originalPomPath = Constants.originalPomFileLocation; // Replace with the actual path
//		String newPomFileDirectory = Constants.newTempPomLocation;
//		String newPomPath = Constants.newTempPomFileLocation;

		
		String originalPomFilePath = pomPath+"\\"+Constants.pomFileName;
		String newPomFileDirectory = pomPath+"\\"+Constants.tempPomFileDirectoryName;
		String newPomFilePath = newPomFileDirectory+"\\"+Constants.tempPomFileName;
		
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
			DependencyOperations.removeElementsByTagName(document, "dependencies");
			DependencyOperations.removeElementsByTagName(document, "parent");
//			removeElementsByTagName(document, "plugins");
			DependencyOperations.removeElementsByTagName(document, "modules");
			
			document.getDocumentElement().normalize();

            // Ensure the build section exists
            Node buildNode = DependencyOperations.getOrCreateBuildSection(document);

            // Add or update each plugin
            DependencyOperations.addOrUpdatePlugin(document, buildNode, DependencyOperations.createDependencyPlugin(document, newPomFileDirectory));
            DependencyOperations.addOrUpdatePlugin(document, buildNode, DependencyOperations.createOwaspPlugin(document, newPomFileDirectory));
            DependencyOperations.addOrUpdatePlugin(document, buildNode, DependencyOperations.createVersionsPlugin(document));
			
            DependencyOperations.removeWhitespaceNodes(document.getDocumentElement());

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
	
	public static void getRecommendationsforDependencies(String originalPomPath, String pomPath) {
		
		System.out.println("Recommendation Module - "+pomPath);
		
		String pomPathFile = pomPath+"\\"+Constants.pomFileName;
		
		String[] pomPathSplit = pomPath.split("\\\\");
		String moduleName = pomPathSplit[pomPathSplit.length-1];
		
	    try {
			String DB_URL = Utils.get_DB_URL(originalPomPath);
			Connection connection = DriverManager.getConnection(DB_URL);
			
			List<DependencyDTO> dependencyList = DependencyDB.getModulewiseDependenciesExceptParent(connection, moduleName);
			
			List<DependencyDTO> pomRecommendations = new ArrayList<>();

			for(DependencyDTO dependency : dependencyList)
			{
				System.out.println("Recommendations to be generated for dependency - "+dependency.getGroupId()+" - "+dependency.getArtifactId()+" - "+dependency.getVersion());
				
				List<DependencyDTO> pomDependencyRecommendations = getRecommendationsforTransitiveDependencies(originalPomPath, dependency, moduleName);
				
				
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
			for(DependencyDTO recommendation : pomRecommendations)
			{
				System.out.println(recommendation.getGroupId());
				System.out.println(recommendation.getArtifactId());
				System.out.println(recommendation.getVersion());
				System.out.println();
				
//				DependencyDB.addOrUpdateDependencyTable(connection, recommendation.getGroupId(), recommendation.getArtifactId(), recommendation.getVersion(), moduleName);
			}
			
			
			
			connection.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

		
		
		List<String> modules = PomOperations.getPomModules(pomPathFile);
		for(String module : modules)
		{
			getRecommendationsforDependencies(originalPomPath, pomPath+"\\"+module);
		}
	}
	
	public static List<DependencyDTO> getRecommendationsforTransitiveDependencies(String originalPomPath, DependencyDTO dependency, String moduleName) {
		
		System.out.println("Starting Recommendations to be generated for dependency - "+dependency.getGroupId()+" - "+dependency.getArtifactId()+" - "+dependency.getVersion());

		String temppomDirectory = originalPomPath+"\\"+Constants.tempPomFileDirectoryName;
		String tempPomDependencyTreePath = temppomDirectory+"\\"+Constants.dependencyTreeName;
		String dependencyCheckReportLocation = temppomDirectory+"\\"+Constants.dependencyCheckReportName;
		String tempPomFileLocation = temppomDirectory+"\\"+Constants.tempPomFileName;
		
		List<DependencyDTO> pomRecommendations = new ArrayList<DependencyDTO>();

		
		try {

			String DB_URL = Utils.get_DB_URL(originalPomPath);

			Connection connection = DriverManager.getConnection(DB_URL);

			DependencyOperations.clearAllDependencies(tempPomFileLocation);

			DependencyOperations.addDependency(tempPomFileLocation, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());

			DependencyOperations.generateDependencyTreeWithPath(tempPomFileLocation, temppomDirectory, tempPomDependencyTreePath);
			
			DependencyDTO root = DependencyOperations.getDependencyTree(tempPomDependencyTreePath);
			
			for(DependencyDTO child : root.getChildren())
			{
				System.out.println("Root dependency - "+dependency.getGroupId()+" - "+dependency.getArtifactId()+" - "+dependency.getVersion());

				List<String> depList = new ArrayList<>();

				Queue<DependencyDTO> depQueue = new LinkedList<>();
				depList.add(child.getArtifactId());
				depQueue.offer(child);
				while(!depQueue.isEmpty())
				{
					DependencyDTO node = depQueue.poll();
					System.out.println("node for recommendation scanning "+node.getArtifactId());
					DependencyOperations.updateDependency(tempPomFileLocation, node.getGroupId(), node.getArtifactId(), node.getVersion());
					
					String leastVulCountVersion = node.getVersion();
					
						System.out.println("Finding Least vulnerable version for dependency - "+node.getGroupId()+" - "+node.getArtifactId()+" - "+node.getVersion());

						leastVulCountVersion = VulnerabilityAnalyzer.getLeastVulnerableVersion(connection, originalPomPath, node.getGroupId(), node.getArtifactId(), node.getVersion(), tempPomFileLocation, temppomDirectory, tempPomDependencyTreePath, dependencyCheckReportLocation, moduleName);
						if(!leastVulCountVersion.equals(node.getVersion()))
						{
							System.out.println("############################# add dep recommendation ############################## "+node.getArtifactId()+" - "+node.getGroupId()+" - "+leastVulCountVersion);
							DependencyDB.updateRecommendedVersionToDependencyTable(connection, node.getGroupId(), node.getArtifactId(), leastVulCountVersion);
							node.setVersion(leastVulCountVersion);
							pomRecommendations.add(node);
						}
					
					DependencyOperations.updateDependencyVersion(tempPomFileLocation, node.getGroupId(),node.getArtifactId(),leastVulCountVersion);
					DependencyOperations.generateDependencyTreeWithPath(tempPomFileLocation, temppomDirectory, tempPomDependencyTreePath);

					node = DependencyOperations.getDependencyTree(tempPomDependencyTreePath);
					for(DependencyDTO nodeChild : node.getChildren().get(0).getChildren())
					{
						if(!depList.contains(nodeChild.getArtifactId()))
						{
							depList.add(nodeChild.getArtifactId());
							depQueue.offer(nodeChild);
							System.out.println("child dependency - "+nodeChild.getGroupId()+" - "+nodeChild.getArtifactId()+" - "+nodeChild.getVersion());
						}
					}
				}
			}
			
			connection.close();

		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return pomRecommendations;
	}

	public static void groupDependencies(String originalPomPath) {
		
		System.out.println("Grouping Module");
		
	    try {
			String DB_URL = Utils.get_DB_URL(originalPomPath);
			Connection connection = DriverManager.getConnection(DB_URL);
			
			List<String> groupIds = DependencyDB.getGropuIds(connection);
			for(String groupId : groupIds)
			{
				List<DependencyDTO> dependencyList = DependencyDB.getGropuIdVersions(connection, groupId);
				long latestTimeStamp = 0;
				String latestVersion = "";
				for(DependencyDTO dependency : dependencyList)
				{
					long timestamp = VersionFetcher.getTimeStampforDependency(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
					if(timestamp>latestTimeStamp)
					{
						latestTimeStamp = timestamp;
						latestVersion = dependency.getVersion();
					}
				}
				DependencyDB.updateGroupIdVersions(connection, groupId, latestVersion);
			}
			
			connection.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		
	}
	
	public static void generateRecommendedPOM(String originalPomPath, String pomPath) {
		
		System.out.println("Recommended POM Module - "+pomPath);
		
		String pomPathFile = pomPath+"\\"+Constants.pomFileName;
		
		String[] pomPathSplit = pomPath.split("\\\\");
		String moduleName = pomPathSplit[pomPathSplit.length-1];
		
	    try {
			String DB_URL = Utils.get_DB_URL(originalPomPath);
			Connection connection = DriverManager.getConnection(DB_URL);
			
			// replicate the same pom of the module and add the temporary dependencies
			
			File originalPom = new File(pomPath+"\\"+Constants.pomFileName);
			File temporaryPom = new File(pomPath+"\\"+Constants.recommendedPomFileName);
			
			try {
				Files.copy(originalPom.toPath(), temporaryPom.toPath(), StandardCopyOption.REPLACE_EXISTING);
				
				List<Dependency> dependencies = PomOperations.getAllPomDependencies(pomPath+"\\"+Constants.recommendedPomFileName);
				
				for(Dependency dependency : dependencies)
				{
					String recommendedVersion = DependencyDB.getVersionOfDependency(connection, dependency.getGroupId(), dependency.getArtifactId(), moduleName);
					if(!recommendedVersion.equalsIgnoreCase("NA"))
					{
						DependencyOperations.updateDependencyVersion(pomPath+"\\"+Constants.recommendedPomFileName, dependency.getGroupId(), dependency.getArtifactId(), recommendedVersion);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			List<DependencyDTO> temporaryDependencies = DependencyDB.getModulewiseTemporaryDependencies(connection, moduleName);
			for(DependencyDTO tempDependency : temporaryDependencies)
			{
				DependencyOperations.addDependency(pomPath+"\\"+Constants.recommendedPomFileName, tempDependency.getGroupId(), tempDependency.getArtifactId(), tempDependency.getVersion());
			}
			
			connection.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

		List<String> modules = PomOperations.getPomModules(pomPathFile);
		for(String module : modules)
		{
			generateRecommendedPOM(originalPomPath, pomPath+"\\"+module);
		}
	}
	
	public static void generateReport(String originalPomPath, String pomPath) {
		
		System.out.println("Report Module - "+pomPath);
		
		String pomPathFile = pomPath+"\\"+Constants.pomFileName;
//		String tempPomPathFile = originalPomPath+"\\"+Constants.tempPomFileName;
		
		String[] pomPathSplit = pomPath.split("\\\\");
		String moduleName = pomPathSplit[pomPathSplit.length-1];
		
		generateModuleReport(originalPomPath, pomPath, moduleName);
		
		List<String> modules = PomOperations.getPomModules(pomPathFile);
		for(String module : modules)
		{
			generateReport(originalPomPath, pomPath+"\\"+module);
		}
		
	}

	public static void generateModuleReport(String originalPomPath, String pomPath, String moduleName) {
		
		String temppomDirectory = originalPomPath+"\\"+Constants.tempPomFileDirectoryName;
		String tempPomDependencyTreePath = temppomDirectory+"\\"+Constants.dependencyTreeName;
		String dependencyCheckReportLocation = temppomDirectory+"\\"+Constants.dependencyCheckReportName;
		String tempPomFileLocation = temppomDirectory+"\\"+Constants.tempPomFileName;
		String originalTreeImgFilePath = temppomDirectory+"\\"+Constants.originalTreeImgName;
		String recommendedTreeImgFilePath = temppomDirectory+"\\"+Constants.recommendedTreeImgName;
		
	    try {
			String DB_URL = Utils.get_DB_URL(originalPomPath);
			Connection connection = DriverManager.getConnection(DB_URL);
		
			List<DependencyDTO> reportDependencies = DependencyDB.getReportDependencyTable(connection, moduleName);
			
			String originalVulString = "";
			String recommendedVulString = "";
			int originalVulCount = 0;
			int recommendedVulCount = 0;
			
			
			
			
			DependencyOperations.clearAllDependencies(tempPomFileLocation);

			for(DependencyDTO dependency : reportDependencies)
			{
				String version = dependency.getVersion().split(",")[0];
				DependencyOperations.addDependency(tempPomFileLocation, dependency.getGroupId(), dependency.getArtifactId(), version);
			}
			
			Map<String, List<VulnerabilityDTO>> originalVulnerabilityMap = VulnerabilityAnalyzer.getVulnerabilityMapOfPom(tempPomFileLocation, temppomDirectory, tempPomDependencyTreePath, dependencyCheckReportLocation);
			originalVulString = VulnerabilityAnalyzer.getVulStringOfPom(tempPomFileLocation, temppomDirectory, tempPomDependencyTreePath, dependencyCheckReportLocation, originalVulnerabilityMap);
			if(!originalVulString.equalsIgnoreCase(""))
			{
				String[] vulArr = originalVulString.split(",");
				originalVulCount = vulArr.length;
			}
			DependencyOperations.generateDependencyTreeWithPath(tempPomFileLocation, temppomDirectory, tempPomDependencyTreePath);
			graphVisual.generateGraphDOT(temppomDirectory, originalTreeImgFilePath, tempPomDependencyTreePath, originalVulnerabilityMap);
			
			
			
			
			
			DependencyOperations.clearAllDependencies(tempPomFileLocation);

			for(DependencyDTO dependency : reportDependencies)
			{
				String version = dependency.getVersion().split(",")[1];
				DependencyOperations.addDependency(tempPomFileLocation, dependency.getGroupId(), dependency.getArtifactId(), version);
			}

			Map<String, List<VulnerabilityDTO>> recommendedVulnerabilityMap = VulnerabilityAnalyzer.getVulnerabilityMapOfPom(tempPomFileLocation, temppomDirectory, tempPomDependencyTreePath, dependencyCheckReportLocation);
			recommendedVulString = VulnerabilityAnalyzer.getVulStringOfPom(tempPomFileLocation, temppomDirectory, tempPomDependencyTreePath, dependencyCheckReportLocation, recommendedVulnerabilityMap);
			if(!recommendedVulString.equalsIgnoreCase(""))
			{
				String[] vulArr = recommendedVulString.split(",");
				recommendedVulCount = vulArr.length;
			}
			DependencyOperations.generateDependencyTreeWithPath(tempPomFileLocation, temppomDirectory, tempPomDependencyTreePath);
			graphVisual.generateGraphDOT(temppomDirectory, recommendedTreeImgFilePath, tempPomDependencyTreePath, recommendedVulnerabilityMap);

			
			
			ImageData originalData = ImageDataFactory.create(originalTreeImgFilePath);
			Image originalImage = new Image(originalData);
			
			ImageData recommendedData = ImageDataFactory.create(recommendedTreeImgFilePath);
			Image recommendedImage = new Image(recommendedData);

			
			System.out.println(originalVulString);
			System.out.println(recommendedVulString);
			System.out.println(originalVulCount);
			System.out.println(recommendedVulCount);
			
			
//			write table into the pdf
	        Path directoryPath = Paths.get(originalPomPath+"\\"+Constants.reportsDirectoryName);

	        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
	            System.out.println("\nDirectory does not exist: " + directoryPath.toString());
				Files.createDirectories(Paths.get(directoryPath.toString()));
	        } else {
	            System.out.println("\nDirectory exists: " + directoryPath.toString());
	        }
	        
			String dest = originalPomPath+"\\"+Constants.reportsDirectoryName+"\\"+"Vulnerability_Report_"+moduleName+"_"+System.currentTimeMillis()+".pdf";
	        try {
	            PdfWriter writer = new PdfWriter(dest);
	            PdfDocument pdf = new PdfDocument(writer);
	            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);
	            
	            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

	            LocalDateTime currentDateTime = LocalDateTime.now();

	            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	            String formattedDateTime = currentDateTime.format(formatter);
	            Text Heading = new Text("MAVO Report").setFontSize(20); // 20pt font
	            
	            document.add(new Paragraph().add(Heading));
	            document.add(new Paragraph().add("Date of report generation - "+formattedDateTime));
	            
	            // Create a table with 4 columns
	            float[] columnWidths = {150f, 150f, 100f, 100f};
	            Table table = new Table(columnWidths);
	            
	            // Add table headers
	            table.addHeaderCell("Group ID");
	            table.addHeaderCell("Artifact ID");
	            table.addHeaderCell("Old Version");
	            table.addHeaderCell("New Version");
	            
	            // Add table data
	            String[][] data = new String[reportDependencies.size()][4];
	            
	            int count = 0;
				for(DependencyDTO dependency : reportDependencies)
				{
					String resolvedVersion = dependency.getVersion().split(",")[0];
					String recommendedVersion = dependency.getVersion().split(",")[1];
					data[count][0] = dependency.getGroupId();
					data[count][1] = dependency.getArtifactId();
					data[count][2] = resolvedVersion;
					data[count][3] = recommendedVersion;
					count++;
				}
	            
	            for (String[] row : data) {
	                for (String cell : row) {
	                    table.addCell(cell);
	                }
	            }
	            
	            document.add(new Paragraph().setPaddingTop(20));
	            document.add(new Paragraph().add("Recommendations - "));
	            document.add(table);
	            
	            document.add(new Paragraph().setPaddingTop(20));
//	            document.add(new Paragraph().add("Old POM.xml vulnerabilities - "+originalVulString));

	            document.add(new Paragraph().add("Original POM Vulnerabilities\n").setFont(boldFont));
	            document.add(new Paragraph().add("Old POM.xml vulnerabilities count - "+originalVulCount).setFont(boldFont));
	            
	            // Create table with 2 columns
	            Table table1 = new Table(UnitValue.createPercentArray(new float[]{3, 5, 3})); // Column widths
	            table1.setWidth(UnitValue.createPercentValue(100));

	            // Add headers
	            table1.addHeaderCell(new Cell().add(new Paragraph("Artifact ID").setBold()).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
	            table1.addHeaderCell(new Cell().add(new Paragraph("Vulnerabilities").setBold()).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
	            table1.addHeaderCell(new Cell().add(new Paragraph("Severity").setBold()).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));

	            // Populate the table with merged cells
	            for (Map.Entry<String, List<VulnerabilityDTO>> entry : originalVulnerabilityMap.entrySet()) {
	                String artifactId = entry.getKey();
	                List<VulnerabilityDTO> vulnerabilities = entry.getValue();

	                // Merge artifact ID cell across the number of vulnerabilities
	                Cell artifactCell = new Cell(vulnerabilities.size(), 1).add(new Paragraph(artifactId).setBold());
	                artifactCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
	                table1.addCell(artifactCell);

	                // Add vulnerability rows
	                for (VulnerabilityDTO vul : vulnerabilities) {
	                	table1.addCell(new Cell().add(new Paragraph(vul.getVulnerabilityName())));
	                	table1.addCell(new Cell().add(new Paragraph(vul.getSeverity()).setFontColor(getSeverityColor(vul.getSeverity()))));
	                }
	            }

	            // Add table to document
	            document.add(table1);

//	            document.add(new Paragraph().add("New POM.xml vulnerabilities - "+recommendedVulString));
	            
	            document.add(new Paragraph().add("\n\nRecommended POM Vulnerabilities\n").setFont(boldFont));
	            document.add(new Paragraph().add("New POM.xml vulnerabilities count - "+recommendedVulCount).setFont(boldFont));

	            // Create table with 2 columns
	            Table table2 = new Table(UnitValue.createPercentArray(new float[]{3, 5, 3})); // Column widths
	            table2.setWidth(UnitValue.createPercentValue(100));

	            // Add headers
	            table2.addHeaderCell(new Cell().add(new Paragraph("Artifact ID").setBold()).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
	            table2.addHeaderCell(new Cell().add(new Paragraph("Vulnerabilities").setBold()).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
	            table2.addHeaderCell(new Cell().add(new Paragraph("Severity").setBold()).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));

	            // Populate the table with merged cells
	            for (Map.Entry<String, List<VulnerabilityDTO>> entry : recommendedVulnerabilityMap.entrySet()) {
	                String artifactId = entry.getKey();
	                List<VulnerabilityDTO> vulnerabilities = entry.getValue();

	                // Merge artifact ID cell across the number of vulnerabilities
	                Cell artifactCell = new Cell(vulnerabilities.size(), 1).add(new Paragraph(artifactId).setBold());
	                artifactCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
	                table2.addCell(artifactCell);

	                // Add vulnerability rows
	                for (VulnerabilityDTO vul : vulnerabilities) {
	                	table2.addCell(new Cell().add(new Paragraph(vul.getVulnerabilityName())));
	                	table2.addCell(new Cell().add(new Paragraph(vul.getSeverity()).setFontColor(getSeverityColor(vul.getSeverity()))));
	                }
	            }

	            // Add table to document
	            document.add(table2);
	            
				document.add(new AreaBreak()); // Move to another new page
				
	            document.add(new Paragraph().add("Original Project Dependency Sturcture").setFont(boldFont).setFixedPosition(30, 740, 500));

				originalImage.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
				originalImage.setFixedPosition(0, pdf.getDefaultPageSize().getHeight() - originalImage.getImageHeight());
				document.add(originalImage);
				
				document.add(new Paragraph(""));
	            document.add(new Paragraph().add("Recommended Project Dependency Sturcture").setFont(boldFont).setFixedPosition(30, 470, 500));

				recommendedImage.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
				recommendedImage.setFixedPosition(0, pdf.getDefaultPageSize().getHeight() - recommendedImage.getImageHeight() - originalImage.getImageHeight());
				document.add(recommendedImage);
				
	            document.close();
	            System.out.println("PDF created: " + dest);
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        }

	        connection.close();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public static void cleanup(String originalPomPath) {
		String folderPath = originalPomPath+"\\"+Constants.tempPomFileDirectoryName;
        try {
            Path path = Paths.get(folderPath);
            if (Files.exists(path)) {
                Files.walk(path)
                     .sorted((p1, p2) -> p2.compareTo(p1)) // Reverse order to delete files before directories
                     .forEach(p -> {
                         try {
                             Files.delete(p);
                             System.out.println("Deleted: " + p);
                         } catch (IOException e) {
                             System.err.println("Failed to delete: " + p);
                         }
                     });
                System.out.println("Folder deleted successfully.");
            } else {
                System.out.println("Folder does not exist.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
    private static Color getSeverityColor(String severity) {
    	if(severity.equalsIgnoreCase("critical")) {
    		return ColorConstants.RED;
    	} else if(severity.equalsIgnoreCase("high")) {
    		return new DeviceRgb(255, 140, 0);
    	} 
//		else if(severity.equalsIgnoreCase("medium")) {
//    		return ColorConstants.YELLOW;
//    	} else if(severity.equalsIgnoreCase("low")) {
//    		return ColorConstants.GREEN;
//		}
    	return ColorConstants.BLACK;
    }
}
