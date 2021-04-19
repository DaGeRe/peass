package de.peass.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.analysis.all.RepoFolders;
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.utils.Constants;
import de.peass.visualization.html.HTMLWriter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Visualizes the root-cause-analysis-tree as HTML page", name = "visualizerca")
public class VisualizeRCA implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(VisualizeRCA.class);

   private static final ObjectMapper MAPPER = new ObjectMapper();
   static {
      MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
   }

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   protected File data[];

   @Option(names = { "-visualizeFull", "--visualizeFull" }, description = "Whether to visualize full tree")
   protected boolean visualizeFull;

   @Option(names = { "-propertyFolder", "--propertyFolder" }, description = "Path to property folder", required = false)
   protected File propertyFolder;

   @Option(names = { "-out", "--out" }, description = "Path for storage of results, default results", required = false)
   protected File resultFolder = new File("results");

   // TODO Fix dirty hack
   final String projectName = "commons-fileupload";

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final CommandLine commandLine = new CommandLine(new VisualizeRCA());
      System.exit(commandLine.execute(args));
   }

   public VisualizeRCA() {
   }

   public VisualizeRCA(final File[] data, final File resultFolder) {
      this.data = data;
      this.resultFolder = resultFolder;
   }

   @Override
   public Void call() throws Exception {
      if (!resultFolder.exists()) {
         resultFolder.mkdir();
      }

      List<File> rcaFolderToHandle = new RCAFolderSearcher(data).searchRCAFiles();
      for (File rcaFolder : rcaFolderToHandle) {
         analyzeFile(resultFolder, rcaFolder);
      }
      List<File> peassFolderToHandle = new RCAFolderSearcher(data).searchPeassFiles();
      for (File peassFolder : peassFolderToHandle) {
         analyzeFile(peassFolder);
      }

      return null;
   }

   private void analyzeFile(final File peassFolder) throws JAXBException, JsonProcessingException, FileNotFoundException, IOException {
      PeASSFolders folders = new PeASSFolders(peassFolder);
      for (File kopemeFile : folders.getFullMeasurementFolder().listFiles((FilenameFilter) new WildcardFileFilter("*xml"))) {
         Kopemedata data = XMLDataLoader.loadData(kopemeFile);
         for (TestcaseType test : data.getTestcases().getTestcase()) {
            for (Chunk chunk : test.getDatacollector().get(0).getChunk()) {
               List<String> versions = getVersions(chunk);
               TestCase testcase = new TestCase(data.getTestcases().getClazz(), test.getName());
               KoPeMeTreeConverter koPeMeTreeConverter = new KoPeMeTreeConverter(folders, versions.get(0), versions.get(1), testcase);
               GraphNode node = koPeMeTreeConverter.getData();
               File destFolder = new File(resultFolder, versions.get(0));
               GraphNode emptyNode = new GraphNode(testcase.getExecutable(), "void " + testcase.getExecutable().replace("#", ".") + "()", CauseSearchData.ADDED);
               emptyNode.setName(testcase.getExecutable());
               CauseSearchData data2 = new CauseSearchData();
               data2.setCauseConfig(new CauseSearcherConfig(testcase, false, false, 1.0, false, false, null));
               data2.setConfig(new MeasurementConfiguration(2, versions.get(0), versions.get(1)));
               HTMLWriter htmlWriter = new HTMLWriter(emptyNode, data2, destFolder, null,
                     node);
               htmlWriter.writeHTML();
            }
         }
      }
   }

   private List<String> getVersions(final Chunk chunk) {
      List<String> versions = new LinkedList<>();
      for (Result result : chunk.getResult()) {
         if (!versions.contains(result.getVersion().getGitversion())) {
            versions.add(result.getVersion().getGitversion());
         }
      }
      return versions;
   }

   private void getFullTree(final RCAGenerator rcaGenerator, final CauseSearchData data, final File treeFolder) throws IOException, JsonParseException, JsonMappingException {
      if (treeFolder.exists()) {
         final File potentialCacheFileOld = new File(treeFolder, data.getMeasurementConfig().getVersionOld());
         final File potentialCacheFile = new File(treeFolder, data.getMeasurementConfig().getVersion());

         final CallTreeNode rootPredecessor = Constants.OBJECTMAPPER.readValue(potentialCacheFileOld, CallTreeNode.class);
         final CallTreeNode rootVersion = Constants.OBJECTMAPPER.readValue(potentialCacheFile, CallTreeNode.class);

         rcaGenerator.setFullTree(rootPredecessor, rootVersion);
      }
   }

   private void analyzeFile(final File versionResultFolder, final File treeFile)
         throws JsonParseException, JsonMappingException, IOException, JsonProcessingException, FileNotFoundException, JAXBException {
      final CauseSearchFolders folders = getCauseSearchFolders(treeFile);

      final RCAGenerator rcaGenerator = new RCAGenerator(treeFile, versionResultFolder, folders);
      final File propertyFolder = getPropertyFolder(projectName);
      rcaGenerator.setPropertyFolder(propertyFolder);

      if (visualizeFull) {
         final CauseSearchData data = rcaGenerator.getData();

         final File treeFolder = folders.getTreeCacheFolder(data.getMeasurementConfig().getVersion(), data.getCauseConfig().getTestCase());
         getFullTree(rcaGenerator, data, treeFolder);
      }
      rcaGenerator.createVisualization();
   }

   private CauseSearchFolders getCauseSearchFolders(final File treeFile) {
      final File projectFolder = treeFile.getAbsoluteFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();

      final CauseSearchFolders folders;
      if (projectFolder.getName().contentEquals("rca")) {
         folders = new CauseSearchFolders(projectFolder.getParentFile());
      } else {
         folders = new CauseSearchFolders(projectFolder);
      }
      return folders;
   }

   private File getPropertyFolder(final String projectName) {
      final File propertyFolder = this.propertyFolder != null ? this.propertyFolder
            : new File(new RepoFolders().getPropertiesFolder(), "properties" + File.separator + projectName);
      return propertyFolder;
   }

   public File[] getData() {
      return data;
   }

   public void setData(final File[] data) {
      this.data = data;
   }

   public File getPropertyFolder() {
      return propertyFolder;
   }

   public void setPropertyFolder(final File propertyFolder) {
      this.propertyFolder = propertyFolder;
   }

   public File getResultFolder() {
      return resultFolder;
   }

   public void setResultFolder(final File resultFolder) {
      this.resultFolder = resultFolder;
   }

}
