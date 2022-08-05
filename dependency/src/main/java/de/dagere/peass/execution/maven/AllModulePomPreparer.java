package de.dagere.peass.execution.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.moduleinfo.ModuleInfoEditor;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

public class AllModulePomPreparer {
   
   private static final Logger LOG = LogManager.getLogger(AllModulePomPreparer.class);
   
   private final TestTransformer testTransformer;
   private final ProjectModules modules;
   private final PeassFolders folders;
   private File lastTmpFile;
   private Charset lastEncoding;
   
   public AllModulePomPreparer(final TestTransformer testTransformer, final ProjectModules modules, final PeassFolders folders) {
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

   public Charset getLastEncoding() {
      return lastEncoding;
   }
}
