package visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import common.dto.DependencyDTO;
import common.dto.VulnerabilityDTO;
import pom.DependencyOperations;
import vulnerability.VulnerabilityAnalyzer;

public class graphVisual {


	public static void generateGraphDOT(String newPomPath, String treeImgPath, String tempPomDependencyTreePath, Map<String, List<VulnerabilityDTO>> originalVulnerabilityMap)
	{
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(newPomPath+"\\tree.dot"))) {
            writer.write("");
//            System.out.println("DOT file created: " + newPomPath+"\\tree.dot");
        } catch (IOException e) {
//            System.err.println("Error writing DOT file: " + e.getMessage());
        }
		StringBuilder dotFormat = new StringBuilder();
        dotFormat.append("digraph G {\n");
        dotFormat.append("node [style=filled];\n");
        dotFormat.append(traverseDOT(tempPomDependencyTreePath, originalVulnerabilityMap));
        dotFormat.append("}");
        
//        System.out.println(dotFormat);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(newPomPath+"\\tree.dot"))) {
            writer.write(dotFormat.toString());
//            System.out.println("DOT file created: " + newPomPath+"\\tree.dot");
        } catch (IOException e) {
//            System.err.println("Error writing DOT file: " + e.getMessage());
        }
        
        String [] arr = treeImgPath.split("\\\\");
        System.out.println(arr[arr.length-1]);
        String treeName = arr[arr.length-1];
        runGraphviz("tree.dot", treeName, newPomPath);

	}
	
	public static String traverseDOT(String newPomDependencyTreePath, Map<String, List<VulnerabilityDTO>> vulnerabilityMap)
	{
		String dot = "";
//		DependencyOperations.generateDependencyTreeWithPath(newPomFilePath, newPomPath, newPomDependencyTreePath);
//        DependencyOperations.generateDependencyCheckReportWithPath(newPomFilePath, newPomPath, newPomDependencyCheckReportPath);
//		Map<String, List<VulnerabilityDTO>> vulnerabilityMap = VulnerabilityAnalyzer.getVulnerabilityList(newPomDependencyCheckReportPath);

		DependencyDTO root = DependencyOperations.getDependencyTree(newPomDependencyTreePath);
		List<String> outerDepList = new ArrayList<>();
		Queue<DependencyDTO> outerDepQueue = new LinkedList<>();

		for(DependencyDTO child : root.getChildren())
		{
			List<String> depList = new ArrayList<>();
	
			Queue<DependencyDTO> depQueue = new LinkedList<>();
			depList.add(child.getArtifactId());
			depQueue.offer(child);
			while(!depQueue.isEmpty())
			{
				DependencyDTO node = depQueue.poll();
				
				int nodeVulCount1 = 0;
				if(node.getChildren().size()==0)
				{
					boolean foundinVulMap = false;
					for (Map.Entry<String, List<VulnerabilityDTO>> entry : vulnerabilityMap.entrySet())
					{
						if(entry.getKey().equalsIgnoreCase(node.getArtifactId()))
						{
							nodeVulCount1 = entry.getValue().size();
							if(nodeVulCount1>0)
							{
//								System.out.println(node.getArtifactId()+" : "+nodeVulCount1);
								dot+="\""+node.getArtifactId()+" : "+nodeVulCount1+"\""+"[fillcolor=red];\n";
							}
							else
							{
								dot+="\""+node.getArtifactId()+" : "+nodeVulCount1+"\";\n";
							}
							foundinVulMap = true;
						}
					}
					if(foundinVulMap == false)
					{
						dot+="\""+node.getArtifactId()+" : "+nodeVulCount1+"\";\n";
					}
					
				}
				for(DependencyDTO nodeChild : node.getChildren())
				{
					if(!depList.contains(nodeChild.getArtifactId()))
					{
						int nodeVulCount = 0;
						int nodeChildVulCount = 0;
						for (Map.Entry<String, List<VulnerabilityDTO>> entry : vulnerabilityMap.entrySet())
						{
							if(entry.getKey().equalsIgnoreCase(node.getArtifactId()))
							{
								nodeVulCount = entry.getValue().size();
								if(nodeVulCount>0)
								{
//									System.out.println(node.getArtifactId()+" : "+nodeVulCount);
									dot+="\""+node.getArtifactId()+" : "+nodeVulCount+"\""+"[fillcolor=red];\n";
								}
							}
							if(entry.getKey().equalsIgnoreCase(nodeChild.getArtifactId()))
							{
								nodeChildVulCount = entry.getValue().size();
								if(nodeChildVulCount>0)
								{
//									System.out.println(nodeChild.getArtifactId()+" : "+nodeChildVulCount);
									dot+="\""+nodeChild.getArtifactId()+" : "+nodeChildVulCount+"\""+"[fillcolor=red];\n";
								}
							}
						}
						
						dot+="\""+node.getArtifactId()+" : "+nodeVulCount+"\""+" -> "+"\""+nodeChild.getArtifactId()+" : "+nodeChildVulCount+"\";\n";
						
						depList.add(nodeChild.getArtifactId());
						depQueue.offer(nodeChild);
					}
					if(!outerDepList.contains(nodeChild.getArtifactId()))
					{
						outerDepList.add(nodeChild.getArtifactId());
						outerDepQueue.offer(nodeChild);
					}
				}
			}
		}
		return dot;
	}
	
	
    private static void runGraphviz(String dotFileName, String outputFileName, String newPomPath) {
        try {
        	// Construct the command
            String[] command = {"C:\\Program Files\\Graphviz\\bin\\dot", "-Tpng", dotFileName, "-o", outputFileName};

//            System.out.println(command.toString());
            // Execute the command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(newPomPath)); // Set working directory
            Process process = processBuilder.start();

            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode == 0) {
//                System.out.println("Graph generated: " + outputFileName);
            } else {
//                System.err.println("Graphviz command failed with exit code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error running Graphviz command: " + e.getMessage());
        }
    }
}
