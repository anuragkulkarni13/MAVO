package pom;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import common.Constants;

public class PomOperations {

	public static List<String> getPomModules(String pomFilePath)
	{
		List<String> modules = new ArrayList<>();
		try
		{
			FileReader reader = new FileReader(pomFilePath);

			MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
			Model model = xpp3Reader.read(reader);

			modules =  model.getModules();

		}catch (Exception e) {
			System.out.println(e);
		}
		
		return modules;
	}
	
	public static Map<String, String> getPomProperties(String pomPathFile)
	{
		Map<String, String> propertiesMap = new HashMap<>();
		try
		{
			FileReader reader = new FileReader(pomPathFile);

			MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
			Model model = xpp3Reader.read(reader);

			Properties properties = model.getProperties();
			Set<Object> keys = properties.keySet();
			for(Object key : keys)
			{
				propertiesMap.put(key.toString(), properties.getProperty(key.toString()));
			}

		}catch (Exception e) {
			System.out.println(e);
		}
		
		return propertiesMap;
	}

	public static Map<String, List<Dependency>> getSegregatedPomDependencies(String pomPath){
		
		Map<String, List<Dependency>> segregatedDependencies = new HashMap<>();
		try
		{
			FileReader reader = new FileReader(pomPath);

			MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
			Model model = xpp3Reader.read(reader);
			
			List<Dependency> depMgmtDependencies = new ArrayList<>();			
			List<Dependency> normalDependencies = new ArrayList<>();			
			DependencyManagement depMgmt = model.getDependencyManagement();
			if(depMgmt != null)
			{
				depMgmtDependencies = depMgmt.getDependencies();
			}
			normalDependencies = model.getDependencies();

			segregatedDependencies.put(Constants.dependencyManagementType, depMgmtDependencies);
			segregatedDependencies.put(Constants.normalDependencyType, normalDependencies);
			
		}catch (Exception e) {
			System.out.println(e);
		}
		
		return segregatedDependencies;
	}
	
	public static List<Dependency> getAllPomDependencies(String pomPath){
		
		List<Dependency> dependencies = new ArrayList<>();
		try
		{
			FileReader reader = new FileReader(pomPath);

			MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
			Model model = xpp3Reader.read(reader);
			
			DependencyManagement depMgmt = model.getDependencyManagement();
			if(depMgmt != null)
			{
				dependencies.addAll(depMgmt.getDependencies());
			}
			dependencies.addAll(model.getDependencies());
			
		}catch (Exception e) {
			System.out.println(e);
		}
		
		return dependencies;
	}
}
