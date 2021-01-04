package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.persistence.Dependencies;
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
         GitUtils.pull(projectFolderLocal);
         final String head = GitUtils.getName("HEAD", projectFolder);
         GitUtils.goToTag(head, projectFolderLocal);
      }

      folders = new PeASSFolders(projectFolderLocal);
      viewFolder = new File(localFolder, "views");
      viewFolder.mkdir();

      propertyFolder = new File(localFolder, "properties");
   }
   
   public void execute() throws InterruptedException, IOException, JAXBException, XmlPullParserException {
      execute(new LinkedList<>());
   }

   public void execute(List<String> includes) throws InterruptedException, IOException, JAXBException, XmlPullParserException {
      final File dependencyFile = new File(localFolder, "dependencies.json");
      final VersionIteratorGit iterator = buildIterator();
      final String url = GitUtils.getURL(projectFolder);
      
      ContinuousDependencyReader dependencyReader = new ContinuousDependencyReader(version.getTag(), projectFolderLocal, dependencyFile);
      final Dependencies dependencies = dependencyReader.getDependencies(iterator, url);

      if (dependencies.getVersions().size() > 0) {
         final Set<TestCase> tests = selectIncludedTests(includes, dependencies);
         
         final File measurementFolder = measure(tests);
         
         analyzeMeasurements(measurementFolder);
      } else {
         LOG.info("No test executed - version did not contain changed tests.");
      }
   }

   private Set<TestCase> selectIncludedTests(List<String> includes, final Dependencies dependencies) throws IOException, JAXBException, JsonParseException, JsonMappingException {
      final TestChooser chooser = new TestChooser(useViews, localFolder, folders, version, 
            viewFolder, propertyFolder, threads, includes);
      final Set<TestCase> tests = chooser.getTestSet(dependencies);
      LOG.debug("Executing measurement on {}", tests);
      return tests;
   }

   private File measure(final Set<TestCase> tests) throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      final File fullResultsVersion = getFullResultsVersion();
      final ContinuousMeasurementExecutor measurementExecutor = new ContinuousMeasurementExecutor(version.getTag(), versionOld, folders, measurementConfig);
      final File measurementFolder = measurementExecutor.executeMeasurements(tests, fullResultsVersion);
      return measurementFolder;
   }

   private void analyzeMeasurements(final File measurementFolder) throws InterruptedException, IOException, JsonGenerationException, JsonMappingException {
      final File changefile = new File(localFolder, "changes.json");
      AnalyseOneTest.setResultFolder(new File(localFolder, version.getTag() + "_graphs"));
      final ProjectStatistics statistics = new ProjectStatistics();
      final AnalyseFullData afd = new AnalyseFullData(changefile, statistics);
      afd.analyseFolder(measurementFolder);
      Constants.OBJECTMAPPER.writeValue(new File(localFolder, "statistics.json"), statistics);
   }

   private VersionIteratorGit buildIterator() {
      versionOld = GitUtils.getName("HEAD~1", projectFolderLocal);
      version = new GitCommit(GitUtils.getName("HEAD", projectFolderLocal), "", "", "");
      
      final List<GitCommit> entries = new LinkedList<>();
      final GitCommit prevCommit = new GitCommit(versionOld, "", "", "");
      entries.add(prevCommit);
      entries.add(new GitCommit(version.getTag(), "", "", ""));
      final VersionIteratorGit iterator = new VersionIteratorGit(projectFolderLocal, entries, prevCommit);
      return iterator;
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
