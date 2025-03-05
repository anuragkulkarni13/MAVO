package common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import common.dto.POMDependencyDTO;

public class Constants {

	public static String VulnerabilityCache = "VulnerabilityCache";

	public static String CombinationsCache = "CombinationsCache";
	
	public static String DependencyTable = "DependencyTable";

	public static String VersionTable = "VersionTable";
	
	public static String DB_NAME = "MAVO_DB.db";
	
	public static String pomFileName = "pom.xml";
	
	public static String recommendedPomFileName = "recommendedPom.xml";
	
	public static String dependencyManagementType = "dependencyManagement";

	public static String pomDependencyType = "pom";

	public static String normalDependencyType = "normal";

	public static String permanentDependencyNature = "permanent";

	public static String temporaryDependencyNature = "temporary";
	
	public static String parentDependencytype = "pom";
	
	public static String excludeListFileName = "excludeList.xml";
	
	public static int ROWS_PER_PAGE = 100;  // Number of results per page

	public static String tempPomFileDirectoryName = "tempPOMForDirectDependencies";
	
	public static String tempPomFileName = "temppom.xml";

	public static String dependencyTreeName = "dependency-tree.json";
	
	public static String dependencyCheckReportName = "dependency-check-report.json";

	public static String reportsDirectoryName = "Reports";
	
	public static String originalTreeImgName = "originalTree.png";

	public static String recommendedTreeImgName = "recommendedTree.png";

// needs to be externalized
	
	public static String mvnPath = "C:\\Program Files\\apache-maven-3.9.8\\bin";
	
	public static String dotPath = "C:\\Program Files\\Graphviz\\bin\\dot";

	public static String mavenDependencyPluginVersion = "3.8.1";
	
	public static String dependencyCheckMavenVersion = "12.1.0";
	
	public static String versionsMavenPluginVersion = "2.18.0";
	
//	public static Map<String, String> globalpropertiesMap = new HashMap<>();
//	
//	public static Map<String, String> excludeList = new HashMap<>();
//	
//	public static List<POMDependencyDTO> parentDependencies = new ArrayList<>();
//
//	public static List<POMDependencyDTO> depMgmtDependencies = new ArrayList<>();
//
//	public static Map<String, List<String>> dependencyMap = new HashMap<>();
//	
//	public static Map<String, List<POMDependencyDTO>> finalDepMgmtChanges = new HashMap<>();
//
//	public static Map<String, String> keyModule = new HashMap<>();
//
//	
//
//	
//	public static String recommendedPomFileName = "\\recommendedPom.xml";
//	

//	

//	
//	
//	public static String reportsFileName = "\\Vulnerability_Report";
//	
//	public static String treeImgName = "\\tree.png";
//
//	public static String treeImgName1 = "\\tree1.png";
//	
//	public static String excludeListName = "\\excludeList.xml";
//	

//	


}
