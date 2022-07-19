package de.dagere.peass.measurement.rca.analyzer;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.properties.ChangedMethodManager;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;

public class SourceChangeTreeAnalyzer implements TreeAnalyzer {

   private static final Logger LOG = LogManager.getLogger(SourceChangeTreeAnalyzer.class);

   private final List<CallTreeNode> includedNodes = new LinkedList<>();
   private final MeasurementConfig config;
   
   public SourceChangeTreeAnalyzer(final CallTreeNode root, final CallTreeNode rootPredecessor, final ChangedMethodManager manager, final MeasurementConfig config) {
      // Only nodes with equal structure may have equal source
      StructureChangeTreeAnalyzer structureChangeTreeAnalyzer = new StructureChangeTreeAnalyzer(root, rootPredecessor);
      List<CallTreeNode> includableNodes = structureChangeTreeAnalyzer.getMeasurementNodesPredecessor();
      this.config = config;
      
      Set<CallTreeNode> includeNodes = calculateIncludedNodesSourceChange(manager, includableNodes);
      calculateIncludeNodesStructureChange(structureChangeTreeAnalyzer, includeNodes);
      includedNodes.addAll(includeNodes);
   }

   private void calculateIncludeNodesStructureChange(StructureChangeTreeAnalyzer structureChangeTreeAnalyzer, Set<CallTreeNode> includeNodes) {
      List<CallTreeNode> unequalStructureNodesPredecessor = structureChangeTreeAnalyzer.getUnequalStructureNodesPredecessor();
      for (CallTreeNode unequalStructureNode : unequalStructureNodesPredecessor) {
         LOG.info("Node {} has structure change, so itself and all parents are included", unequalStructureNode);
         addParents(unequalStructureNode, includeNodes);
      }
   }

   private Set<CallTreeNode> calculateIncludedNodesSourceChange(final ChangedMethodManager manager, final List<CallTreeNode> includableNodes) {
      final Set<CallTreeNode> includeNodes = new HashSet<>();
      for (CallTreeNode node : includableNodes) {
         File mainSourceFile = manager.getMethodMainFile(config.getFixedCommitConfig().getCommit(), node.toEntity());
         File oldSourceFile = manager.getMethodOldFile(config.getFixedCommitConfig().getCommit(), node.toEntity());
         if (!mainSourceFile.exists() && !oldSourceFile.exists()) {
            File diffSourceFile = manager.getMethodDiffFile(config.getFixedCommitConfig().getCommit(), node.toEntity());
            if (diffSourceFile.exists()) {
               LOG.debug("Node {} has no change", node);
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
