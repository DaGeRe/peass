package de.peass.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peass.analysis.all.RepoFolders;
import de.peass.dependency.CauseSearchFolders;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Visualizes the root-cause-analysis-tree as HTML page", name = "visualizerca")
public class GenerateRCAHTML implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(GenerateRCAHTML.class);

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

   private final File resultFolder = new File("results");
   // TODO Fix dirty hack
   final String projectName = "commons-fileupload";

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final CommandLine commandLine = new CommandLine(new GenerateRCAHTML());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      for (final File source : data) {
         if (source.isDirectory()) {
            if (source.getName().endsWith("_peass")) {
               handlePeassFolder(source);
            } else if (source.getName().equals("galaxy") || source.getParentFile().getName().contains("galaxy")) {
               handleSlurmFolder(source);
            } else {
               boolean containsSlurmChild = false;
               for (final File child : source.listFiles()) {
                  if (child.getName().matches("[0-9]+_[0-9]+")) {
                     containsSlurmChild = true;
                  }
               }
               if (containsSlurmChild) {
                  handleSlurmFolder(source);
               } else {
                  handleSimpleFolder(source);
               }
            }
         } else {
            analyzeFile(resultFolder, source);
         }
      }
      return null;
   }

   private void handleSimpleFolder(final File source) throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      for (final File treeFile : source.listFiles()) {
         if (treeFile.getName().endsWith(".json")) {
            final RCAGenerator rcaGenerator = new RCAGenerator(treeFile, resultFolder);
            final File propertyFolder = getPropertyFolder(projectName);
            rcaGenerator.setPropertyFolder(propertyFolder);
            if (visualizeFull) {
               final CauseSearchData data = rcaGenerator.getData();

               final File projectFolder = treeFile.getAbsoluteFile().getParentFile().getParentFile().getParentFile().getParentFile();
               final File treeFolder = new File(projectFolder, "treeCache" + File.separator + data.getMeasurementConfig().getVersion() + File.separator
                     + data.getCauseConfig().getTestCase().getClazz() + File.separator
                     + data.getCauseConfig().getTestCase().getMethod());

               getFullTree(rcaGenerator, data, treeFolder);
            }
            rcaGenerator.createVisualization();
         }
      }
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

   private void handleSlurmFolder(final File source) throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      for (final File job : source.listFiles()) {
         if (job.isDirectory()) {
            final File peassFolder = new File(job, "peass");
            handlePeassFolder(peassFolder);
         }
      }
   }

   private void handlePeassFolder(final File source) throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      final File rcaFolder = new File(source, "rca" + File.separator + "tree");
      for (final File versionFolder : rcaFolder.listFiles()) {
         final File versionResultFolder = new File(resultFolder, versionFolder.getName());
         versionResultFolder.mkdirs();
         for (final File testcaseFolder : versionFolder.listFiles()) {
            for (final File treeFile : testcaseFolder.listFiles()) {
               if (treeFile.getName().endsWith(".json")) {
                  analyzeFile(versionResultFolder, treeFile);
               }
            }
         }
      }
   }

   private void analyzeFile(final File versionResultFolder, final File treeFile)
         throws JsonParseException, JsonMappingException, IOException, JsonProcessingException, FileNotFoundException {
      final RCAGenerator rcaGenerator = new RCAGenerator(treeFile, versionResultFolder);
      final File propertyFolder = getPropertyFolder(projectName);
      rcaGenerator.setPropertyFolder(propertyFolder);
      if (visualizeFull) {
         final CauseSearchData data = rcaGenerator.getData();

         final File projectFolder = treeFile.getAbsoluteFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();

         final CauseSearchFolders folders;
         if (projectFolder.getName().contentEquals("rca")) {
            folders = new CauseSearchFolders(projectFolder.getParentFile());
         } else {
            folders = new CauseSearchFolders(projectFolder);
         }

         final File treeFolder = folders.getTreeCacheFolder(data.getMeasurementConfig().getVersion(), data.getCauseConfig().getTestCase());

         getFullTree(rcaGenerator, data, treeFolder);

      }
      rcaGenerator.createVisualization();
   }

   private File getPropertyFolder(final String projectName) {
      final File propertyFolder = this.propertyFolder != null ? this.propertyFolder : new File(new RepoFolders().getPropertiesFolder(), "properties" + File.separator + projectName);
      return propertyFolder;
   }

}
