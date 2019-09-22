package de.peass.measurement.searchcause;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.testtransformation.JUnitTestTransformer;
import kieker.analysis.exception.AnalysisConfigurationException;

public class LevelMeasurer {
   
   private final CauseSearchFolders folders;
   private int adaptiveId = 0;
   private final CauseSearcherConfig causeSearcherConfig;
   private final JUnitTestTransformer testgenerator;
   private final MeasurementConfiguration measurementConfiguration;
   
   public LevelMeasurer(final CauseSearchFolders folders, final CauseSearcherConfig causeSearcherConfig, final JUnitTestTransformer testgenerator, final MeasurementConfiguration measurementConfiguration) {
      this.folders = folders;
      this.causeSearcherConfig = causeSearcherConfig;
      this.testgenerator = testgenerator;
      this.measurementConfiguration = measurementConfiguration;
   }

   public void measureVersion(final List<CallTreeNode> nodes)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final CauseTester executor = new CauseTester(folders, testgenerator, measurementConfiguration, causeSearcherConfig);
      final Set<CallTreeNode> includedNodes = prepareNodes(nodes);
      executor.setIncludedMethods(includedNodes);
      executor.evaluate(causeSearcherConfig.getTestCase());
      executor.getDurations(adaptiveId);
      executor.cleanup(adaptiveId);
      adaptiveId++;
   }

   private Set<CallTreeNode> prepareNodes(final List<CallTreeNode> nodes) {
      final Set<CallTreeNode> includedNodes = new HashSet<CallTreeNode>();
      includedNodes.addAll(nodes);
      nodes.forEach(node -> node.setVersions(measurementConfiguration.getVersion(), measurementConfiguration.getVersionOld()));
      return includedNodes;
   }
}
