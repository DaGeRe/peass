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

   @Option(names = { "-propertyFolder", "--propertyFolder" }, description = "Path to property folder", required = false)
   protected File propertyFolder;
   
   private File resultFolder = new File("results");

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
               handleSimpleFolder(source);
            }
         } else {
            new RCAGenerator(source, resultFolder).createVisualization();
         }
      }
      return null;
   }

   private void handleSimpleFolder(final File source) throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      for (final File treeFile : source.listFiles()) {
         if (treeFile.getName().endsWith(".json")) {
            final RCAGenerator rcaGenerator = new RCAGenerator(treeFile, resultFolder);
            rcaGenerator.createVisualization();
         }
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
                  final String projectName = "commons-fileupload";
                  final File propertyFolder = this.propertyFolder != null ? 
                        this.propertyFolder : 
                     new File(new RepoFolders().getPropertiesFolder(), "properties" + File.separator + projectName); 
                  final RCAGenerator rcaGenerator = new RCAGenerator(treeFile, versionResultFolder);
                  rcaGenerator.setPropertyFolder(propertyFolder);
                  rcaGenerator.createVisualization();
               }
            }
         }
      }
   }

}
