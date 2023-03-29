package de.dagere.peass.measurement.rca;

import java.util.HashSet;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffGenerator.data.TestMethodCall;
import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;

public class TestPatternSetGenerator {
   @Test
   public void generatePattern() {
      FixedCommitConfig config = new FixedCommitConfig();
      config.setCommit("000001");
      config.setCommitOld("000000");
      PatternSetGenerator generator = new PatternSetGenerator(config, new TestMethodCall("de.pack.Clazz", "myTest"));

      HashSet<CallTreeNode> includedNodes = new HashSet<>();
      includedNodes.add(new CallTreeNode("de.pack.Clazz#myTest", "public void de.pack.Clazz.myTest()", "public void de.pack.Clazz.myTest()", new MeasurementConfig(5)));
      includedNodes
            .add(new CallTreeNode("de.core.Clazz#myMethod", "public void de.core.Clazz.myMethod(int a)", "public void de.core.Clazz.myMethod(int a)", new MeasurementConfig(5)));

      Set<String> patternSet = generator.generatePatternSet(includedNodes, "000001");

      MatcherAssert.assertThat(patternSet, IsIterableContaining.hasItem("public void de.core.Clazz.myMethod(int a)"));
      MatcherAssert.assertThat(patternSet, IsIterableContaining.hasItem("void de.pack.Clazz.myTest()"));
      MatcherAssert.assertThat(patternSet, IsIterableContaining.hasItem("public void de.pack.Clazz.myTest()"));
   }
}
