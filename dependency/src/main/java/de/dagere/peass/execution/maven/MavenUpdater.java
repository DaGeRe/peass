package de.dagere.peass.execution.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.execution.MavenPomUtil;
import de.dagere.peass.dependency.execution.PomJavaUpdater;
import de.dagere.peass.dependency.execution.ProjectModules;

public class MavenUpdater {
   
   private static final Logger LOG = LogManager.getLogger(MavenUpdater.class);
   
   private final PeASSFolders folders;
   private final ProjectModules modules;
   private final MeasurementConfiguration measurementConfig;
   
   public MavenUpdater(final PeASSFolders folders, final ProjectModules modules, final MeasurementConfiguration measurementConfig) {
      this.folders = folders;
      this.modules = modules;
      this.measurementConfig = measurementConfig;
   }

   public void updateJava() throws FileNotFoundException, IOException, XmlPullParserException {
      final File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      if (measurementConfig.isRemoveSnapshots()) {
         MavenPomUtil.cleanSnapshotDependencies(pomFile);
      }
      PomJavaUpdater.fixCompilerVersion(pomFile);
      for (File module : modules.getModules()) {
         final File pomFileModule = new File(module, "pom.xml");
         if (measurementConfig.isRemoveSnapshots()) {
            MavenPomUtil.cleanSnapshotDependencies(pomFileModule);
         }
         PomJavaUpdater.fixCompilerVersion(pomFileModule);
      }
   }
}
