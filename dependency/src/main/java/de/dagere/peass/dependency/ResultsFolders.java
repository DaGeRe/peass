package de.dagere.peass.dependency;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class ResultsFolders {
   
   private static final Logger LOG = LogManager.getLogger(ResultsFolders.class);
   
   private final File resultFolder;
   private final String projectName;
   
   public ResultsFolders(final File resultFolder, final String projectName) {
      this.resultFolder = resultFolder;
      resultFolder.mkdirs();
      this.projectName = projectName;
   }
   
   public File getDependencyFile() {
      return new File(resultFolder, "deps_" + projectName + ".json");
   }
   
   public File getExecutionFile() {
      return new File(resultFolder, "execute_" + projectName + ".json");
   }
   
   public File getCoverageSelectionFile() {
      return new File(resultFolder, "coverageSelection_" + projectName + ".json");
   }
   
   public File getViewFolder() {
      File viewFolder = new File(resultFolder, "views_" + projectName);
      viewFolder.mkdirs();
      return viewFolder;
   }
   
   public File getPropertiesFile() {
      return new File(getPropertiesFolder(), "properties.json"); 
   }
   
   public File getVersionDiffFolder(final String version) {
      File diffsFolder = new File(getVersionViewFolder(version), "diffs");
      diffsFolder.mkdirs();
      return diffsFolder;
   }
   
   public File getVersionViewFolder(final String version) {
      File versionViewFolder = new File(getViewFolder(), "view_" + version);
      versionViewFolder.mkdirs();
      return versionViewFolder;
   }
   
   public File getViewMethodDir(final String version, final TestCase testcase) {
      final File methodDir = new File(getClazzDir(version, testcase), testcase.getMethod());
      if (!methodDir.exists()) {
         boolean create = methodDir.mkdirs();
         LOG.debug("Created directory {} Success: {}", methodDir.getAbsolutePath(), create);
      } else {
         LOG.debug("Directory {} already existing", methodDir.getAbsolutePath());
      }
      return methodDir;
   }
   
   private File getClazzDir(final String version, final TestCase testcase) {
      final File viewResultsFolder = new File(getViewFolder(), "view_" + version);
      if (!viewResultsFolder.exists()) {
         viewResultsFolder.mkdir();
      }
      String clazzDirName = (testcase.getModule() != null && !testcase.getModule().equals("")) ? testcase.getModule() + ChangedEntity.MODULE_SEPARATOR + testcase.getClazz()
            : testcase.getClazz();
      final File clazzDir = new File(viewResultsFolder, clazzDirName);
      if (!clazzDir.exists()) {
         clazzDir.mkdir();
      }
      return clazzDir;
   }
   
   public File getPropertiesFolder() {
      File propertyFolder = new File(resultFolder, "properties_" + projectName);
      propertyFolder.mkdirs();
      return propertyFolder;
   }
}
