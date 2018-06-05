package de.peran;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import de.peran.dependency.PeASSFolders;
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependency.reader.DependencyReader;
import de.peran.dependency.traces.ViewGenerator;
import de.peran.dependencyprocessors.DependencyTester;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;
import de.peran.measurement.analysis.AnalyseFullData;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.statistics.DependencyStatisticAnalyzer;
import de.peran.utils.OptionConstants;
import de.peran.vcs.GitCommit;
import de.peran.vcs.GitUtils;
import de.peran.vcs.VersionIteratorGit;

/**
 * Executes performance tests continously inside of a project.
 * 
 * Therefore, the current HEAD commit and the predecessing commit are analysed; if no changes happen between this commits, no tests are executed.
 * 
 * @author reichelt
 *
 */
public class ContinousExecutor {
   private static final Logger LOG = LogManager.getLogger(ContinousExecutor.class);

   public static void main(String[] args) throws  InterruptedException, IOException, ParseException, JAXBException {
      final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.WARMUP, OptionConstants.ITERATIONS, OptionConstants.VMS,
            OptionConstants.REPETITIONS, OptionConstants.USEKIEKER);
      final String homeFolderName = System.getenv("PEASS_HOME") != null ? System.getenv("PEASS_HOME") : System.getenv("HOME") + File.separator + ".peass" + File.separator;
      final File peassFolder = new File(homeFolderName);

      if (!peassFolder.exists()) {
         peassFolder.mkdirs();
      }
      LOG.debug("PeASS-Folder: {} Exists: {}", peassFolder, peassFolder.exists());
//
      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      final String folderName = line.getOptionValue(OptionConstants.FOLDER.getName());
      LOG.debug("Folder: {}", folderName);
      final File cloneProjectFolder = new File(folderName);

      final int vms = Integer.parseInt(line.getOptionValue(OptionConstants.VMS.getName(), "15"));
      final int warmup = Integer.parseInt(line.getOptionValue(OptionConstants.WARMUP.getName(), "10"));
      final int iterations = Integer.parseInt(line.getOptionValue(OptionConstants.ITERATIONS.getName(), "10"));
      final int repetitions = Integer.parseInt(line.getOptionValue(OptionConstants.REPETITIONS.getName(), "10"));
      final boolean useViews = true;

      final File localFolder = new File(peassFolder, cloneProjectFolder.getName() + "_full");
      final File projectFolder = new File(localFolder, cloneProjectFolder.getName());
      if (!localFolder.exists() || !projectFolder.exists()) {
         localFolder.mkdirs();
         final ProcessBuilder builder = new ProcessBuilder("git", "clone", cloneProjectFolder.getAbsolutePath());
         builder.directory(localFolder);
         builder.start().waitFor();
      }

      final PeASSFolders folders = new PeASSFolders(projectFolder);

      final File dependencyFile = new File(localFolder, "dependencies.xml");
      final String previousName = GitUtils.getName("HEAD~1", projectFolder);
      final GitCommit headCommit = new GitCommit(GitUtils.getName("HEAD", projectFolder), "", "", "");
//
      final Versiondependencies dependencies = getDependencies(projectFolder, dependencyFile, previousName, headCommit);

      if (dependencies.getVersions().getVersion().size() > 0) {
         VersionComparator.setDependencies(dependencies);
         final Version currentVersion = dependencies.getVersions().getVersion().get(dependencies.getVersions().getVersion().size() - 1);

         final Set<TestCase> tests = new HashSet<>();
         
         if (useViews) {
            final File executeFile = new File(localFolder, "execute.json");
            final File viewFolder = new File(localFolder, "views");
            viewFolder.mkdir();
            FileUtils.deleteDirectory(folders.getKiekerResultFolder());
            if (!executeFile.exists()){
               final ViewGenerator viewgenerator = new ViewGenerator(projectFolder, dependencies, executeFile, viewFolder);
               viewgenerator.processCommandline();
            }
            final ChangedTraceTests traceTests = new ObjectMapper().readValue(executeFile, ChangedTraceTests.class);
            final TestSet traceTestSet = traceTests.getVersions().get(currentVersion.getVersion());
            for (final Map.Entry<ChangedEntity, List<String>> test : traceTestSet.getTestcases().entrySet()){
               for (final String method : test.getValue()){
                  tests.add(new TestCase(test.getKey().getClazz(), method));
               }
            }
         } else {
            for (final Dependency dep : currentVersion.getDependency()) {
               for (final Testcase testcase : dep.getTestcase()) {
                  for (final String method : testcase.getMethod()) {
                     tests.add(new TestCase(testcase.getClazz(), method));
                  }
               }
            }
         }

         final File fullResultsVersion = new File(localFolder, headCommit.getTag());
         if (!fullResultsVersion.exists()) {

            final DependencyTester tester = new DependencyTester(folders, warmup, iterations, vms, false, repetitions, false);
            for (final TestCase test : tests) {
               tester.evaluate(headCommit.getTag(), previousName, test);
            }

            final File fullResultsFolder = folders.getFullMeasurementFolder();
            LOG.debug("Moving to: {}", fullResultsVersion.getAbsolutePath());
            FileUtils.moveDirectory(fullResultsFolder, fullResultsVersion);
         }

         final File measurementFolder = new File(fullResultsVersion, "measurements");
         final File changefile = new File(localFolder, "changes.json");
         final AnalyseFullData afd = new AnalyseFullData(changefile);
         afd.analyseFolder(measurementFolder);
      } else {
         LOG.info("No test executed - version did not contain changed tests.");
      }
   }

   private static Versiondependencies getDependencies(final File projectFolder, final File dependencyFile, final String previousName, final GitCommit headCommit)
         throws JAXBException {
      Versiondependencies dependencies;

      final String url = GitUtils.getURL(projectFolder);
      final List<GitCommit> entries = new LinkedList<>();
      final GitCommit prevCommit = new GitCommit(previousName, "", "", "");
      entries.add(prevCommit);
      entries.add(headCommit);
      final VersionIteratorGit iterator = new VersionIteratorGit(projectFolder, entries, prevCommit);
      if (!dependencyFile.exists()) {
         final DependencyReader reader = new DependencyReader(projectFolder, dependencyFile, url, iterator);
         reader.readDependencies();
         dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
      } else {
         dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
         VersionComparator.setDependencies(dependencies);
         final Version currentVersion = dependencies.getVersions().getVersion().get(dependencies.getVersions().getVersion().size() - 1);
         if (!currentVersion.getVersion().equals(headCommit.getTag())) {
            final DependencyReader reader = new DependencyReader(projectFolder, dependencyFile, dependencies.getUrl(), iterator);
            reader.readDependencies();
            dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
         }
      }
      return dependencies;
   }
}
