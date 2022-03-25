package de.dagere.peass.measurement.rca.strategytest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseSearcherConfigMixin;
import de.dagere.peass.measurement.rca.analyzer.SourceChangeTreeAnalyzer;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import kieker.analysis.exception.AnalysisConfigurationException;

public class TestSourceChangeTreeAnalyzer {
   @Test
   public void testNodeSelection() throws InterruptedException, IOException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException {
      final MeasurementConfig config = new MeasurementConfig(15, "fd2c8ddf3fa52973ee54c4db87b47bb587886200", "fd2c8ddf3fa52973ee54c4db87b47bb587886200~1");
      BothTreeReader treeReader = new BothTreeReader(new CauseSearcherConfig(new TestCase("de.peass.MainTest#testMe"), new CauseSearcherConfigMixin()),
            config,
            new CauseSearchFolders(new File("src/test/resources/sourceChangeRCATest/project_3")), 
            new EnvironmentVariables());
      treeReader.readTrees();

      SourceChangeTreeAnalyzer analyzer = new SourceChangeTreeAnalyzer(treeReader.getRootVersion(), treeReader.getRootPredecessor(),
            new File("src/test/resources/sourceChangeRCATest/properties_project_3"), config);

      List<String> instrumentedCalls = analyzer.getMeasurementNodesPredecessor().stream().map(node -> node.getCall()).collect(Collectors.toList());
      MatcherAssert.assertThat(instrumentedCalls, IsIterableContaining.hasItem("de.peass.MainTest#testMe"));
      MatcherAssert.assertThat(instrumentedCalls, IsIterableContaining.hasItem("de.peass.C0_0#method0"));
      MatcherAssert.assertThat(instrumentedCalls, IsIterableContaining.hasItem("de.peass.C1_0#method0"));

      MatcherAssert.assertThat(instrumentedCalls, Matchers.not(IsIterableContaining.hasItem("de.peass.AddRandomNumbers#addSomething")));
      MatcherAssert.assertThat(instrumentedCalls, Matchers.not(IsIterableContaining.hasItem("de.peass.C1_0#method1")));
   }
}
