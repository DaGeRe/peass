package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.execution.MeasurementConfigurationMixin;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependency.reader.VersionKeeper;
import de.peass.dependency.traces.ViewGenerator;
import de.peass.dependencyprocessors.AdaptiveTester;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.Constants;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionIteratorGit;
import de.peran.AnalyseOneTest;
import de.peran.measurement.analysis.AnalyseFullData;
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
public class ContinuousExecutor implements Callable<Void> {
   private static final Logger LOG = LogManager.getLogger(ContinuousExecutor.class);

   @Mixin
   MeasurementConfigurationMixin measurementConfigMixin;

   @Option(names = { "-threads", "--threads" }, description = "Count of threads")
   int threads = 100;

   @Option(names = { "-test", "--test" }, description = "Name of the test to execute")
   String testName;

   @Option(names = { "-folder", "--folder" }, description = "Folder of the project that should be analyzed", required = true)
   protected File projectFolder;

   public static void main(final String[] args) throws InterruptedException, IOException, JAXBException {
      final ContinuousExecutor command = new ContinuousExecutor();
      final CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);

   }

   private static void cloneProject(final File cloneProjectFolder, final File localFolder) throws InterruptedException, IOException {
      localFolder.mkdirs();
      final ProcessBuilder builder = new ProcessBuilder("git", "clone", cloneProjectFolder.getAbsolutePath());
      builder.directory(localFolder);
      builder.start().waitFor();
   }

   private File executeMeasurements(final File localFolder, final PeASSFolders folders,
         final String previousName,
         final GitCommit headCommit, final Set<TestCase> tests) throws IOException, InterruptedException, JAXBException {
      final File fullResultsVersion = new File(localFolder, headCommit.getTag());
      if (!fullResultsVersion.exists()) {
         final MeasurementConfiguration configuration = new MeasurementConfiguration(measurementConfigMixin);
         final JUnitTestTransformer testgenerator = new JUnitTestTransformer(folders.getProjectFolder(), configuration);
         testgenerator.getConfig().setUseKieker(false);

         final AdaptiveTester tester = new AdaptiveTester(folders, testgenerator);
         for (final TestCase test : tests) {
            tester.evaluate(test);
         }

         final File fullResultsFolder = folders.getFullMeasurementFolder();
         LOG.debug("Moving to: {}", fullResultsVersion.getAbsolutePath());
         FileUtils.moveDirectory(fullResultsFolder, fullResultsVersion);
      }

      final File measurementFolder = new File(fullResultsVersion, "measurements");
      return measurementFolder;
   }

   private static TestSet getViewTests(final int threads, final File localFolder, final File projectFolder, final PeASSFolders folders, final Dependencies dependencies,
         final String version) throws IOException, JAXBException, JsonParseException, JsonMappingException {
      final File executeFile = new File(localFolder, "execute.json");
      final File viewFolder = new File(localFolder, "views");
      viewFolder.mkdir();
      FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
      if (!executeFile.exists()) {
         final ViewGenerator viewgenerator = new ViewGenerator(projectFolder, dependencies, executeFile, viewFolder, threads, 15);
         viewgenerator.processCommandline();
      }
      final ExecutionData traceTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);
      LOG.debug("Version: {} Path: {}", version, executeFile.getAbsolutePath());
      final TestSet traceTestSet = traceTests.getVersions().get(version);
      return traceTestSet;
   }

   private static Dependencies getDependencies(final File projectFolder, final File dependencyFile, final String previousName, final GitCommit headCommit)
         throws JAXBException, JsonParseException, JsonMappingException, IOException, InterruptedException, XmlPullParserException {
      Dependencies dependencies;

      final String url = GitUtils.getURL(projectFolder);
      final List<GitCommit> entries = new LinkedList<>();
      final GitCommit prevCommit = new GitCommit(previousName, "", "", "");
      entries.add(prevCommit);
      entries.add(headCommit);
      final VersionIteratorGit iterator = new VersionIteratorGit(projectFolder, entries, prevCommit);
      boolean needToLoad = false;

      final VersionKeeper nonRunning = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonRunning_" + projectFolder.getName() + ".json"));
      final VersionKeeper nonChanges = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonChanges_" + projectFolder.getName() + ".json"));

      // final VersionKeeper nrv = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonrunning.json"));
      if (!dependencyFile.exists()) {
         dependencies = fullyLoadDependencies(projectFolder, dependencyFile, url, iterator, nonChanges);
      } else {
         dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);

         if (dependencies.getVersions().size() > 0) {
            final String versionName = dependencies.getVersionNames()[dependencies.getVersions().size() - 1];
            if (!versionName.equals(headCommit.getTag())) {
               needToLoad = true;
            }
         } else {
            needToLoad = true;
         }
         if (needToLoad) {
            //TODO Continuous Dependency Reading
         }
      }
      
      return dependencies;
   }

   private static Dependencies fullyLoadDependencies(final File projectFolder, final File dependencyFile, final String url, final VersionIteratorGit iterator,
         final VersionKeeper nonChanges) throws IOException, InterruptedException, XmlPullParserException, JsonParseException, JsonMappingException {
      Dependencies dependencies;
      final DependencyReader reader = new DependencyReader(projectFolder, dependencyFile, url, iterator, 10, nonChanges);
      iterator.goToPreviousCommit();
      if (!reader.readInitialVersion()) {
         LOG.error("Analyzing first version was not possible");
      } else {
         reader.readDependencies();
      }
      dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
      return dependencies;
   }

   @Override
   public Void call() throws Exception {
      final String homeFolderName = System.getenv("PEASS_HOME") != null ? System.getenv("PEASS_HOME") : System.getenv("HOME") + File.separator + ".peass" + File.separator;
      final File peassFolder = new File(homeFolderName);

      if (!peassFolder.exists()) {
         peassFolder.mkdirs();
      }
      LOG.debug("PeASS-Folder: {} Exists: {}", peassFolder, peassFolder.exists());
      //
      LOG.debug("Folder: {}", projectFolder);
      final File cloneProjectFolder = projectFolder;

      final boolean useViews = true;

      final File localFolder = new File(peassFolder, cloneProjectFolder.getName() + "_full");
      final File projectFolder = new File(localFolder, cloneProjectFolder.getName());
      if (!localFolder.exists() || !projectFolder.exists()) {
         cloneProject(cloneProjectFolder, localFolder);
      } else {
         final String head = GitUtils.getName("HEAD", cloneProjectFolder);
         GitUtils.goToTag(head, projectFolder);
      }

      final PeASSFolders folders = new PeASSFolders(projectFolder);

      final File dependencyFile = new File(localFolder, "dependencies.json");
      final String previousName = GitUtils.getName("HEAD~1", projectFolder);
      final GitCommit headCommit = new GitCommit(GitUtils.getName("HEAD", projectFolder), "", "", "");
      //
      final Dependencies dependencies = getDependencies(projectFolder, dependencyFile, previousName, headCommit);

      if (dependencies.getVersions().size() > 0) {
         VersionComparator.setDependencies(dependencies);
         final String versionName = dependencies.getVersionNames()[dependencies.getVersions().size() - 1];
         final Version currentVersion = dependencies.getVersions().get(versionName);

         final Set<TestCase> tests = new HashSet<>();

         if (useViews) {
            final TestSet traceTestSet = getViewTests(threads, localFolder, projectFolder, folders, dependencies, versionName);
            for (final Map.Entry<ChangedEntity, Set<String>> test : traceTestSet.getTestcases().entrySet()) {
               for (final String method : test.getValue()) {
                  tests.add(new TestCase(test.getKey().getClazz(), method));
               }
            }
         } else {
            for (final TestSet dep : currentVersion.getChangedClazzes().values()) {
               tests.addAll(dep.getTests());
            }
         }

         final File measurementFolder = executeMeasurements(localFolder, folders, previousName, headCommit, tests);
         final File changefile = new File(localFolder, "changes.json");
         AnalyseOneTest.setResultFolder(new File(localFolder, headCommit.getTag() + "_graphs"));
         final AnalyseFullData afd = new AnalyseFullData(changefile);
         afd.analyseFolder(measurementFolder);
      } else {
         LOG.info("No test executed - version did not contain changed tests.");
      }
      return null;
   }
}
