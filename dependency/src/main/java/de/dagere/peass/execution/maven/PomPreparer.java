package de.dagere.peass.execution.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.moduleinfo.ModuleInfoEditor;
import de.dagere.peass.execution.kieker.ArgLineBuilder;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

public class PomPreparer {
   
   private static final Logger LOG = LogManager.getLogger(PomPreparer.class);
   
   private final TestTransformer testTransformer;
   private final ProjectModules modules;
   private final PeassFolders folders;
   private File lastTmpFile;
   private Charset lastEncoding;
   
   public PomPreparer(final TestTransformer testTransformer, final ProjectModules modules, final PeassFolders folders) {
      this.testTransformer = testTransformer;
      this.modules = modules;
      this.folders = folders;
   }

   public void preparePom() {
      try {
         lastTmpFile = Files.createTempDirectory(folders.getKiekerTempFolder().toPath(), "kiekerTemp").toFile();
         OnePomPreparer preparer = new OnePomPreparer(testTransformer);
         for (final File module : modules.getModules()) {
            lastEncoding = preparer.editOneBuildfile(true, new File(module, "pom.xml"), lastTmpFile);
            final File potentialModuleFile = new File(module, "src/main/java/module-info.java");
            LOG.debug("Checking {}", potentialModuleFile.getAbsolutePath());
            if (potentialModuleFile.exists()) {
               ModuleInfoEditor.addKiekerRequires(potentialModuleFile);
            }
         }
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   
   
//   public File getLastTmpFile() {
//      return lastTmpFile;
//   }

   public Charset getLastEncoding() {
      return lastEncoding;
   }
}
