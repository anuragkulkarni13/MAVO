package reporting;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;

import common.Constants;
import common.dto.POMDependencyDTO;
import common.dto.VulnerabilityDTO;
import pom.DependencyOperations;
import pom.PomOperations;
import visualization.graphVisual;
import vulnerability.VulnerabilityAnalyzer;

public class Reporting {

	public static void createReport1(String originalPomPath, String pomPath)
	{
		System.out.println("Report Module - "+pomPath);
		
		String[] arr = pomPath.split("\\\\");
		String pomModule = arr[arr.length-1];
		
		try
		{

//			System.out.println(pomPath+Constants.pomFileName);
//			FileReader pomReader = new FileReader(pomPath+Constants.pomFileName);
//
//			MavenXpp3Reader pomMavenReader = new MavenXpp3Reader();
//			Model pomModel = pomMavenReader.read(pomReader);
//			
//			List<Dependency> pomDepMgmtDeps = new ArrayList<>();
//			List<Dependency> pomDeps = new ArrayList<>();
//			
//			DependencyManagement pomDepManagement = pomModel.getDependencyManagement();
//			//          System.out.println(depManagement);
//			if (pomDepManagement != null) {
//				pomDepMgmtDeps = pomDepManagement.getDependencies();
//			}
//			
//			pomDeps = pomModel.getDependencies();
			
			
			List<Dependency> pomDepMgmtDeps = PomOperations.getDepMgmtDepndenciesFromPOM(pomPath+Constants.pomFileName);
			List<POMDependencyDTO> pomDeps1 = PomOperations.getDepndenciesFromPOM(pomPath+Constants.pomFileName);
			
			System.out.println("\nDep Mgmt Dependencies in pom.xml");
			for(Dependency d : pomDepMgmtDeps)
			{
				System.out.println(d);
			}
			
			List<Dependency> pomDeps = pomDeps1.stream()
            .map(dto -> {
                Dependency dependency = new Dependency();
                dependency.setGroupId(dto.getGroupId());
                dependency.setArtifactId(dto.getArtifactId());
                dependency.setVersion(dto.getVersion());
                return dependency;
            })
            .collect(Collectors.toList());
			
			System.out.println("\nDependencies in pom.xml");
			for(Dependency d : pomDeps)
			{
				System.out.println(d);
			}
			
			
			
			
//			System.out.println(pomPath+Constants.recommendedPomFileName);
			FileReader recReader = new FileReader(pomPath+Constants.recommendedPomFileName);

			MavenXpp3Reader recMavenReader = new MavenXpp3Reader();
			Model recModel = recMavenReader.read(recReader);
			
			List<Dependency> recDepMgmtDeps = new ArrayList<>();
			List<Dependency> recDeps = new ArrayList<>();
			
			DependencyManagement recDepManagement = recModel.getDependencyManagement();
			//          System.out.println(depManagement);
			if (recDepManagement != null) {
				recDepMgmtDeps = recDepManagement.getDependencies();
			}
			
			recDeps = recModel.getDependencies();
			
			System.out.println("\nDep Mgmt Recommendations in recommendedPom.xml");
			for(Dependency d : recDepMgmtDeps)
			{
				System.out.println(d);
			}
			
			System.out.println("\nDep Recommendations in recommendedPom.xml");
			for(Dependency d : recDeps)
			{
				System.out.println(d);
			}
			
			createModuleReport1(originalPomPath, pomModule, pomDepMgmtDeps, pomDeps, recDepMgmtDeps, recDeps);
			
			
		}catch(Exception e){
			System.out.println(e);
		}

		
		List<String> modules = PomOperations.getPomModules(pomPath+Constants.pomFileName);
		
		for(String module : modules)
		{
			createReport1(originalPomPath, pomPath+"\\"+module);
		}
	}
	
	public static void createModuleReport1(String originalPomPath, String moduleName, List<Dependency> pomDepMgmtDeps, List<Dependency> pomDeps, 
			List<Dependency> recDepMgmtDeps, List<Dependency> recDeps)
	{
		
		String newPomFilePath = originalPomPath+Constants.tempPomFileDirectoryName+Constants.tempPomFileName;
		String newPomPath = originalPomPath+Constants.tempPomFileDirectoryName;
		String newPomDependencyCheckReportPath = newPomPath+Constants.dependencyCheckReportName;
		String newPomDependencyTreePath = newPomPath+Constants.dependencyTreeName;
		String treeImgPath = newPomPath+Constants.treeImgName;
		String treeImgPath1 = newPomPath+Constants.treeImgName1;
        
		try {
	        Path directoryPath = Paths.get(originalPomPath+Constants.reportsDirectoryName);

	        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
	            System.out.println("\nDirectory does not exist: " + originalPomPath+Constants.reportsDirectoryName);
				Files.createDirectories(Paths.get(originalPomPath+Constants.reportsDirectoryName));
	        } else {
	            System.out.println("\nDirectory exists: " + originalPomPath+Constants.reportsDirectoryName);
	        }
	        
            LocalDateTime currentDateTime = LocalDateTime.now();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String formattedDateTime = currentDateTime.format(formatter);
            
            PdfWriter writer = new PdfWriter(originalPomPath+Constants.reportsDirectoryName+Constants.reportsFileName+"_"+moduleName+"_"+System.currentTimeMillis()+".pdf");

            PdfDocument pdfDoc = new PdfDocument(writer);

            Document document = new Document(pdfDoc);

            Text Heading = new Text("MAVO Report").setFontSize(20); // 20pt font
            
            document.add(new Paragraph().add(Heading));
            document.add(new Paragraph().add("Date of report generation - "+formattedDateTime));
            
            document.add(new Paragraph(new Text("Original POM").setFontSize(16)));
			PomOperations.clearAllDependencies(newPomFilePath);

			Map<String, String> pomProperties = PomOperations.getPomProperties(newPomFilePath);
			
			for (Map.Entry<String, String> entry : pomProperties.entrySet()) {
	            document.add(new Paragraph("<"+entry.getKey()+">"+entry.getValue()+"<"+entry.getKey()+">"));
			}
			
			document.add(new Paragraph(""));
			document.add(new Paragraph("<dependencyManagement>"));
            for(Dependency dep : pomDepMgmtDeps)
			{
	            document.add(new Paragraph("`        <dependency>"));
	            document.add(new Paragraph("`                <groupId>"+dep.getGroupId()+"</groupId>"));
	            document.add(new Paragraph("`                <artifactId>"+dep.getArtifactId()+"</artifactId>"));
	            document.add(new Paragraph("`                <version>"+dep.getVersion()+"</version>"));
	            document.add(new Paragraph("`        </dependency>"));
			}
            document.add(new Paragraph("</dependencyManagement>"));
            
            document.add(new Paragraph(""));
			document.add(new Paragraph("<dependencies>"));
            for(Dependency dep : pomDeps)
			{
	            PomOperations.addDependency(newPomFilePath, dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
	            document.add(new Paragraph("<dependency>"));
	            document.add(new Paragraph("`        <groupId>"+dep.getGroupId()+"</groupId>"));
	            document.add(new Paragraph("`        <artifactId>"+dep.getArtifactId()+"</artifactId>"));
	            document.add(new Paragraph("`        <version>"+dep.getVersion()+"</version>"));
	            document.add(new Paragraph("</dependency>"));
			}
            document.add(new Paragraph("</dependencies>"));
			document.add(new Paragraph(""));
			document.add(new Paragraph(""));

			int vulCount = 0;
			DependencyOperations.generateDependencyTreeWithPath(newPomFilePath, newPomPath, newPomDependencyTreePath);
	        DependencyOperations.generateDependencyCheckReportWithPath(newPomFilePath, newPomPath, newPomDependencyCheckReportPath);
			Map<String, List<VulnerabilityDTO>> vulnerabilityMap = VulnerabilityAnalyzer.getVulnerabilityList(newPomDependencyCheckReportPath);
			
			for (Map.Entry<String, List<VulnerabilityDTO>> entry : vulnerabilityMap.entrySet()) {
				List<VulnerabilityDTO> vulList = entry.getValue();
				String oldPomVulList = "";
				boolean first = true;
				for(VulnerabilityDTO v : vulList)
				{
					if(first == true)
					{
						oldPomVulList += v.getVulnerabilityName();
						first = false;
					}
					else
					{
						oldPomVulList += ", "+v.getVulnerabilityName();
					}
				}
				document.add(new Paragraph(entry.getKey()+" - "+oldPomVulList));
//			    System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$Key: " + entry.getKey() + ", Value: " + entry.getValue().size());
			    vulCount += entry.getValue().size();
			}
			document.add(new Paragraph(""));
			System.out.println("\nOld POM - "+vulCount);
			document.add(new Paragraph(new Text("Original POM Vulnerability count - "+vulCount).setFontSize(16)));
			
			graphVisual.generateGraphDOT(newPomFilePath, newPomPath, newPomDependencyTreePath, newPomDependencyCheckReportPath, treeImgPath);

            ImageData data = ImageDataFactory.create(treeImgPath);
            Image img = new Image(data);
			
            
            
            
            
            
            
            document.add(new AreaBreak());
            document.add(new Paragraph(new Text("Recommended POM").setFontSize(16)));
			PomOperations.clearAllDependencies(newPomFilePath);

			for (Map.Entry<String, String> entry : pomProperties.entrySet()) {
				if(Constants.globalpropertiesMap.containsKey(entry.getKey()))
				{
		            document.add(new Paragraph("<"+entry.getKey()+">"+Constants.globalpropertiesMap.get(entry.getKey())+"<"+entry.getKey()+">"));
				}
				else
				{
		            document.add(new Paragraph("<"+entry.getKey()+">"+entry.getValue()+"<"+entry.getKey()+">"));
				}
			}
			
			document.add(new Paragraph("<dependencyManagement>"));
            for(Dependency dep : recDepMgmtDeps)
			{
	            document.add(new Paragraph("`        <dependency>"));
	            document.add(new Paragraph("`                <groupId>"+dep.getGroupId()+"</groupId>"));
	            document.add(new Paragraph("`                <artifactId>"+dep.getArtifactId()+"</artifactId>"));
	            document.add(new Paragraph("`                <version>"+dep.getVersion()+"</version>"));
	            document.add(new Paragraph("`        </dependency>"));
			}
            document.add(new Paragraph("</dependencyManagement>"));
            
            document.add(new Paragraph(""));
			document.add(new Paragraph("<dependencies>"));
			for(Dependency dep : recDeps)
			{
	            PomOperations.addDependency(newPomFilePath, dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
	            document.add(new Paragraph("<dependency>"));
	            document.add(new Paragraph("`        <groupId>"+dep.getGroupId()+"</groupId>"));
	            document.add(new Paragraph("`        <artifactId>"+dep.getArtifactId()+"</artifactId>"));
	            document.add(new Paragraph("`        <version>"+dep.getVersion()+"</version>"));
	            document.add(new Paragraph("</dependency>"));
			}
			document.add(new Paragraph("</dependencies>"));
			int vulCount1 = 0;
			DependencyOperations.generateDependencyTreeWithPath(newPomFilePath, newPomPath, newPomDependencyTreePath);
	        DependencyOperations.generateDependencyCheckReportWithPath(newPomFilePath, newPomPath, newPomDependencyCheckReportPath);
			Map<String, List<VulnerabilityDTO>> vulnerabilityMap1 = VulnerabilityAnalyzer.getVulnerabilityList(newPomDependencyCheckReportPath);
			
			document.add(new Paragraph(""));

			for (Map.Entry<String, List<VulnerabilityDTO>> entry : vulnerabilityMap1.entrySet()) {
				List<VulnerabilityDTO> vulList = entry.getValue();
				String oldPomVulList = "";
				boolean first = true;
				for(VulnerabilityDTO v : vulList)
				{
					if(first == true)
					{
						oldPomVulList += v.getVulnerabilityName();
						first = false;
					}
					else
					{
						oldPomVulList += ", "+v.getVulnerabilityName();
					}
				}
				document.add(new Paragraph(entry.getKey()+" - "+oldPomVulList));
//			    System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$Key: " + entry.getKey() + ", Value: " + entry.getValue().size());
			    vulCount1 += entry.getValue().size();
			}
			document.add(new Paragraph(""));
			System.out.println("\nNew POM - "+vulCount1);
			document.add(new Paragraph(new Text("Recommended POM Vulnerability count - "+vulCount1).setFontSize(16)));
			
			graphVisual.generateGraphDOT(newPomFilePath, newPomPath, newPomDependencyTreePath, newPomDependencyCheckReportPath, treeImgPath1);

			document.add(new AreaBreak()); // Move to another new page
			ImageData data1 = ImageDataFactory.create(treeImgPath1);
			Image img1 = new Image(data1);
            
			
			img.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
			img.setFixedPosition(0, pdfDoc.getDefaultPageSize().getHeight() - img.getImageHeight());
			document.add(img);
			document.add(new Paragraph(""));
			img1.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
			img1.setFixedPosition(0, pdfDoc.getDefaultPageSize().getHeight() - img1.getImageHeight() - img.getImageHeight());
			document.add(img1);
			
            document.close();

            System.out.println("\nPDF created successfully!\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	
//	public static void createReport(String originalPomPath, String pomPath, 
//			Map<String, List<POMDependencyDTO>> finalChanges)
//	{
//		System.out.println("Module - "+pomPath);
//		
//		String[] arr = pomPath.split("\\\\");
//		String pomModule = arr[arr.length-1];
//		
//		List<POMDependencyDTO> depMgmtRecommendations = new ArrayList<>();
//		List<POMDependencyDTO> depRecommendations = new ArrayList<>();
//				
//		if(Constants.finalDepMgmtChanges.containsKey(pomModule))
//		{
//			depMgmtRecommendations = Constants.finalDepMgmtChanges.get(pomModule);
//		}
//		
//		if(finalChanges.containsKey(pomModule))
//		{
//			depRecommendations = finalChanges.get(pomModule);
//		}
//		
//		List<Dependency> OriginalDepMgmtList = PomOperations.getDepMgmtDepndenciesFromPOM(pomPath+Constants.pomFileName);
//		List<POMDependencyDTO> OriginalDepList = PomOperations.getDepndenciesFromPOM(pomPath+Constants.pomFileName);
//		
//		createModuleReport(originalPomPath, pomModule, OriginalDepMgmtList, OriginalDepList, depMgmtRecommendations, depRecommendations);
//		
//		List<String> modules = PomOperations.getPomModules(pomPath+Constants.pomFileName);
//		
//		for(String module : modules)
//		{
//			createReport(originalPomPath, pomPath+"\\"+module, finalChanges);
//		}
//	}
//	
//	public static void createModuleReport(String originalPomPath, String moduleName, 
//			List<Dependency> OriginalDepMgmtList, List<POMDependencyDTO> OriginalDepList, 
//			List<POMDependencyDTO> depMgmtRecommendations, List<POMDependencyDTO> depRecommendations)
//	{
//		
////		System.out.println(OriginalDepMgmtList);
////		System.out.println(OriginalDepList);
////		System.out.println(depMgmtRecommendations);
////		System.out.println(depRecommendations);
//		
//		String newPomFilePath = originalPomPath+Constants.tempPomFileDirectoryName+Constants.tempPomFileName;
//		String newPomPath = originalPomPath+Constants.tempPomFileDirectoryName;
//		String newPomDependencyCheckReportPath = newPomPath+Constants.dependencyCheckReportName;
//		String newPomDependencyTreePath = newPomPath+Constants.dependencyTreeName;
//		String treeImgPath = newPomPath+Constants.treeImgName;
//        
//		try {
//	        Path directoryPath = Paths.get(originalPomPath+Constants.reportsDirectoryName);
//
//	        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
////	            System.out.println("Directory does not exist: " + originalPomPath+Constants.reportsDirectoryName);
//				Files.createDirectories(Paths.get(originalPomPath+Constants.reportsDirectoryName));
//	        } else {
////	            System.out.println("Directory exists: " + originalPomPath+Constants.reportsDirectoryName);
//	        }
//	        
//            LocalDateTime currentDateTime = LocalDateTime.now();
//
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//            String formattedDateTime = currentDateTime.format(formatter);
//            
//            PdfWriter writer = new PdfWriter(originalPomPath+Constants.reportsDirectoryName+Constants.reportsFileName+"_"+moduleName+"_"+System.currentTimeMillis()+".pdf");
//
//            PdfDocument pdfDoc = new PdfDocument(writer);
//
//            Document document = new Document(pdfDoc);
//
//            Text Heading = new Text("MAVO Report").setFontSize(20); // 20pt font
//            
//            document.add(new Paragraph().add(Heading));
//            document.add(new Paragraph().add("Date of report generation - "+formattedDateTime));
//            
//            document.add(new Paragraph(new Text("Original POM").setFontSize(16)));
//			PomOperations.clearAllDependencies(newPomFilePath);
//
//			Map<String, String> pomProperties = PomOperations.getPomProperties(newPomFilePath);
//			
//			for (Map.Entry<String, String> entry : pomProperties.entrySet()) {
//	            document.add(new Paragraph("<"+entry.getKey()+">"+entry.getValue()+"<"+entry.getKey()+">"));
//			}
//			
//			document.add(new Paragraph(""));
//			document.add(new Paragraph("<dependencyManagement>"));
//            for(Dependency dep : OriginalDepMgmtList)
//			{
//	            document.add(new Paragraph("`        <dependency>"));
//	            document.add(new Paragraph("`                <groupId>"+dep.getGroupId()+"</groupId>"));
//	            document.add(new Paragraph("`                <artifactId>"+dep.getArtifactId()+"</artifactId>"));
//	            document.add(new Paragraph("`                <version>"+dep.getVersion()+"</version>"));
//	            document.add(new Paragraph("`        </dependency>"));
//			}
//            document.add(new Paragraph("</dependencyManagement>"));
//            for(POMDependencyDTO dep : OriginalDepList)
//			{
//	            PomOperations.addDependency(newPomFilePath, dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
//	            document.add(new Paragraph("<dependency>"));
//	            document.add(new Paragraph("`        <groupId>"+dep.getGroupId()+"</groupId>"));
//	            document.add(new Paragraph("`        <artifactId>"+dep.getArtifactId()+"</artifactId>"));
//	            document.add(new Paragraph("`        <version>"+dep.getVersion()+"</version>"));
//	            document.add(new Paragraph("</dependency>"));
//			}
//			document.add(new Paragraph(""));
//			document.add(new Paragraph(""));
//
//			int vulCount = 0;
//			DependencyOperations.generateDependencyTreeWithPath(newPomFilePath, newPomPath, newPomDependencyTreePath);
//	        DependencyOperations.generateDependencyCheckReportWithPath(newPomFilePath, newPomPath, newPomDependencyCheckReportPath);
//			Map<String, List<VulnerabilityDTO>> vulnerabilityMap = VulnerabilityAnalyzer.getVulnerabilityList(newPomDependencyCheckReportPath);
//			
//			for (Map.Entry<String, List<VulnerabilityDTO>> entry : vulnerabilityMap.entrySet()) {
//				List<VulnerabilityDTO> vulList = entry.getValue();
//				String oldPomVulList = "";
//				boolean first = true;
//				for(VulnerabilityDTO v : vulList)
//				{
//					if(first == true)
//					{
//						oldPomVulList += v.getVulnerabilityName();
//						first = false;
//					}
//					else
//					{
//						oldPomVulList += ", "+v.getVulnerabilityName();
//					}
//				}
//				document.add(new Paragraph(entry.getKey()+" - "+oldPomVulList));
////			    System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$Key: " + entry.getKey() + ", Value: " + entry.getValue().size());
//			    vulCount += entry.getValue().size();
//			}
//			document.add(new Paragraph(""));
//			System.out.println("Old POM - "+vulCount);
//			document.add(new Paragraph(new Text("Original POM Vulnerability count - "+vulCount).setFontSize(16)));
//			
//			graphVisual.generateGraphDOT(newPomFilePath, newPomPath, newPomDependencyTreePath, newPomDependencyCheckReportPath);
//
////            document.add(new AreaBreak()); // Move to a new page
//            ImageData data = ImageDataFactory.create(treeImgPath);
//            Image img = new Image(data);
////            img.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
////            img.setFixedPosition(0, pdfDoc.getDefaultPageSize().getHeight() - img.getImageHeight());
////            document.add(img);
//			
//			
//            
//            
//            
//            document.add(new AreaBreak());
//            document.add(new Paragraph(new Text("Recommended POM").setFontSize(16)));
//			PomOperations.clearAllDependencies(newPomFilePath);
//
//			for (Map.Entry<String, String> entry : pomProperties.entrySet()) {
//				if(Constants.globalpropertiesMap.containsKey(entry.getKey()))
//				{
//		            document.add(new Paragraph("<"+entry.getKey()+">"+Constants.globalpropertiesMap.get(entry.getKey())+"<"+entry.getKey()+">"));
//				}
//				else
//				{
//		            document.add(new Paragraph("<"+entry.getKey()+">"+entry.getValue()+"<"+entry.getKey()+">"));
//				}
//			}
//			
//			document.add(new Paragraph("<dependencyManagement>"));
//            for(POMDependencyDTO dep : depMgmtRecommendations)
//			{
//	            document.add(new Paragraph("`        <dependency>"));
//	            document.add(new Paragraph("`                <groupId>"+dep.getGroupId()+"</groupId>"));
//	            document.add(new Paragraph("`                <artifactId>"+dep.getArtifactId()+"</artifactId>"));
//	            document.add(new Paragraph("`                <version>"+dep.getVersion()+"</version>"));
//	            document.add(new Paragraph("`        </dependency>"));
//			}
//            document.add(new Paragraph("</dependencyManagement>"));
//			for(POMDependencyDTO dep : depRecommendations)
//			{
//	            PomOperations.addDependency(newPomFilePath, dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
//	            document.add(new Paragraph("<dependency>"));
//	            document.add(new Paragraph("`        <groupId>"+dep.getGroupId()+"</groupId>"));
//	            document.add(new Paragraph("`        <artifactId>"+dep.getArtifactId()+"</artifactId>"));
//	            document.add(new Paragraph("`        <version>"+dep.getVersion()+"</version>"));
//	            document.add(new Paragraph("</dependency>"));
//			}
//			int vulCount1 = 0;
//			DependencyOperations.generateDependencyTreeWithPath(newPomFilePath, newPomPath, newPomDependencyTreePath);
//	        DependencyOperations.generateDependencyCheckReportWithPath(newPomFilePath, newPomPath, newPomDependencyCheckReportPath);
//			Map<String, List<VulnerabilityDTO>> vulnerabilityMap1 = VulnerabilityAnalyzer.getVulnerabilityList(newPomDependencyCheckReportPath);
//			
//			document.add(new Paragraph(""));
//
//			for (Map.Entry<String, List<VulnerabilityDTO>> entry : vulnerabilityMap1.entrySet()) {
//				List<VulnerabilityDTO> vulList = entry.getValue();
//				String oldPomVulList = "";
//				boolean first = true;
//				for(VulnerabilityDTO v : vulList)
//				{
//					if(first == true)
//					{
//						oldPomVulList += v.getVulnerabilityName();
//						first = false;
//					}
//					else
//					{
//						oldPomVulList += ", "+v.getVulnerabilityName();
//					}
//				}
//				document.add(new Paragraph(entry.getKey()+" - "+oldPomVulList));
////			    System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$Key: " + entry.getKey() + ", Value: " + entry.getValue().size());
//			    vulCount1 += entry.getValue().size();
//			}
//			document.add(new Paragraph(""));
//			System.out.println("New POM - "+vulCount1);
//			document.add(new Paragraph(new Text("Recommended POM Vulnerability count - "+vulCount1).setFontSize(16)));
//			
//			graphVisual.generateGraphDOT(newPomFilePath, newPomPath, newPomDependencyTreePath, newPomDependencyCheckReportPath);
//
//			document.add(new AreaBreak()); // Move to another new page
//			ImageData data1 = ImageDataFactory.create(treeImgPath);
//			Image img1 = new Image(data1);
////			img1.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
////			img1.setFixedPosition(0, pdfDoc.getDefaultPageSize().getHeight() - img1.getImageHeight());
////			document.add(img1);
//            
//			
////			document.add(new Paragraph(new Text("Original POM Vulnerability count - "+vulCount).setFontSize(16)));
//			img.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
//			img.setFixedPosition(0, pdfDoc.getDefaultPageSize().getHeight() - img.getImageHeight());
//			document.add(img);
//			document.add(new Paragraph(""));
////			document.add(new Paragraph(new Text("Recommended POM Vulnerability count - "+vulCount1).setFontSize(16)));
//			img1.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
//			img1.setFixedPosition(0, pdfDoc.getDefaultPageSize().getHeight() - img1.getImageHeight());
//			document.add(img1);
//			
//            document.close();
//
//            System.out.println("PDF created successfully!");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//	}
//	
}
