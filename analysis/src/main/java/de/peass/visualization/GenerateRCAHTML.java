package de.peass.visualization;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
               final File rcaFolder = new File(source, "rootCauseAnalysisTree");
               for (final File versionFolder : rcaFolder.listFiles()) {
                  for (final File testcaseFlder : versionFolder.listFiles()) {
                     for (final File treeFile : testcaseFlder.listFiles()) {
                        new RCAGenerator(treeFile, resultFolder).createVisualization();
                     }
                  }
               }
            } else {
               for (final File treeFile : source.listFiles()) {
                  new RCAGenerator(treeFile, resultFolder).createVisualization();
               }
            }
         } else {
            new RCAGenerator(source, resultFolder).createVisualization();
         }
      }
      return null;
   }

   
}
