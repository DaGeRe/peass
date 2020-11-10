package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.ContinuousExecutionStarter;
import de.peass.analysis.properties.PropertyReader;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.execution.MeasurementConfiguration;
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
import de.peran.measurement.analysis.ProjectStatistics;

public class ContinuousExecutor {

   private static final Logger LOG = LogManager.getLogger(ContinuousExecutor.class);

   private final File projectFolder;
   private final MeasurementConfiguration measurementConfig;
   private final int threads;
   private final boolean useViews;

   private GitCommit version;
   private String versionOld;

   private final File localFolder;
   private final File projectFolderLocal;
   private final PeASSFolders folders;
   private final File viewFolder;
   private final File propertyFolder;

   public ContinuousExecutor(File projectFolder, MeasurementConfiguration measurementConfig, int threads, boolean useViews) throws InterruptedException, IOException {
      this.projectFolder = projectFolder;
      this.measurementConfig = measurementConfig;
      this.threads = threads;
      this.useViews = useViews;

      localFolder = getLocalFolder();
      projectFolderLocal = new File(localFolder, projectFolder.getName());
      if (!localFolder.exists() || !projectFolderLocal.exists()) {
         cloneProject(projectFolder, localFolder);
         if (!projectFolderLocal.exists()) {
            throw new RuntimeException("Was not able to clone project!");
         }
      } else {
         final String head = GitUtils.getName("HEAD", projectFolder);
         GitUtils.goToTag(head, projectFolderLocal);
      }

      folders = new PeASSFolders(projectFolderLocal);
      viewFolder = new File(localFolder, "views");
      viewFolder.mkdir();

      propertyFolder = new File(localFolder, "properties");
   }

   public void execute() throws InterruptedException, IOException, JAXBException, XmlPullParserException {
      final File dependencyFile = new File(localFolder, "dependencies.json");
      versionOld = GitUtils.getName("HEAD~1", projectFolderLocal);
      version = new GitCommit(GitUtils.getName("HEAD", projectFolderLocal), "", "", "");
      //
      final Dependencies dependencies = getDependencies(projectFolderLocal, dependencyFile);

      if (dependencies.getVersions().size() > 0) {
         VersionComparator.setDependencies(dependencies);
         final String versionName = dependencies.getVersionNames()[dependencies.getVersions().size() - 1];
         final Version currentVersion = dependencies.getVersions().get(versionName);

         final Set<TestCase> tests = getTestSet(dependencies, currentVersion);

         final File measurementFolder = executeMeasurements(tests);
         final File changefile = new File(localFolder, "changes.json");
         AnalyseOneTest.setResultFolder(new File(localFolder, version.getTag() + "_graphs"));
         final ProjectStatistics statistics = new ProjectStatistics();
         final AnalyseFullData afd = new AnalyseFullData(changefile, statistics);
         afd.analyseFolder(measurementFolder);
         Constants.OBJECTMAPPER.writeValue(new File(localFolder, "statistics.json"), statistics);
      } else {
         LOG.info("No test executed - version did not contain changed tests.");
      }
   }

   private Set<TestCase> getTestSet(final Dependencies dependencies, final Version currentVersion) throws IOException, JAXBException, JsonParseException, JsonMappingException {
      final Set<TestCase> tests = new HashSet<>();

      if (useViews) {
         final TestSet traceTestSet = getViewTests(dependencies);
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
      return tests;
   }

   private static void cloneProject(final File cloneProjectFolder, final File localFolder) throws InterruptedException, IOException {
      localFolder.mkdirs();
      File gitFolder = new File(cloneProjectFolder, ".git");
      if (gitFolder.exists()) {
         LOG.info("Cloning using git clone");
         final ProcessBuilder builder = new ProcessBuilder("git", "clone", cloneProjectFolder.getAbsolutePath());
         builder.directory(localFolder);
         builder.start().waitFor();
      } else {
         throw new RuntimeException("No git folder " + gitFolder.getAbsolutePath() + " present - "
               + "currently, only git projects are supported");
      }
   }

   private File executeMeasurements(final Set<TestCase> tests) throws IOException, InterruptedException, JAXBException {
      final File fullResultsVersion = getFullResultsVersion();
      if (!fullResultsVersion.exists()) {
         MeasurementConfiguration copied = new MeasurementConfiguration(measurementConfig);
         final JUnitTestTransformer testgenerator = new JUnitTestTransformer(folders.getProjectFolder(), copied);
         testgenerator.getConfig().setUseKieker(false);
         measurementConfig.setVersion(version.getTag());
         measurementConfig.setVersionOld(versionOld);

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

   private TestSet getViewTests(final Dependencies dependencies)
         throws IOException, JAXBException, JsonParseException, JsonMappingException {
      final File executeFile = new File(localFolder, "execute.json");

      FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
      if (!executeFile.exists()) {
         final ViewGenerator viewgenerator = new ViewGenerator(projectFolder, dependencies, executeFile, viewFolder, threads, 15);
         viewgenerator.processCommandline();
         final PropertyReader propertyReader = new PropertyReader(propertyFolder, folders.getProjectFolder(), viewFolder);
         propertyReader.readAllTestsProperties(viewgenerator.getChangedTraceMethods());
      }
      final ExecutionData traceTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);
      LOG.debug("Version: {} Path: {}", version, executeFile.getAbsolutePath());
      final TestSet traceTestSet = traceTests.getVersions().get(version.getTag());

      return traceTestSet;
   }

   private Dependencies getDependencies(final File projectFolder, final File dependencyFile)
         throws JAXBException, JsonParseException, JsonMappingException, IOException, InterruptedException, XmlPullParserException {
      Dependencies dependencies;

      final String url = GitUtils.getURL(projectFolder);
      final List<GitCommit> entries = new LinkedList<>();
      final GitCommit prevCommit = new GitCommit(versionOld, "", "", "");
      entries.add(prevCommit);
      entries.add(version);
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
            if (!versionName.equals(version.getTag())) {
               needToLoad = true;
            }
         } else {
            needToLoad = true;
         }
         if (needToLoad) {
            // TODO Continuous Dependency Reading
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

   public File getLocalFolder() {
      final String homeFolderName = System.getenv("PEASS_HOME") != null ? System.getenv("PEASS_HOME") : System.getenv("HOME") + File.separator + ".peass" + File.separator;
      final File peassFolder = new File(homeFolderName);

      if (!peassFolder.exists()) {
         peassFolder.mkdirs();
      }
      LOG.debug("PeASS-Folder: {} Exists: {}", peassFolder, peassFolder.exists());
      //
      LOG.debug("Folder: {}", projectFolder);
      final File cloneProjectFolder = projectFolder;

      final File localFolder = new File(peassFolder, cloneProjectFolder.getName() + "_full");
      return localFolder;
   }

   public String getLatestVersion() {
      return version.getTag();
   }

   public PeASSFolders getFolders() {
      return folders;
   }

   public String getVersionOld() {
      return versionOld;
   }

   public File getPropertyFolder() {
      return propertyFolder;
   }

   public File getProjectFolder() {
      return folders.getProjectFolder();
   }
   
   public File getFullResultsVersion() {
      final File fullResultsVersion = new File(localFolder, version.getTag());
      return fullResultsVersion;
   }
}
