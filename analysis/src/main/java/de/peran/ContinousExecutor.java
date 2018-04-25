package de.peran;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.dependency.PeASSFolderUtil;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.reader.DependencyReader;
import de.peran.dependencyprocessors.DependencyTester;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;
import de.peran.measurement.analysis.AnalyseFullData;
import de.peran.statistics.DependencyStatisticAnalyzer;
import de.peran.utils.OptionConstants;
import de.peran.vcs.GitCommit;
import de.peran.vcs.GitUtils;
import de.peran.vcs.VersionIteratorGit;

/**
 * Executes performance tests continously inside of a project
 * 
 * @author reichelt
 *
 */
public class ContinousExecutor {
   private static final Logger LOG = LogManager.getLogger(ContinousExecutor.class);

   public static void main(String[] args) throws ParseException, InterruptedException, IOException, JAXBException {

      final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.WARMUP, OptionConstants.ITERATIONS, OptionConstants.VMS);
      final String homeFolderName = System.getenv("PEASS_HOME") != null ? System.getenv("PEASS_HOME") : System.getenv("HOME") + File.separator + ".peass" + File.separator;
      final File peassFolder = new File(homeFolderName);

      if (!peassFolder.exists()) {
         peassFolder.mkdirs();
      }

      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      final File cloneProjectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));

      final int vms = Integer.parseInt(line.getOptionValue(OptionConstants.VMS.getName(), "15"));
      final int warmup = Integer.parseInt(line.getOptionValue(OptionConstants.WARMUP.getName(), "10"));
      final int iterations = Integer.parseInt(line.getOptionValue(OptionConstants.ITERATIONS.getName(), "10"));
      final int repetitions = Integer.parseInt(line.getOptionValue(OptionConstants.REPETITIONS.getName(), "10"));

      final File localFolder = new File(peassFolder, cloneProjectFolder.getName() + "_full");
      final File projectFolder = new File(localFolder, cloneProjectFolder.getName());
      if (!localFolder.exists() || !projectFolder.exists()) {
         localFolder.mkdirs();
         final ProcessBuilder builder = new ProcessBuilder("git", "clone", cloneProjectFolder.getAbsolutePath());
         builder.directory(localFolder);
         builder.start().waitFor();
      }

      PeASSFolderUtil.setProjectFolder(projectFolder);

      final File dependencyFile = new File(localFolder, "dependencies.xml");
      final String previousName = GitUtils.getName("HEAD~1", projectFolder);
      final GitCommit headCommit;

      if (!dependencyFile.exists()) {
         headCommit = new GitCommit(GitUtils.getName("HEAD", projectFolder), "", "", "");
         final String url = GitUtils.getURL(projectFolder);
         final List<GitCommit> entries = new LinkedList<>();
         final GitCommit prevCommit = new GitCommit(previousName, "", "", "");
         entries.add(prevCommit);
         entries.add(headCommit);
         final VersionIteratorGit iterator = new VersionIteratorGit(projectFolder, entries, prevCommit);
         final DependencyReader reader = new DependencyReader(projectFolder, dependencyFile, url, iterator);
         reader.readDependencies();
      } else {
         headCommit = new GitCommit(GitUtils.getName("HEAD", projectFolder), "", "", "");
      }

      final Versiondependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
      VersionComparator.setDependencies(dependencies);
      final Version currentVersion = dependencies.getVersions().getVersion().get(dependencies.getVersions().getVersion().size() - 1);

      final File fullResultsVersion = new File(localFolder, headCommit.getTag());
      if (!fullResultsVersion.exists()) {
         final Set<TestCase> tests = new HashSet<>();
         for (final Dependency dep : currentVersion.getDependency()) {
            for (final Testcase testcase : dep.getTestcase()) {
               for (final String method : testcase.getMethod()) {
                  tests.add(new TestCase(testcase.getClazz(), method));
               }
            }
         }

         final DependencyTester tester = new DependencyTester(projectFolder, warmup, iterations, vms, false, repetitions, false);
         for (final TestCase test : tests) {
            tester.evaluate(headCommit.getTag(), previousName, test);
         }

         final File fullResultsFolder = PeASSFolderUtil.getFullMeasurementFolder();
         LOG.debug("Moving to: {}", fullResultsVersion.getAbsolutePath());
         FileUtils.moveDirectory(fullResultsFolder, fullResultsVersion);
      }

      final File measurementFolder = new File(fullResultsVersion, "measurements");
      new AnalyseFullData().analyseFolder(measurementFolder);
   }
}
