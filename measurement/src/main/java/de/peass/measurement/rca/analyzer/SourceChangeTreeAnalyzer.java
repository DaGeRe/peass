package de.peass.measurement.rca.analyzer;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kieker.KiekerPatternConverter;

public class SourceChangeTreeAnalyzer implements TreeAnalyzer {

   private final List<CallTreeNode> includedNodes = new LinkedList<>();
   
   public SourceChangeTreeAnalyzer(final CallTreeNode root, final CallTreeNode rootPredecessor, File sourceFolder) {
      // Only nodes with equal structure may have equal source
      List<CallTreeNode> includableNodes = new StructureChangeTreeAnalyzer(root, rootPredecessor).getMeasurementNodesPredecessor();
      
      for (CallTreeNode node : includableNodes) {
         String fileNameStart = KiekerPatternConverter.getFileNameStart(node.getKiekerPattern());
         final File currentSourceFile = new File(sourceFolder, fileNameStart + "_main.txt");
         final File predecessorSourceFile = new File(sourceFolder, fileNameStart + "_predecessor.txt");
         if (!currentSourceFile.exists() && !predecessorSourceFile.exists()) {
            includedNodes.add(node);
         }
      }
      
   }
   
   
   @Override
   public List<CallTreeNode> getMeasurementNodesPredecessor() {
      // TODO Auto-generated method stub
      return includedNodes;
   }

}
