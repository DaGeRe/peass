package de.dagere.peass.measurement.rca;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;

public class PatternSetGenerator {

   private static final Logger LOG = LogManager.getLogger(PatternSetGenerator.class);

   private final FixedCommitConfig config;
   private final TestCase testcase;

   public PatternSetGenerator(FixedCommitConfig config, TestCase testcase) {
      this.config = config;
      this.testcase = testcase;
   }

   public Set<String> generatePatternSet(Set<CallTreeNode> includedNodes, final String version) {
      Set<String> includedPattern = new HashSet<>();
      if (config.getCommitOld().equals(version)) {
         includedNodes.forEach(node -> {
            LOG.trace(node);
            if (!node.getKiekerPattern().equals(CauseSearchData.ADDED)) {
               addIncludedPattern(node, node.getKiekerPattern(), includedPattern);
            }
         });
      } else {
         LOG.debug("Searching other: " + version);
         includedNodes.forEach(node -> {
            LOG.trace(node);
            if (!node.getOtherKiekerPattern().equals(CauseSearchData.ADDED)) {
               addIncludedPattern(node, node.getOtherKiekerPattern(), includedPattern);
            }
         });
      }
      return includedPattern;
   }

   private void addIncludedPattern(CallTreeNode node, String addPattern, Set<String> includedPattern) {
      if (node.getCall().equals(testcase.getExecutable()) && addPattern.startsWith("public ")) {
         includedPattern.add(addPattern.substring("public ".length()));
         includedPattern.add(addPattern);
      } else {
         includedPattern.add(addPattern);
      }
   }
}
