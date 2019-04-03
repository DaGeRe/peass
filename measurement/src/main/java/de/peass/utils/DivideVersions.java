package de.peass.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
      final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE, OptionConstants.EXECUTIONFILE);
      options.addOption(experimentIdOption);
      final CommandLineParser parser = new DefaultParser();

      final CommandLine line = parser.parse(options, args);

      final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
      final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
      VersionComparator.setDependencies(dependencies);
      

      final ExecutionData changedTests = TestLoadUtil.loadChangedTests(line);

      final File resultFolder = new File("results");
      if (!resultFolder.exists()) {
         resultFolder.mkdirs();
      }
      final String experimentid = line.getOptionValue("experiment_id", "unknown");

      final File executeCommands = new File(resultFolder, "execute-" + VersionComparator.getProjectName() + ".sh");
      generateExecuteCommands(dependencies, changedTests, experimentid, executeCommands, System.out);

   }

   public static void generateExecuteCommands(final Dependencies dependencies, final ExecutionData changedTests, final String experimentid,
         final File executeCommands, final PrintStream goal) throws IOException {
      final String url = dependencies.getUrl().replaceAll("\n", "").replaceAll(" ", "");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(executeCommands))) {
         goal.println("timestamp=$(date +%s)");
         final String[] versions = dependencies.getVersionNames();
         for (int i = 0; i < dependencies.getVersions().size(); i++) {
            final String endversion = versions[i];
            // System.out.println("-startversion " + startversion + " -endversion " + endversion);
            if (changedTests == null) {
               goal.println(
                     "sbatch --nice=1000000 --time=10-0 "
                           + "--output=/nfs/user/do820mize/processlogs/process_" + i + "_$timestamp.out "
                           + "--workdir=/nfs/user/do820mize "
                           + "--export=PROJECT=" + url + ",HOME=/newnfs/user/do820mize,START="
                           + endversion + ",END=" + endversion + ",INDEX=" + i + " executeTests.sh");
            } else if (changedTests != null && changedTests.getVersions().containsKey(endversion)) {
               for (final Map.Entry<ChangedEntity, Set<String>> testcase : changedTests.getVersions().get(endversion).getTestcases().entrySet()) {
                  for (final String method : testcase.getValue()) {
                     final String testcaseName = testcase.getKey().getJavaClazzName() + "#" + method;
                     final String simpleTestName = testcaseName.substring(testcaseName.lastIndexOf('.') +1);
                     createSingleSBatch(experimentid, goal, url, i, endversion, testcaseName, simpleTestName);
                     
                     writer.write("java -cp target/measurement-0.1-SNAPSHOT.jar de.peass.AdaptiveTestStarter "
                           + "-test " + testcaseName + " "
                           + "-warmup 0 "
                           + "-iterations 1000 "
                           + "-repetitions 100 "
                           + "-vms 100 "
                           + "-timeout 10 "
                           + "-startversion " + endversion + " "
                           + "-endversion " + endversion + " "
                           + "-executionfile $PEASS_REPOS/dependencies-final/execute_" + dependencies.getName() + ".json "
                           + "-folder ../../projekte/" + dependencies.getName() + "/ "
                           + "-dependencyfile $PEASS_REPOS/dependencies-final/deps_" + dependencies.getName() + ".json &> measurement_" + endversion.substring(0, 6) + "_" + testcaseName
                           + ".txt\n");
                     writer.flush();
                  }
               }
            }
         }
      }
   }

   public static void createSingleSBatch(final String experimentid, final PrintStream goal, final String url, final int i, final String endversion, final String testcaseName,
         final String simpleTestName) {
      goal.println(
            "sbatch --partition=galaxy-low-prio --nice=1000000 --time=10-0 "
                  + "--output=/nfs/user/do820mize/processlogs/" + i + "_" + simpleTestName + "_$timestamp.out "
                  + "--workdir=/nfs/user/do820mize "
                  + "--export=PROJECT=" + url + ",HOME=/nfs/user/do820mize,"
                  + "START=" + endversion + ","
                  + "END=" + endversion + ","
                  + "INDEX=" + i + ","
                  + "EXPERIMENT_ID=" + experimentid + ","
                  + "TEST=" + testcaseName + " executeTests.sh");
   }
}
