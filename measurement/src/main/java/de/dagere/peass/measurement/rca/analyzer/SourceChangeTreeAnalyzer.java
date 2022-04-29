package de.dagere.peass.measurement.rca.analyzer;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.properties.MethodChangeReader;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;

public class SourceChangeTreeAnalyzer implements TreeAnalyzer {

   private static final Logger LOG = LogManager.getLogger(SourceChangeTreeAnalyzer.class);

   private final List<CallTreeNode> includedNodes = new LinkedList<>();
   private final MeasurementConfig config;

   public SourceChangeTreeAnalyzer(final CallTreeNode root, final CallTreeNode rootPredecessor, final File sourceFolder, final MeasurementConfig config) {
      // Only nodes with equal structure may have equal source
      List<CallTreeNode> includableNodes = new StructureChangeTreeAnalyzer(root, rootPredecessor).getMeasurementNodesPredecessor();
      this.config = config;
      if (sourceFolder == null || !sourceFolder.isDirectory()) {
         throw new RuntimeException("Source folder " + sourceFolder + " not found - source change analysis not possible!");
      }

      Set<CallTreeNode> includeNodes = calculateIncludedNodes(sourceFolder, includableNodes);
      includedNodes.addAll(includeNodes);
   }

   private Set<CallTreeNode> calculateIncludedNodes(final File sourceFolder, final List<CallTreeNode> includableNodes) {
      File methodSourceFolder = new File(sourceFolder, "methods");
      final Set<CallTreeNode> includeNodes = new HashSet<>();
      for (CallTreeNode node : includableNodes) {
         File mainSourceFile = MethodChangeReader.getMethodMainFile(methodSourceFolder, config.getExecutionConfig().getCommit(), node.toEntity());
         File oldSourceFile = MethodChangeReader.getMethodOldFile(methodSourceFolder, config.getExecutionConfig().getCommit(), node.toEntity());
         if (!mainSourceFile.exists() && !oldSourceFile.exists()) {
            File diffSourceFile = MethodChangeReader.getMethodDiffFile(methodSourceFolder, config.getExecutionConfig().getCommit(), node.toEntity());
            if (diffSourceFile.exists()) {
               LOG.trace("Node {} has no change", node);
            } else {
               LOG.error("Error - file {} did not exist", diffSourceFile);
            }
         } else {
            LOG.info("Node {} has change, so itself and all parents are included", node);
            addParents(node, includeNodes);
         }
      }
      LOG.debug("Identified {} included nodes", includableNodes.size());
      return includeNodes;
   }

   public void addParents(final CallTreeNode current, final Set<CallTreeNode> includeNodes) {
      includeNodes.add(current);
      if (current.getParent() != null) {
         addParents(current.getParent(), includeNodes);
      }
   }

   @Override
   public List<CallTreeNode> getMeasurementNodesPredecessor() {
      return includedNodes;
   }

}
