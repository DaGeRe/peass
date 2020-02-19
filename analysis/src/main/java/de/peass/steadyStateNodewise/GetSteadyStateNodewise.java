package de.peass.steadyStateNodewise;

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
import de.peass.measurement.rca.serialization.MeasuredNode;
import de.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Looks at the evolution of measurements of single nodes in to find whether steady state was reached", name = "steadystatenodewise")
public class GetSteadyStateNodewise implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(GetSteadyStateNodewise.class);

   private static final ObjectMapper MAPPER = new ObjectMapper();
   static {
      MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
   }

   @Option(names = { "-data", "--data" }, description = "Path to datafolder", required = true)
   protected File data[];

   private final File resultFolder = new File("results");
   // TODO Fix dirty hack
   final String projectName = "commons-fileupload";

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final CommandLine commandLine = new CommandLine(new GetSteadyStateNodewise());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      for (final File source : data) {
         if (source.isDirectory()) {
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
         } else {
            analyzeFile(resultFolder, source);
         }
      }
      return null;
   }

   private void handleSimpleFolder(final File source) throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      throw new RuntimeException("Not supported yet.");
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
      if (rcaFolder.exists()) {
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
   }

   private void analyzeFile(final File versionResultFolder, final File treeFile) throws JsonParseException, JsonMappingException, IOException, JsonProcessingException, FileNotFoundException {
      File details = new File(treeFile.getParentFile(), "details" + File.separator + treeFile.getName());

      System.out.println("Reading: " + details);

      CauseSearchData data = Constants.OBJECTMAPPER.readValue(details, CauseSearchData.class);

      MeasuredNode node = data.getNodes();
      printInVMDeviations(node);
   }

   public void printInVMDeviations(MeasuredNode node) {
      // final Collection<List<StatisticalSummary>> values = ;

      new PerVMDeviationDeriver(node.getKiekerPattern(), node.getValues().getValues().values()).printDeviations();
      new PerVMDeviationDeriver(node.getKiekerPattern(), node.getValuesPredecessor().getValues().values()).printDeviations();

      for (MeasuredNode child : node.getChildren()) {
         printInVMDeviations(child);
      }
   }

}
