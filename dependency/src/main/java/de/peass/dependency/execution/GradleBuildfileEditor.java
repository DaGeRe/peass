package de.peass.dependency.execution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.parsing.GradleParseHelper;
import de.peass.dependency.execution.gradle.FindDependencyVisitor;
import de.peass.testtransformation.JUnitTestTransformer;

public class GradleBuildfileEditor {
   
   private static final Logger LOG = LogManager.getLogger(GradleBuildfileEditor.class);
   
   private final JUnitTestTransformer testTransformer;
   private final File buildfile;
   private final ProjectModules modules;
   
   public GradleBuildfileEditor(final JUnitTestTransformer testTransformer, final File buildfile, final ProjectModules modules) {
      this.testTransformer = testTransformer;
      this.buildfile = buildfile;
      this.modules = modules;
   }

   public FindDependencyVisitor addDependencies(final File tempFolder) {
      FindDependencyVisitor visitor = null;
      try {
         LOG.debug("Editing buildfile: {}", buildfile.getAbsolutePath());
         visitor = GradleParseUtil.parseBuildfile(buildfile);
         if (visitor.isUseJava() == true) {
            editGradlefileContents(tempFolder, visitor);
         } else {
            LOG.debug("Buildfile itself does not contain Java plugin, checking parent projects");
            boolean isUseJava = isParentUseJava(buildfile, modules);
            if (isUseJava) {
               editGradlefileContents(tempFolder, visitor);
            }

         }

         LOG.debug("Writing changed buildfile: {}", buildfile.getAbsolutePath());
         Files.write(buildfile.toPath(), visitor.getLines(), StandardCharsets.UTF_8);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return visitor;
   }
   
   private static boolean isParentUseJava(final File buildfile, final ProjectModules modules) throws IOException, FileNotFoundException {
      List<File> parentProjects = modules.getParents(buildfile.getParentFile());
      boolean isUseJava = false;
      for (File parentProject : parentProjects) {
         File parentBuildfile = GradleParseHelper.findGradleFile(parentProject);
         LOG.debug("Reading " + parentBuildfile);
         FindDependencyVisitor parentVisitor = GradleParseUtil.parseBuildfile(parentBuildfile);
         if (parentVisitor.isSubprojectJava()) {
            isUseJava = true;
         }
      }
      return isUseJava;
   }
   
   private void editGradlefileContents(final File tempFolder, final FindDependencyVisitor visitor) {
      if (visitor.getBuildTools() != -1) {
         GradleParseUtil.updateBuildTools(visitor);
      }

      if (visitor.getBuildToolsVersion() != -1) {
         GradleParseUtil.updateBuildToolsVersion(visitor);
      }

      addDependencies(visitor);

      addKiekerLine(tempFolder, visitor);
   }
   
   


   private void addDependencies(final FindDependencyVisitor visitor) {
      boolean isAddJunit3 = testTransformer.isJUnit3();
      if (visitor.getDependencyLine() != -1) {
         for (RequiredDependency dependency : RequiredDependency.getAll(isAddJunit3)) {
            final String dependencyGradle = "implementation '" + dependency.getGradleDependency() + "'";
            visitor.addLine(visitor.getDependencyLine() - 1, dependencyGradle);
         }
      } else {
         visitor.getLines().add("dependencies { ");
         for (RequiredDependency dependency : RequiredDependency.getAll(isAddJunit3)) {
            final String dependencyGradle = "implementation '" + dependency.getGradleDependency() + "'";
            visitor.getLines().add(dependencyGradle);
         }
         visitor.getLines().add("}");
      }
   }

   public void addKiekerLine(final File tempFolder, final FindDependencyVisitor visitor) {
      if (tempFolder != null) {
         final String javaagentArgument = new ArgLineBuilder(testTransformer, buildfile.getParentFile()).buildArglineGradle(tempFolder);
         if (visitor.getAndroidLine() != -1) {
            if (visitor.getUnitTestsAll() != -1) {
               visitor.addLine(visitor.getUnitTestsAll() - 1, javaagentArgument);
            } else if (visitor.getTestOptionsAndroid() != -1) {
               visitor.addLine(visitor.getTestOptionsAndroid() - 1, "unitTests.all{" + javaagentArgument + "}");
            } else {
               visitor.addLine(visitor.getAndroidLine() - 1, "testOptions{ unitTests.all{" + javaagentArgument + "} }");
            }
         } else {
            if (visitor.getTestLine() != -1) {
               visitor.addLine(visitor.getTestLine() - 1, javaagentArgument);
            } else {
               visitor.getLines().add("test { " + javaagentArgument + "}");
            }
         }
      }
   }
}
