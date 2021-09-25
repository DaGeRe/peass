package de.dagere.peass.execution.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.execution.PomJavaUpdater;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.dependency.execution.pom.SnapshotRemoveUtil;

public class MavenUpdater {
   
   private static final Logger LOG = LogManager.getLogger(MavenUpdater.class);
   
   private final PeassFolders folders;
   private final ProjectModules modules;
   private final MeasurementConfiguration measurementConfig;
   
   public MavenUpdater(final PeassFolders folders, final ProjectModules modules, final MeasurementConfiguration measurementConfig) {
      this.folders = folders;
      this.modules = modules;
      this.measurementConfig = measurementConfig;
   }

   public void updateJava() throws FileNotFoundException, IOException, XmlPullParserException {
      final File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      if (measurementConfig.getExecutionConfig().isRemoveSnapshots()) {
         SnapshotRemoveUtil.cleanSnapshotDependencies(pomFile);
      }
      PomJavaUpdater.fixCompilerVersion(pomFile);
      for (File module : modules.getModules()) {
         final File pomFileModule = new File(module, "pom.xml");
         if (measurementConfig.getExecutionConfig().isRemoveSnapshots()) {
            SnapshotRemoveUtil.cleanSnapshotDependencies(pomFileModule);
         }
         PomJavaUpdater.fixCompilerVersion(pomFileModule);
      }
   }
}
