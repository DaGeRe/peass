package de.peass.measurement.searchcause;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

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
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;
import kieker.analysis.exception.AnalysisConfigurationException;
import de.peass.measurement.MeasurementConfiguration;

public class CauseSearcher {

   private static final Logger LOG = LogManager.getLogger(CauseSearcher.class);

   // MethodNode node;

   private PeASSFolders folders;
   private String version, predecessor;
   private TestCase testcase;
   private final TestSet testset;

   private CallTreeNode rootPredecessor;
   private CallTreeNode rootVersion;
   private List<CallTreeNode> differingNodes = new LinkedList<CallTreeNode>();

   private File currentFolder, predecessorFolder;

   private final MeasurementConfiguration config;

   private int adaptiveId = 0;
   final JUnitTestTransformer testgenerator;

   private final CauseSearchData data;

   public static void main(String[] args)
         throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      File projectFolder = new File("../../projekte/commons-fileupload");
      String version = "4ed6e923cb2033272fcb993978d69e325990a5aa";
      TestCase test = new TestCase("org.apache.commons.fileupload.ServletFileUploadTest", "testFoldedHeaders");

      final JUnitTestTransformer testgenerator = new JUnitTestTransformer(projectFolder);
      testgenerator.setSumTime(300000);
      CauseSearcher searcher = new CauseSearcher(projectFolder, version, version + "~1", test, testgenerator, new MeasurementConfiguration(15, 5, 0.01, 0.01));
      searcher.search();
   }

   public CauseSearcher(File projectFolder, String version, String predecessor, TestCase testCase, final JUnitTestTransformer testgenerator, MeasurementConfiguration config)
         throws InterruptedException, IOException {
      data = new CauseSearchData(testCase, version, predecessor);
      this.testgenerator = testgenerator;
      this.config = config;
      this.folders = new PeASSFolders(projectFolder);
      this.version = version;
      this.predecessor = predecessor;
      this.testcase = testCase;
      testset = new TestSet(testcase);

      initFolders(version, predecessor);
   }

   private void initFolders(String version, String predecessor) throws InterruptedException, IOException {
      currentFolder = new File(folders.getTempProjectFolder(), version);
      GitUtils.clone(folders, currentFolder);
      GitUtils.goToTag(version, currentFolder);

      predecessorFolder = new File(folders.getTempProjectFolder(), predecessor);
      GitUtils.clone(folders, predecessorFolder);
      GitUtils.goToTag(predecessor, predecessorFolder);
   }

   public List<ChangedEntity> search()
         throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {

      getCallTree();

      FileUtils.deleteDirectory(currentFolder);
      FileUtils.deleteDirectory(predecessorFolder);
      initFolders(version, predecessor);

      return searchCause();
   }

   private List<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      List<ChangedEntity> changed = new LinkedList<>();
      CallTreeNode currentPredecessorNode = rootPredecessor;
      CallTreeNode currentVersionNode = rootVersion;

      isLevelDifferent(Arrays.asList(new CallTreeNode[] { currentPredecessorNode }),
            Arrays.asList(new CallTreeNode[] { currentVersionNode }));

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
         measureVersion(needToMeasurePredecessor, predecessorFolder);
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
      Relation relation = StatisticUtil.agnosticTTest(statisticsPredecessor, statisticsVersion, config.getType1error(), config.getType2error());
      LOG.debug("Relation: {}", relation);
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
            data.addDiff(predecessorNode);

         }
      }
   }

   private void measureVersion(List<CallTreeNode> nodes, File projectFolder)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      CauseTester executor = new CauseTester(folders, testgenerator, config, testcase);
      HashSet<CallTreeNode> includedNodes = new HashSet<CallTreeNode>();
      includedNodes.addAll(nodes);
      nodes.forEach(node -> node.setVersions(version, predecessor));
      executor.setIncludedMethods(includedNodes);
      executor.evaluate(version, predecessor, testcase);
      executor.getDurations(version, predecessor);
      File testcaseFolder = new File(folders.getDetailResultFolder(), testcase.getClazz());
      File adaptiveRunFolder = new File(folders.getDetailResultFolder(), "" + adaptiveId);
      testcaseFolder.renameTo(adaptiveRunFolder);
      adaptiveId++;
   }

   private void getCallTree() throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException {
      final TreeReader resultsManager = new TreeReader(predecessorFolder, config.getTimeout());
      rootPredecessor = getCallTree(testset, resultsManager, predecessor);

      final TreeReader resultsManagerPrevious = new TreeReader(currentFolder, config.getTimeout());
      rootVersion = getCallTree(testset, resultsManagerPrevious, version);
      LOG.info("Traces equal: {}", TreeUtil.areTracesEqual(rootPredecessor, rootVersion));
   }

   private CallTreeNode getCallTree(TestSet testset, final TreeReader resultsManager, final String githash)
         throws IOException, XmlPullParserException, InterruptedException, FileNotFoundException, ViewNotFoundException, AnalysisConfigurationException {
      resultsManager.getExecutor().loadClasses();
      resultsManager.executeKoPeMeKiekerRun(testset, githash);
      CallTreeNode root = resultsManager.getTree(testcase);
      return root;
   }
}
