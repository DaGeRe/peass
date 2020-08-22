package de.peass.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.statistics.DependencyStatisticAnalyzer;

/**
 * Divides the versions of a dependencyfile (and optionally an executionfile) in order to start slurm test executions.
 * 
 * @author reichelt
 *
 */
public class DivideVersions {

   public static void main(final String[] args) throws JAXBException, ParseException, JsonParseException, JsonMappingException, IOException {
      final Option experimentIdOption = Option.builder("experiment_id").hasArg().build();
      final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE, OptionConstants.EXECUTIONFILE, OptionConstants.USE_SLURM);
      options.addOption(experimentIdOption);
      final CommandLineParser parser = new DefaultParser();

      final CommandLine line = parser.parse(options, args);

      final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
      final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
      VersionComparator.setDependencies(dependencies);

      final ExecutionData changedTests = TestLoadUtil.loadChangedTests(line);

      final File resultFolder = new File("results");
      if (!resultFolder.exists()) {
         resultFolder.mkdirs();
      }
      final String experimentId = line.getOptionValue("experiment_id", "unknown");

      boolean useSlurm = Boolean.parseBoolean(line.getOptionValue(OptionConstants.USE_SLURM.getName(), "true"));

      PrintStream destination = System.out;
      RunCommandWriter writer;
      if (useSlurm) {
         destination.println("timestamp=$(date +%s)");
         writer = new RunCommandWriterSlurm(System.out, experimentId, dependencies);
      } else {
         writer = new RunCommandWriter(destination, experimentId, dependencies);
      }

      generateExecuteCommands(dependencies, changedTests, experimentId, writer);
   }

   public static void generateExecuteCommands(final Dependencies dependencies, final ExecutionData changedTests, final String experimentId, PrintStream goal) throws IOException {
      generateExecuteCommands(dependencies, changedTests, experimentId, new RunCommandWriterSlurm(goal, experimentId, dependencies));
   }

   public static void generateExecuteCommands(final ExecutionData changedTests, final String experimentId, PrintStream goal) throws IOException {
      generateExecuteCommands(changedTests, experimentId, new RunCommandWriterSlurm(goal, experimentId, changedTests));
   }

   public static void generateExecuteCommands(final ExecutionData changedTests, final String experimentId, RunCommandWriter writer)
         throws IOException {
      int i = 0;
      for (Map.Entry<String, TestSet> entry : changedTests.getVersions().entrySet()) {
         for (final Map.Entry<ChangedEntity, Set<String>> testcase : entry.getValue().getTestcases().entrySet()) {
            for (final String method : testcase.getValue()) {
               final String testcaseName = testcase.getKey().getJavaClazzName() + "#" + method;
               writer.createSingleMethodCommand(i, entry.getKey(), testcaseName);

            }
         }
         i++;
      }
   }

   public static void generateExecuteCommands(final Dependencies dependencies, final ExecutionData changedTests, final String experimentId, RunCommandWriter writer)
         throws IOException {
      final String[] versions = dependencies.getVersionNames();
      for (int i = 0; i < dependencies.getVersions().size(); i++) {
         final String endversion = versions[i];
         // System.out.println("-startversion " + startversion + " -endversion " + endversion);
         if (changedTests == null) {
            writer.createFullVersionCommand(i, endversion);
         } else if (changedTests != null && changedTests.getVersions().containsKey(endversion)) {
            for (final Map.Entry<ChangedEntity, Set<String>> testcase : changedTests.getVersions().get(endversion).getTestcases().entrySet()) {
               for (final String method : testcase.getValue()) {
                  final String testcaseName = testcase.getKey().getJavaClazzName() + "#" + method;
                  writer.createSingleMethodCommand(i, endversion, testcaseName);

               }
            }
         }
      }
   }
}
