package de.peass.measurement.rca;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.searcher.CauseSearcher;
import de.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.peass.testtransformation.JUnitTestTransformer;
import kieker.analysis.exception.AnalysisConfigurationException;

public class LevelCauseSearchExperimentalStarter {
   
   private static final Logger LOG = LogManager.getLogger(LevelCauseSearchExperimentalStarter.class);
   
   public static void main(final String[] args)
         throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final File projectFolder = new File("../../projekte/commons-fileupload");
      final String version = "4ed6e923cb2033272fcb993978d69e325990a5aa";
      final TestCase test = new TestCase("org.apache.commons.fileupload.ServletFileUploadTest", "testFoldedHeaders");

      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(15, 5, 0.01, 0.01, true, version, version + "~1");
      measurementConfiguration.setUseKieker(true);
      final JUnitTestTransformer testtransformer = new JUnitTestTransformer(projectFolder, measurementConfiguration);
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(test, true, false, 5.0, false, 0.1, false, false, RCAStrategy.COMPLETE);
      final CauseSearchFolders folders2 = new CauseSearchFolders(projectFolder);
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, folders2);

      final CauseTester measurer = new CauseTester(folders2, testtransformer, causeSearcherConfig);
      final LevelCauseSearcher searcher = new LevelCauseSearcher(reader, causeSearcherConfig, measurer, measurementConfiguration, folders2);
      reader.readTrees();

      List<CallTreeNode> predecessor = Arrays.asList(new CallTreeNode[] { reader.getRootPredecessor() });
      List<CallTreeNode> current = Arrays.asList(new CallTreeNode[] { reader.getRootVersion() });

      int level = 0;
      boolean hasChilds = true;
      while (hasChilds) {
         level++;
         LOG.info("Level: " + level + " " + predecessor.get(0).getKiekerPattern());
         boolean foundNodeLevel = false;
         final List<CallTreeNode> predecessorNew = new LinkedList<>();
         final List<CallTreeNode> currentNew = new LinkedList<>();

         final Iterator<CallTreeNode> currentIterator = current.iterator();

         for (final Iterator<CallTreeNode> preIterator = predecessor.iterator(); preIterator.hasNext() && currentIterator.hasNext();) {
            final CallTreeNode predecessorChild = preIterator.next();
            final CallTreeNode currentChild = currentIterator.next();
            predecessorNew.addAll(predecessorChild.getChildren());
            currentNew.addAll(currentChild.getChildren());
            final String searchedCall = "public static long org.apache.commons.fileupload.util.Streams.copy(java.io.InputStream,java.io.OutputStream,boolean,byte[])";
            if (predecessorChild.getKiekerPattern().equals(searchedCall) && currentChild.getKiekerPattern().equals(searchedCall)) {
               foundNodeLevel = true;
            }
            if (predecessorChild.getKiekerPattern().equals(searchedCall) != currentChild.getKiekerPattern().equals(searchedCall)) {
               LOG.info(predecessorChild.getKiekerPattern());
               LOG.info(currentChild.getKiekerPattern());
               throw new RuntimeException("Tree structure differs above searched node!");
            }
         }
         if (foundNodeLevel) {
            LOG.info("Found!");
            searcher.isLevelDifferent(predecessorNew, currentNew);
         }
         predecessor = predecessorNew;
         current = currentNew;
         if (predecessor.isEmpty()) {
            hasChilds = false;
         }
      }
   }
}
