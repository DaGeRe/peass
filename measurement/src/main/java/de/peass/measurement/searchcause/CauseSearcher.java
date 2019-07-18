package de.peass.measurement.searchcause;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.execution.MavenTestExecutor;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.StatisticUtil;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.data.CauseSearchData;
import de.peass.measurement.searchcause.kieker.TreeReader;
import de.peass.measurement.searchcause.kieker.TreeReaderFactory;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;
import kieker.analysis.exception.AnalysisConfigurationException;
import de.peass.measurement.MeasurementConfiguration;

public class CauseSearcher {

   private static final Logger LOG = LogManager.getLogger(CauseSearcher.class);

   // Basic config
   private final PeASSFolders folders;
   private final String version, predecessor;
   private final TestCase testcase;
   
   // Measurement Config
   private final MeasurementConfiguration config;

   // Tree
   private CallTreeNode rootPredecessor;
   private CallTreeNode rootVersion;
   
   // Result
   private List<CallTreeNode> differingNodes = new LinkedList<CallTreeNode>();
   private final CauseSearchData data;

   private int adaptiveId = 0;
   private final JUnitTestTransformer testtransformer;

   public static void main(String[] args)
         throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      File projectFolder = new File("../../projekte/commons-fileupload");
      String version = "4ed6e923cb2033272fcb993978d69e325990a5aa";
      TestCase test = new TestCase("org.apache.commons.fileupload.ServletFileUploadTest", "testFoldedHeaders");

      final JUnitTestTransformer testgenerator = new JUnitTestTransformer(projectFolder);
      testgenerator.setSumTime(300000);
      CauseSearcher searcher = new CauseSearcher(new CauseSearcherConfig(projectFolder, version, version + "~1", test), testgenerator, new MeasurementConfiguration(15, 5, 0.01, 0.01));
      searcher.search();
   }

   public CauseSearcher(CauseSearcherConfig causeSearchConfig, final JUnitTestTransformer testgenerator, MeasurementConfiguration config)
         throws InterruptedException, IOException {
      data = new CauseSearchData(causeSearchConfig.testCase, causeSearchConfig.version, causeSearchConfig.predecessor, config);
      this.testtransformer = testgenerator;
      this.config = config;
      this.folders = new PeASSFolders(causeSearchConfig.projectFolder);
      this.version = causeSearchConfig.version;
      this.predecessor = causeSearchConfig.predecessor;
      this.testcase = causeSearchConfig.testCase;
   }

   public List<ChangedEntity> search()
         throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      getCallTree();

      return searchCause();
   }

   private List<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      isLevelDifferent(Arrays.asList(new CallTreeNode[] { rootPredecessor }),
            Arrays.asList(new CallTreeNode[] { rootVersion }));

      final List<ChangedEntity> changed = new LinkedList<>();
      differingNodes.forEach(node -> changed.add(node.toEntity()));

      writeTreeState();
      return changed;
   }

   private void writeTreeState() throws IOException, JsonGenerationException, JsonMappingException {
      File treeData = new File(folders.getFullMeasurementFolder(), testcase.getShortClazz() + "#" + testcase.getMethod() + "_tree.json");
      Constants.OBJECTMAPPER.writeValue(treeData, data);
   }

   private void isLevelDifferent(List<CallTreeNode> currentPredecessorNodeList, List<CallTreeNode> currentVersionNodeList)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final LevelCauseSearcher levelSearcher = new LevelCauseSearcher(currentPredecessorNodeList, currentVersionNodeList);

      final List<CallTreeNode> needToMeasurePredecessor = levelSearcher.getNeedToMeasurePredecessor();
      final List<CallTreeNode> needToMeasureCurrent = levelSearcher.getNeedToMeasureCurrent();

      if (needToMeasureCurrent.size() > 0 && needToMeasurePredecessor.size() > 0) {
         measureVersion(needToMeasurePredecessor);
         levelSearcher.calculateDiffering(predecessor, version);

         differingNodes.addAll(levelSearcher.getTreeStructureDifferingNodes());

         for (CallTreeNode predecessorNode : needToMeasurePredecessor) {
            analyseNode(levelSearcher, predecessorNode);
         }
         writeTreeState();
         
         isLevelDifferent(levelSearcher.getDifferingPredecessor(), levelSearcher.getDifferingPredecessor());
      }
   }

   private void analyseNode(LevelCauseSearcher levelSearcher, CallTreeNode predecessorNode) {
      final DescriptiveStatistics statisticsPredecessor = predecessorNode.getStatistics(predecessor);
      final DescriptiveStatistics statisticsVersion = predecessorNode.getStatistics(version);
      final Relation relation = StatisticUtil.agnosticTTest(statisticsPredecessor, statisticsVersion, config.getType1error(), config.getType2error());
      LOG.debug("Relation: {}", relation);
      data.addDiff(predecessorNode);
      if (relation.equals(Relation.UNEQUAL)) {
         boolean anyChildReMeasure = false;
         for (CallTreeNode testChild : predecessorNode.getChildren()) {
            LOG.debug("Testing: " + testChild + " " + levelSearcher.getDifferingPredecessor());
            if (levelSearcher.getDifferingPredecessor().contains(testChild)) {
               anyChildReMeasure = true;
            }
         }
         
         if (!anyChildReMeasure) {
            LOG.debug("Adding {} - no childs needs to be remeasured, T={}", predecessorNode, TestUtils.t(statisticsPredecessor, statisticsVersion));
            LOG.debug("Childs: {}", predecessorNode.getChildren());
            differingNodes.add(predecessorNode);
         }
      }
   }

   private void measureVersion(final List<CallTreeNode> nodes)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final CauseTester executor = new CauseTester(folders, testtransformer, config, testcase);
      final Set<CallTreeNode> includedNodes = new HashSet<CallTreeNode>();
      includedNodes.addAll(nodes);
      nodes.forEach(node -> node.setVersions(version, predecessor));
      executor.setIncludedMethods(includedNodes);
      executor.evaluate(version, predecessor, testcase);
      executor.getDurations(version, predecessor, adaptiveId);
      adaptiveId++;
   }

   private void getCallTree() throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException {
      final TreeReader resultsManager = TreeReaderFactory.createTreeReader(folders, predecessor, config.getTimeout());
      rootPredecessor = resultsManager.getTree(testcase, predecessor);

      final TreeReader resultsManagerPrevious = TreeReaderFactory.createTreeReader(folders, version, config.getTimeout());
      rootVersion = resultsManagerPrevious.getTree(testcase, version);
      LOG.info("Traces equal: {}", TreeUtil.areTracesEqual(rootPredecessor, rootVersion));
   }
}
