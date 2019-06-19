package de.peran.analysis.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
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

import de.peass.analysis.changes.Change;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.OptionConstants;
import de.peass.utils.RunCommandWriter;
import de.peass.utils.RunCommandWriterSlurm;
import de.peass.utils.TestLoadUtil;
import de.peran.analysis.helper.read.PropertyReadHelper;

public class GetOnlysourceExecutions {
   public static void main(final String[] args) throws JAXBException, ParseException, JsonParseException, JsonMappingException, IOException {
      final Option experimentIdOption = Option.builder("experiment_id").hasArg().build();
      final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE, OptionConstants.EXECUTIONFILE, OptionConstants.FOLDER, OptionConstants.VIEWFOLDER);
      options.addOption(experimentIdOption);
      final CommandLineParser parser = new DefaultParser();

      final CommandLine line = parser.parse(options, args);

      DependencyReaderUtil.loadDependencies(line);
      final Dependencies dependencies = VersionComparator.getDependencies();
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

      RunCommandWriter writer = new RunCommandWriterSlurm(new PrintStream(new FileOutputStream(executeCommands)), experimentid, dependencies);
      final String[] versions = dependencies.getVersionNames();
      for (int versionIndex = 0; versionIndex < dependencies.getVersions().size(); versionIndex++) {
         final String endversion = versions[versionIndex];
         // System.out.println("-startversion " + startversion + " -endversion " + endversion);
         if (changedTests == null) {
            writer.createFullVersionCommand(versionIndex, endversion);
         } else if (changedTests != null && changedTests.getVersions().containsKey(endversion)) {
            for (final Map.Entry<ChangedEntity, Set<String>> testcase : changedTests.getVersions().get(endversion).getTestcases().entrySet()) {
               for (final String method : testcase.getValue()) {
                  final Change c = new Change();
                  System.out.println(endversion);
                  final PropertyReadHelper helper = new PropertyReadHelper(endversion, VersionComparator.getPreviousVersion(endversion), testcase.getKey(), c,
                        projectFolder, viewFolder);
                  final ChangeProperty prop = new ChangeProperty();
                  prop.setMethod(method);
                  helper.getSourceInfos(prop);
                  if (prop.isAffectsSource() && !prop.isAffectsTestSource()) {
                     final String testcaseName = testcase.getKey().getJavaClazzName() + "#" + method;
                     writer.createSingleMethodCommand(versionIndex, endversion, testcaseName);
                  }
               }
            }
         }
      }

   }
}
