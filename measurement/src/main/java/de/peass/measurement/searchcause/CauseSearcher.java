package de.peass.measurement.searchcause;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.organize.FolderDeterminer;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.data.CauseSearchData;
import de.peass.measurement.searchcause.kieker.BothTreeReader;
import de.peass.measurement.searchcause.treeanalysis.LevelDifferingDeterminer;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.Constants;
import kieker.analysis.exception.AnalysisConfigurationException;

public class CauseSearcher {

   private static final Logger LOG = LogManager.getLogger(CauseSearcher.class);

   // Basic config
   protected final PeASSFolders folders;
   protected final CauseSearcherConfig causeSearchConfig;
   protected final MeasurementConfiguration measurementConfig;

   // Classes doing the real work
   protected final BothTreeReader reader;
   protected final LevelMeasurer measurer;

   // Result
   protected List<CallTreeNode> differingNodes = new LinkedList<CallTreeNode>();
   protected final CauseSearchData data;
   private final File treeDataFile;

   public static void main(final String[] args)
         throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final File projectFolder = new File("../../projekte/commons-fileupload");
      final String version = "4ed6e923cb2033272fcb993978d69e325990a5aa";
      final TestCase test = new TestCase("org.apache.commons.fileupload.ServletFileUploadTest", "testFoldedHeaders");

      final JUnitTestTransformer testtransformer = new JUnitTestTransformer(projectFolder);
      testtransformer.setSumTime(300000);
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(version, version + "~1", test);
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(15, 5, 0.01, 0.01);
      final PeASSFolders folders2 = new PeASSFolders(projectFolder);
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, folders2);
      final LevelMeasurer measurer = new LevelMeasurer(folders2, causeSearcherConfig, testtransformer, measurementConfiguration);
      final CauseSearcher searcher = new CauseSearcher(reader, causeSearcherConfig, measurer, measurementConfiguration, folders2);
      searcher.search();
   }

   public CauseSearcher(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final LevelMeasurer measurer, final MeasurementConfiguration measurementConfig, final PeASSFolders folders)
         throws InterruptedException, IOException {
      data = new CauseSearchData(causeSearchConfig.getTestCase(), causeSearchConfig.getVersion(), causeSearchConfig.getPredecessor(), measurementConfig);
      this.reader = reader;
      this.measurer = measurer;
      this.measurementConfig = measurementConfig;
      this.folders = folders;
      this.causeSearchConfig = causeSearchConfig;
      
      treeDataFile = new File(folders.getFullMeasurementFolder(), causeSearchConfig.getVersion() + File.separator + 
            causeSearchConfig.getTestCase().getShortClazz() + File.separator 
            + causeSearchConfig.getTestCase().getMethod() + ".json");
      if (treeDataFile.getParentFile().exists()) {
         throw new RuntimeException("Old tree data folder " + treeDataFile.getAbsolutePath() + " exists - please cleanup!");
      }
      treeDataFile.getParentFile().mkdirs();
      
      final File potentialOldFolder = new File(folders.getDetailResultFolder(causeSearchConfig.getVersion(), causeSearchConfig.getTestCase()), "0");
      if (potentialOldFolder.exists()) {
         throw new RuntimeException("Old measurement folder " + potentialOldFolder.getAbsolutePath() + " exists - please cleanup!");
      }
      new FolderDeterminer(folders).testResultFolders(causeSearchConfig.getVersion(), causeSearchConfig.getPredecessor(), causeSearchConfig.getTestCase());
   }

   public List<ChangedEntity> search()
         throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      reader.readTrees();

      LOG.info("Tree size: {}", reader.getRootPredecessor().getTreeSize());
      
      return searchCause();
   }

   protected List<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      isLevelDifferent(Arrays.asList(new CallTreeNode[] { reader.getRootPredecessor() }),
            Arrays.asList(new CallTreeNode[] { reader.getRootVersion() }));

      writeTreeState();
      return convertToChangedEntitites();
   }

   protected List<ChangedEntity> convertToChangedEntitites() {
      final List<ChangedEntity> changed = new LinkedList<>();
      differingNodes.forEach(node -> changed.add(node.toEntity()));
      return changed;
   }

   protected void writeTreeState() throws IOException, JsonGenerationException, JsonMappingException {
      Constants.OBJECTMAPPER.writeValue(treeDataFile, data);
   }

   private void isLevelDifferent(final List<CallTreeNode> currentPredecessorNodeList, final List<CallTreeNode> currentVersionNodeList)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final LevelDifferingDeterminer levelSearcher = new LevelDifferingDeterminer(currentPredecessorNodeList, currentVersionNodeList, causeSearchConfig, measurementConfig);

      final List<CallTreeNode> needToMeasurePredecessor = levelSearcher.getNeedToMeasurePredecessor();
      final List<CallTreeNode> needToMeasureCurrent = levelSearcher.getNeedToMeasureCurrent();

      if (needToMeasureCurrent.size() > 0 && needToMeasurePredecessor.size() > 0) {
         measurer.measureVersion(needToMeasurePredecessor);
         levelSearcher.calculateDiffering();

         for (final CallTreeNode predecessorNode : needToMeasurePredecessor) {
            data.addDiff(predecessorNode);
            levelSearcher.analyseNode(predecessorNode);
         }

         differingNodes.addAll(levelSearcher.getTreeStructureDifferingNodes());
         differingNodes.addAll(levelSearcher.getMeasurementDiffering());

         writeTreeState();

         isLevelDifferent(levelSearcher.getDifferingPredecessor(), levelSearcher.getDifferingPredecessor());
      }
   }

}
