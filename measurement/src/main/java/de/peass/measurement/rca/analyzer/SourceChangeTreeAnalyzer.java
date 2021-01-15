package de.peass.measurement.rca.analyzer;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kieker.KiekerPatternConverter;

public class SourceChangeTreeAnalyzer implements TreeAnalyzer {

   private static final Logger LOG = LogManager.getLogger(SourceChangeTreeAnalyzer.class);

   private final List<CallTreeNode> includedNodes = new LinkedList<>();
   private final MeasurementConfiguration config;

   public SourceChangeTreeAnalyzer(final CallTreeNode root, final CallTreeNode rootPredecessor, File sourceFolder, MeasurementConfiguration config) {
      // Only nodes with equal structure may have equal source
      List<CallTreeNode> includableNodes = new StructureChangeTreeAnalyzer(root, rootPredecessor).getMeasurementNodesPredecessor();
      this.config = config;

      Set<CallTreeNode> includeNodes = calculateIncludedNodes(sourceFolder, includableNodes);
      includedNodes.addAll(includeNodes);
   }

   private Set<CallTreeNode> calculateIncludedNodes(File sourceFolder, List<CallTreeNode> includableNodes) {
      Set<CallTreeNode> includeNodes = new HashSet<>();
      LOG.debug("Searching in {}", sourceFolder);
      File parentFolder = null;
      for (File candidate : new File(sourceFolder, "methods").listFiles()) {
         if (candidate.getName().equals(config.getVersion())) {
            parentFolder = candidate;
         }
      }
      if (parentFolder == null) {
         throw new RuntimeException("Did not find property folder for " + config.getVersion() + " in " + sourceFolder.getAbsolutePath());
      }
      for (CallTreeNode node : includableNodes) {
         String fileNameStart = KiekerPatternConverter.getFileNameStart(node.getKiekerPattern());
         final File currentSourceFile = new File(parentFolder, fileNameStart + "_main.txt");
         final File predecessorSourceFile = new File(parentFolder, fileNameStart + "_predecessor.txt");
         if (!currentSourceFile.exists() && !predecessorSourceFile.exists()) {
            final File currentDiffFile = new File(parentFolder, fileNameStart + "_diff.txt");
            if (currentDiffFile.exists()) {
               LOG.trace("Node {} has no change", node);
            } else {
               LOG.error("Error - file {} did not exist", currentDiffFile);
            }
         } else {
            LOG.info("Node {} has change, so itself and all parents are included", node);
            addParents(node, includeNodes);
         }
      }
      LOG.debug("Identified {} included nodes", includableNodes.size());
      return includeNodes;
   }

   public void addParents(CallTreeNode current, Set<CallTreeNode> includeNodes) {
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
