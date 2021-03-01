package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.ci.ContinuousExecutor;
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.execution.ExecutionConfigMixin;
import de.peass.dependency.execution.MeasurementConfigurationMixin;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Executes performance tests continously inside of a project.
 * 
 * Therefore, the current HEAD commit and the predecessing commit are analysed; if no changes happen between this commits, no tests are executed.
 * 
 * @author reichelt
 *
 */
@Command(description = "Examines the current and last version of a project. If informations are present in default paths, these are used", 
   name = "continuousExecution")
public class ContinuousExecutionStarter implements Callable<Void> {
   private static final Logger LOG = LogManager.getLogger(ContinuousExecutionStarter.class);

   @Mixin
   ExecutionConfigMixin executionMixin;
   
   @Mixin
   MeasurementConfigurationMixin measurementConfigMixin;

   @Option(names = { "-threads", "--threads" }, description = "Count of threads")
   int threads = 100;

   @Option(names = { "-test", "--test" }, description = "Name of the test to execute")
   String testName;

   @Option(names = { "-folder", "--folder" }, description = "Folder of the project that should be analyzed", required = true)
   protected File projectFolder;
   
   private final boolean useViews = true;

   public static void main(final String[] args) throws InterruptedException, IOException, JAXBException {
      final ContinuousExecutionStarter command = new ContinuousExecutionStarter();
      final CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      final MeasurementConfiguration measurementConfig = new MeasurementConfiguration(measurementConfigMixin, executionMixin);
      final ContinuousExecutor executor = new ContinuousExecutor(projectFolder, measurementConfig, threads, useViews);
      executor.execute();
      return null;
   }
}
