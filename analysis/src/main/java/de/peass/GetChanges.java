package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import de.peass.analysis.changes.ChangeReader;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "getchanges", description = "Determines changes based on measurement values using agnostic t-test", mixinStandardHelpOptions = true)
public class GetChanges implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(GetChanges.class);

   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Path to the dependencyfile")
   protected File dependencyFile;

   @Option(names = { "-executionfile", "--executionfile" }, description = "Path to the executionfile")
   protected File executionfile;

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   protected File data;

   @Option(names = { "-out", "--out" }, description = "Path for saving the changefile")
   File out = new File("results");

   @Option(names = { "-confidence", "--confidence" }, description = "Confidence level for agnostic t-test")
   private double confidence = 0.01;

   public GetChanges() {

   }

   public static void main(final String[] args) {
      CommandLine commandLine = new CommandLine(new GetChanges());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      getVersionOrder();

      if (!out.exists()) {
         out.mkdirs();
      }
      final File statisticFolder = new File(out, "statistics");
      if (!statisticFolder.exists()) {
         statisticFolder.mkdir();
      }

      final ChangeReader reader = new ChangeReader(statisticFolder, VersionComparator.getProjectName());
      reader.setConfidence(confidence);

      reader.readFile(data);
      return null;
   }

   public void getVersionOrder() throws IOException, JsonParseException, JsonMappingException {
      Dependencies dependencies = null;
      ExecutionData executionData = null;
      if (dependencyFile != null) {
         dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);
      }
      if (executionfile != null) {
         executionData = Constants.OBJECTMAPPER.readValue(executionfile, ExecutionData.class);
         dependencies = new Dependencies(executionData);
         VersionComparator.setDependencies(dependencies);
      }
      if (executionData == null && dependencies == null) {
         throw new RuntimeException("Dependencyfile and executionfile not readable - one needs to be defined and valid!");
      }
   }
}
