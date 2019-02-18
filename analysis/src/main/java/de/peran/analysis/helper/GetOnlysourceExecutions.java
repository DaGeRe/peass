package de.peran.analysis.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.changes.Change;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.OptionConstants;
import de.peass.utils.TestLoadUtil;
import de.peran.analysis.helper.read.PropertyReadHelper;

public class GetOnlysourceExecutions {
   public static void main(final String[] args) throws JAXBException, ParseException, JsonParseException, JsonMappingException, IOException {
      Option experimentIdOption = Option.builder("experiment_id").hasArg().build();
      final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE, OptionConstants.EXECUTIONFILE, OptionConstants.FOLDER, OptionConstants.VIEWFOLDER);
      options.addOption(experimentIdOption);
      final CommandLineParser parser = new DefaultParser();

      final CommandLine line = parser.parse(options, args);

      DependencyReaderUtil.loadDependencies(line);
      final Dependencies dependencies = VersionComparator.getDependencies();
      final String url = dependencies.getUrl().replaceAll("\n", "").replaceAll(" ", "");
      final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
      final File viewFolder = new File(line.getOptionValue(OptionConstants.VIEWFOLDER.getName()));

      final ExecutionData changedTests = TestLoadUtil.loadChangedTests(line);

      final File resultFolder = new File("results");
      if (!resultFolder.exists()) {
         resultFolder.mkdirs();
      }
      final String experimentid = line.getOptionValue("experiment_id", "unknown");

      final String projectName = VersionComparator.getProjectName();
      final File executeCommands = new File(resultFolder, "execute-" + projectName + "-onlysource.sh");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(executeCommands))) {
         // writer.write("export REPETITIONS=1000\n");
         // writer.write("export ITERATIONS=100\n");
         System.out.println("timestamp=$(date +%s)");
         String[] versions = dependencies.getVersionNames();
         for (int i = 0; i < dependencies.getVersions().size(); i++) {
            final String endversion = versions[i];
            // System.out.println("-startversion " + startversion + " -endversion " + endversion);
            if (changedTests == null) {
               System.out.println(
                     "sbatch --nice=1000000 --time=10-0 "
                           + "--output=/nfs/user/do820mize/processlogs/process_" + i + "_$timestamp.out "
                           + "--workdir=/nfs/user/do820mize "
                           + "--export=PROJECT=" + url + ",HOME=/newnfs/user/do820mize,START="
                           + endversion + ",END=" + endversion + ",INDEX=" + i + " executeTests.sh");
            } else if (changedTests != null && changedTests.getVersions().containsKey(endversion)) {
               for (final Map.Entry<ChangedEntity, List<String>> testcase : changedTests.getVersions().get(endversion).getTestcases().entrySet()) {
                  for (final String method : testcase.getValue()) {
                     final Change c = new Change();
                     System.out.println(endversion);
                     PropertyReadHelper helper = new PropertyReadHelper(endversion, VersionComparator.getPreviousVersion(endversion), testcase.getKey(), c,
                           projectFolder, viewFolder);
                     final ChangeProperty prop = new ChangeProperty();
                     prop.setMethod(method);
                     helper.getSourceInfos(prop);
                     if (prop.isAffectsSource() && !prop.isAffectsTestSource()) {
                        final String testcaseName = testcase.getKey().getJavaClazzName() + "#" + method;
                        System.out.println(
                              "sbatch --nice=1000000 --time=10-0 "
                                    + "--output=/nfs/user/do820mize/processlogs/process_" + i + "_" + method + "_$timestamp.out "
                                    + "--workdir=/nfs/user/do820mize "
                                    + "--export=PROJECT=" + url + ",HOME=/nfs/user/do820mize,"
                                    + "START=" + endversion + ","
                                    + "END=" + endversion + ","
                                    + "INDEX=" + i + ","
                                    + "EXPERIMENT_ID=" + experimentid + ","
                                    + "TEST=" + testcaseName + " executeTests.sh");
                        writer.write("java -cp target/measurement-0.1-SNAPSHOT.jar de.peran.AdaptiveTestStarter "
                              + "-test " + testcaseName + " "
                              + "-warmup 0 "
                              + "-iterations 1000 "
                              + "-repetitions 100 "
                              + "-vms 100 "
                              + "-timeout 300 "
                              + "-startversion " + endversion + " "
                              + "-endversion " + endversion + " "
                              + "-executionfile ../../dependencies/execute-"+projectName+".json "
                              + "-folder ../../projekte/" + projectName + "/"
                              + "-dependencyfile ../../dependencies/deps_"+projectName+".xml &> measurement_" + endversion.substring(0, 6) + "_" + testcaseName + ".txt\n");
                        writer.flush();
                     }
                  }
               }
            }
         }
      }

   }
}
