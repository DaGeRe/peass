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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.organize.FolderDeterminer;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.treeanalysis.LevelDifferentNodeDeterminer;
import de.peass.testtransformation.JUnitTestTransformer;
import kieker.analysis.exception.AnalysisConfigurationException;

public class CauseSearcher {

   private static final Logger LOG = LogManager.getLogger(CauseSearcher.class);

   // Basic config
   protected final CauseSearchFolders folders;
   protected final CauseSearcherConfig causeSearchConfig;
   protected final MeasurementConfiguration measurementConfig;

   // Classes doing the real work
   protected final BothTreeReader reader;
   protected final CauseTester measurer;

   // Result
   protected List<CallTreeNode> differingNodes = new LinkedList<>();
   protected CausePersistenceManager persistenceManager;

   public static void main(final String[] args)
         throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final File projectFolder = new File("../../projekte/commons-fileupload");
      final String version = "4ed6e923cb2033272fcb993978d69e325990a5aa";
      final TestCase test = new TestCase("org.apache.commons.fileupload.ServletFileUploadTest", "testFoldedHeaders");

      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(15 * 1000 * 60, 5, 0.01, 0.01, true, version, version + "~1");
      measurementConfiguration.setUseKieker(true);
      final JUnitTestTransformer testtransformer = new JUnitTestTransformer(projectFolder, measurementConfiguration);
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(test, true, false, 5.0, false, 0.1, false, false);
      final CauseSearchFolders folders2 = new CauseSearchFolders(projectFolder);
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, folders2);

      final CauseTester measurer = new CauseTester(folders2, testtransformer, causeSearcherConfig);
      final CauseSearcher searcher = new CauseSearcher(reader, causeSearcherConfig, measurer, measurementConfiguration, folders2);
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

   public CauseSearcher(final CauseTester measurer, final CauseSearchData finishedData, final CauseSearchFolders folders) {
      reader = null;
      this.measurer = measurer;
      this.measurementConfig = finishedData.getMeasurementConfig();
      this.causeSearchConfig = finishedData.getCauseConfig();
      this.folders = folders;
      persistenceManager = new CausePersistenceManager(finishedData, folders);
   }

   public CauseSearcher(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final CauseTester measurer, final MeasurementConfiguration measurementConfig,
         final CauseSearchFolders folders)
         throws InterruptedException, IOException {
      this.reader = reader;
      this.measurer = measurer;
      this.measurementConfig = measurementConfig;
      this.folders = folders;
      this.causeSearchConfig = causeSearchConfig;
      persistenceManager = new CausePersistenceManager(causeSearchConfig, measurementConfig, folders);

      final File potentialOldFolder = new File(folders.getArchiveResultFolder(measurementConfig.getVersion(), causeSearchConfig.getTestCase()), "0");
      if (potentialOldFolder.exists()) {
         throw new RuntimeException("Old measurement folder " + potentialOldFolder.getAbsolutePath() + " exists - please cleanup!");
      }
      new FolderDeterminer(folders).testResultFolders(measurementConfig.getVersion(), measurementConfig.getVersionOld(), causeSearchConfig.getTestCase());
   }

   public List<ChangedEntity> search()
         throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      reader.readTrees();

      LOG.info("Tree size: {}", reader.getRootPredecessor().getTreeSize());

      return searchCause();
   }

   protected List<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      reader.getRootPredecessor().setOtherVersionNode(reader.getRootVersion());
      reader.getRootVersion().setOtherVersionNode(reader.getRootPredecessor());
      isLevelDifferent(Arrays.asList(new CallTreeNode[] { reader.getRootPredecessor() }),
            Arrays.asList(new CallTreeNode[] { reader.getRootVersion() }));

      return convertToChangedEntitites();
   }

   protected List<ChangedEntity> convertToChangedEntitites() {
      final List<ChangedEntity> changed = new LinkedList<>();
      differingNodes.forEach(node -> changed.add(node.toEntity()));
      return changed;
   }

   protected void writeTreeState() throws IOException, JsonGenerationException, JsonMappingException {
      persistenceManager.writeTreeState();
   }

   public void isLevelDifferent(final List<CallTreeNode> currentPredecessorNodeList, final List<CallTreeNode> currentVersionNodeList)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final LevelDifferentNodeDeterminer levelSearcher = new LevelDifferentNodeDeterminer(currentPredecessorNodeList, currentVersionNodeList, causeSearchConfig, measurementConfig);

      final List<CallTreeNode> measurePredecessor = levelSearcher.getMeasurePredecessor();

      if (measurePredecessor.size() > 0) {
         analyseLevel(levelSearcher, measurePredecessor);
         writeTreeState();

         isLevelDifferent(levelSearcher.getMeasureNextLevelPredecessor(), levelSearcher.getMeasureNextLevel());
      }
   }

   private void analyseLevel(final LevelDifferentNodeDeterminer levelSearcher, final List<CallTreeNode> measuredPredecessor)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      measurer.measureVersion(measuredPredecessor);
      levelSearcher.calculateDiffering();

      for (final CallTreeNode predecessorNode : measuredPredecessor) {
         persistenceManager.addMeasurement(predecessorNode);
      }

      differingNodes.addAll(levelSearcher.getTreeStructureDifferingNodes());
      differingNodes.addAll(levelSearcher.getCurrentLevelDifferent());
   }

   public CauseSearchData getRCAData() {
      // TODO Auto-generated method stub
      return persistenceManager.getRCAData();
   }

}
