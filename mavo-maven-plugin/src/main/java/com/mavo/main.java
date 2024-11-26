package com.mavo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import algorithm.POMAnalyzerAlgorithm;
import common.Constants;
import common.Utils;
import common.dto.DependencyDTO;
import common.dto.VulnerabilityDTO;
import pom.DependencyOperations;
import pom.PomCreations;
import visualization.graphVisual;
import vulnerability.VulnerabilityAnalyzer;

public class main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String originalPomPath = "D:\\Anurag\\Bits Testing Project\\Parent";
		boolean excludeParentDependencies = true;
		boolean groupRecommendations = false;
		boolean optimizeRecommendations = false;
		String patchRestriction = "major";
		
//        POMAnalyzerAlgorithm.pomAnalyzerAlgorithm(originalPomPath, excludeParentDependencies, 
//        		groupRecommendations, optimizeRecommendations, patchRestriction);
        
//		String newPomFilePath = originalPomPath+Constants.tempPomFileDirectoryName+Constants.tempPomFileName;
//		String newPomPath = originalPomPath+Constants.tempPomFileDirectoryName;
//		String newPomDependencyCheckReportPath = newPomPath+Constants.dependencyCheckReportName;
//		String newPomDependencyTreePath = newPomPath+Constants.dependencyTreeName;
//		
//        graphVisual.generateGraphDOT(newPomFilePath, newPomPath, newPomDependencyTreePath, newPomDependencyCheckReportPath);
		
//		PomCreations.createRecommendedPOM(originalPomPath);
		
//		Utils.combinations(3);
//		Utils.combinations(4);
//		Utils.combinations(5);
//		Utils.combinations(6);
//		Utils.combinations(7);
//		Utils.combinations(8);
//		Utils.combinations(9);
//		Utils.combinations(10);
        
        
//		String newPomFilePath = originalPomPath+Constants.tempPomFileDirectoryName+Constants.tempPomFileName;
//		String newPomPath = originalPomPath+Constants.tempPomFileDirectoryName;
//		String newPomDependencyCheckReportPath = newPomPath+Constants.dependencyCheckReportName;
//		String newPomDependencyTreePath = newPomPath+Constants.dependencyTreeName;
//		String treeImgPath = newPomPath+Constants.treeImgName;
//        
//		String dot = "";
//		DependencyOperations.generateDependencyTreeWithPath(newPomFilePath, newPomPath, newPomDependencyTreePath);
//        DependencyOperations.generateDependencyCheckReportWithPath(newPomFilePath, newPomPath, newPomDependencyCheckReportPath);
//		Map<String, List<VulnerabilityDTO>> vulnerabilityMap = VulnerabilityAnalyzer.getVulnerabilityList(newPomDependencyCheckReportPath);
//
//		DependencyDTO root = DependencyOperations.getDependencyTree(newPomDependencyTreePath);
//		List<String> outerDepList = new ArrayList<>();
//		Queue<DependencyDTO> outerDepQueue = new LinkedList<>();
//
//		for(DependencyDTO child : root.getChildren())
//		{
//			List<String> depList = new ArrayList<>();
//	
//			Queue<DependencyDTO> depQueue = new LinkedList<>();
//			depList.add(child.getArtifactId());
//			depQueue.offer(child);
//			while(!depQueue.isEmpty())
//			{
//				DependencyDTO node = depQueue.poll();
//				
//				int nodeVulCount1 = 0;
//				for (Map.Entry<String, List<VulnerabilityDTO>> entry : vulnerabilityMap.entrySet())
//				{
//					if(entry.getKey().equalsIgnoreCase(node.getArtifactId()))
//					{
//						nodeVulCount1 = entry.getValue().size();
//						if(nodeVulCount1>0)
//						{
//							System.out.println(node.getArtifactId()+" : "+nodeVulCount1);
//							dot+="\""+node.getArtifactId()+" : "+nodeVulCount1+"\""+"[fillcolor=red];\n";
//						}
//					}
//				}
//				for(DependencyDTO nodeChild : node.getChildren())
//				{
//					if(!depList.contains(nodeChild.getArtifactId()))
//					{
//						int nodeVulCount = 0;
//						int nodeChildVulCount = 0;
//						for (Map.Entry<String, List<VulnerabilityDTO>> entry : vulnerabilityMap.entrySet())
//						{
//							if(entry.getKey().equalsIgnoreCase(nodeChild.getArtifactId()))
//							{
//								nodeChildVulCount = entry.getValue().size();
//								if(nodeChildVulCount>0)
//								{
//									System.out.println(nodeChild.getArtifactId()+" : "+nodeChildVulCount);
//									dot+="\""+nodeChild.getArtifactId()+" : "+nodeChildVulCount+"\""+"[fillcolor=red];\n";
//								}
//							}
//						}
//						
//						dot+="\""+node.getArtifactId()+" : "+nodeVulCount+"\""+" -> "+"\""+nodeChild.getArtifactId()+" : "+nodeChildVulCount+"\";\n";
//						
//						depList.add(nodeChild.getArtifactId());
//						depQueue.offer(nodeChild);
//					}
//					if(!outerDepList.contains(nodeChild.getArtifactId()))
//					{
//						outerDepList.add(nodeChild.getArtifactId());
//						outerDepQueue.offer(nodeChild);
//					}
//				}					
//			}
//		}
//		
//		System.out.println(dot);

	}

}
