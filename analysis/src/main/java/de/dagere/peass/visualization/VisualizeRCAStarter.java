package de.dagere.peass.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Visualizes the root-cause-analysis-tree as HTML page", name = "visualizerca")
public class VisualizeRCAStarter implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(VisualizeRCAStarter.class);

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

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final CommandLine commandLine = new CommandLine(new VisualizeRCAStarter());
      System.exit(commandLine.execute(args));
   }

   public VisualizeRCAStarter() {
   }

   public VisualizeRCAStarter(final File[] data, final File resultFolder, File propertyFolder) {
      this.data = data;
      this.resultFolder = resultFolder;
      this.propertyFolder = propertyFolder;
   }

   @Override
   public Void call() throws IOException {
      if (!resultFolder.exists()) {
         resultFolder.mkdir();
      }

      List<File> rcaFolderToHandle = new RCAFolderSearcher(data).searchRCAFiles();
      LOG.info("Handling: " + rcaFolderToHandle);
      for (File rcaFolder : rcaFolderToHandle) {
         visualizeRCAFile(resultFolder, rcaFolder);
      }
      List<File> peassFolderToHandle = new RCAFolderSearcher(data).searchPeassFiles();
      for (File peassFolder : peassFolderToHandle) {
         visualizeRegularMeasurementFile(peassFolder);
      }

      return null;
   }

   private void visualizeRegularMeasurementFile(final File peassFolder) throws IOException {
      VisualizeRegularMeasurement measurementVisualizer = new VisualizeRegularMeasurement(resultFolder);
      measurementVisualizer.analyzeFile(peassFolder);
   }

   private void getFullTree(final RCAGenerator rcaGenerator, final CauseSearchData data, final File treeFolder) throws IOException, JsonParseException, JsonMappingException {
      if (treeFolder.exists()) {
         final File potentialCacheFileOld = new File(treeFolder, data.getMeasurementConfig().getFixedCommitConfig().getCommitOld());
         final File potentialCacheFile = new File(treeFolder, data.getMeasurementConfig().getFixedCommitConfig().getCommit());

         final CallTreeNode rootPredecessor = Constants.OBJECTMAPPER.readValue(potentialCacheFileOld, CallTreeNode.class);
         final CallTreeNode rootCurrent = Constants.OBJECTMAPPER.readValue(potentialCacheFile, CallTreeNode.class);

         rcaGenerator.setFullTree(rootPredecessor, rootCurrent);
      }
   }

   private void visualizeRCAFile(final File commitResultFolder, final File treeFile) throws IOException {
      final CauseSearchFolders folders = getCauseSearchFolders(treeFile);

      final RCAGenerator rcaGenerator = new RCAGenerator(treeFile, commitResultFolder, folders);
      rcaGenerator.setPropertyFolder(propertyFolder);

      final CauseSearchData data = rcaGenerator.getData();
      final File treeFolder = folders.getTreeCacheFolder(data.getMeasurementConfig().getFixedCommitConfig().getCommit(), data.getCauseConfig().getTestCase());
      if (visualizeFull) {
         getFullTree(rcaGenerator, data, treeFolder);
      }
      visualizeSingleTree(rcaGenerator, treeFolder, data.getMeasurementConfig().getFixedCommitConfig().getCommitOld());
      visualizeSingleTree(rcaGenerator, treeFolder, data.getMeasurementConfig().getFixedCommitConfig().getCommit());
      
      rcaGenerator.createVisualization();
   }

   private void visualizeSingleTree(final RCAGenerator rcaGenerator, final File treeFolder, String commit) throws IOException, StreamReadException, DatabindException {
      final File potentialCacheFile = new File(treeFolder, commit);
      if (potentialCacheFile.exists()) {
         final CallTreeNode rootPredecessor = Constants.OBJECTMAPPER.readValue(potentialCacheFile, CallTreeNode.class);
         rcaGenerator.createSingleVisualization(commit, rootPredecessor);
      }
   }

   private CauseSearchFolders getCauseSearchFolders(final File treeFile) {
      final File projectFolder = treeFile.getAbsoluteFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
      System.out.println(projectFolder + " " + treeFile);

      final CauseSearchFolders folders;
      if (projectFolder.getName().contentEquals("rca")) {
         folders = new CauseSearchFolders(projectFolder.getParentFile());
      } else {
         folders = new CauseSearchFolders(projectFolder);
      }
      return folders;
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
