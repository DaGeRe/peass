package de.dagere.peass.execution.gradle;

import static de.dagere.peass.execution.gradle.GradleParseUtil.createTextForAdding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.parsing.GradleParseHelper;
import de.dagere.peass.execution.kieker.ArgLineBuilder;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.execution.utils.RequiredDependency;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.JUnitVersions;

public class GradleBuildfileEditor {

   private static final Logger LOG = LogManager.getLogger(GradleBuildfileEditor.class);

   private final JUnitTestTransformer testTransformer;
   private final File buildfile;
   private final ProjectModules modules;
   private final GradleTaskAnalyzer taskAnalyzer;

   public GradleBuildfileEditor(final JUnitTestTransformer testTransformer, final File buildfile, final ProjectModules modules, GradleTaskAnalyzer taskAnalyzer) {
      this.testTransformer = testTransformer;
      this.buildfile = buildfile;
      this.modules = modules;
      this.taskAnalyzer = taskAnalyzer;
   }

   public GradleBuildfileVisitor addDependencies(final File tempFolder, EnvironmentVariables env) {
      GradleBuildfileVisitor visitor = null;
      try {
         LOG.debug("Editing buildfile: {}", buildfile.getAbsolutePath());
         visitor = GradleParseUtil.parseBuildfile(buildfile, testTransformer.getConfig().getExecutionConfig());

         if (taskAnalyzer.isUseJava()) {
            editGradlefileContents(tempFolder, visitor);
         } else {
            LOG.debug("Buildfile itself does not contain Java plugin, checking parent projects");
            boolean isUseJava = isParentUseJava(buildfile, modules);
            if (isUseJava) {
               editGradlefileContents(tempFolder, visitor);
            } else {
               LOG.info("Parent buildfile did not contain java; not changing buildfile");
            }
         }

         LOG.debug("Writing changed buildfile: {}", buildfile.getAbsolutePath());
         Files.write(buildfile.toPath(), visitor.getLines(), StandardCharsets.UTF_8);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return visitor;
   }

   private boolean isParentUseJava(final File buildfile, final ProjectModules modules) throws IOException, FileNotFoundException {
      List<File> parentProjects = modules.getParents(buildfile.getParentFile());
      boolean isUseJava = false;
      for (File parentProject : parentProjects) {
         File parentBuildfile = GradleParseHelper.findGradleFile(parentProject);
         LOG.debug("Reading " + parentBuildfile);
         GradleBuildfileVisitor parentVisitor = GradleParseUtil.parseBuildfile(parentBuildfile, testTransformer.getConfig().getExecutionConfig());
         if (parentVisitor.isSubprojectJava()) {
            isUseJava = true;
         }
      }
      return isUseJava;
   }

   private void editGradlefileContents(final File tempFolder, final GradleBuildfileVisitor visitor) {
      if (visitor.getBuildTools() != -1) {
         GradleParseUtil.updateBuildTools(visitor);
      }

      if (visitor.getBuildToolsVersion() != -1) {
         GradleParseUtil.updateBuildToolsVersion(visitor);
      }

      if (taskAnalyzer.isUseSpringBoot()) {
         LOG.info("Adding spring boot ext");
         GradleParseUtil.addJUnitVersionSpringBoot(visitor);
      } else {
         LOG.info("Did not find spring boot");
      }

      GradleParseUtil.removeExclusions(visitor);

      addDependencies(visitor);

      if ((visitor.getTestTaskProperties() != null && visitor.getTestTaskProperties().getPropertiesLine() != -1) 
            || (visitor.getIntegrationTestTaskProperties() != null && visitor.getIntegrationTestTaskProperties().getPropertiesLine() != -1)) {
         GradleParseUtil.updateExecutionMode(visitor);
      }

      addKiekerLine(tempFolder, visitor);
   }

   private void addDependencies(final GradleBuildfileVisitor visitor) {
      JUnitVersions versions = testTransformer.getJUnitVersions();
      boolean isExcludeLog4j = testTransformer.getConfig().getExecutionConfig().isExcludeLog4jSlf4jImpl();
      boolean isAnbox = testTransformer.getConfig().getExecutionConfig().isUseAnbox();
      if (visitor.getDependencyLine() != -1) {
         for (RequiredDependency dependency : RequiredDependency.getAll(versions)) {
            final String dependencyGradle;
            // TODO Find a solution to include the case for isExcludeLog4j and isAnbox.
            // In the future there could be a case where isExcludeLog4j and isAnbox is used.
            // Right now it's only isExcludeLog4j or isAnbox.
            if (isExcludeLog4j && dependency.getMavenDependency().getArtifactId().contains("kopeme")) {
               String excludeString = "{ exclude group: '" + MavenPomUtil.LOG4J_GROUPID + "', module: '" + MavenPomUtil.LOG4J_SLF4J_IMPL_ARTIFACTID + "' }";
               dependencyGradle = "implementation ('" + dependency.getGradleDependency() + "') " + excludeString;
            } else if (isAnbox && dependency.getGradleDependency().contains("kopeme")) {
               String[] excludes = {
                     "    implementation ('" + dependency.getGradleDependency() + "') {",
                     "        exclude group: '" + "net.kieker-monitoring" + "', module: '" + "kieker'",
                     "        exclude group: '" + "org.hamcrest" + "', module: '" + "hamcrest'",
                     "        exclude group: '" + "org.aspectj" + "', module: '" + "aspectjrt'",
                     "        exclude group: '" + "org.apache.logging.log4j" + "', module: '" + "log4j-core'",
                     "    }",
               };
               for (String line : excludes) {
                  visitor.addLine(visitor.getDependencyLine() - 1, line);
               }
               continue;
            } else {
               dependencyGradle = "implementation '" + dependency.getGradleDependency() + "'";
            }
            visitor.addLine(visitor.getDependencyLine() - 1, dependencyGradle);
         }
         if (testTransformer.getConfig().getExecutionConfig().isUseAnbox()) {
            visitor.addLine(visitor.getDependencyLine() - 1, "    androidTestImplementation 'androidx.test:rules:1.4.0'");
         }
      } else {
         visitor.getLines().add("dependencies { ");
         for (RequiredDependency dependency : RequiredDependency.getAll(versions)) {
            final String dependencyGradle = "implementation '" + dependency.getGradleDependency() + "'";
            visitor.getLines().add(dependencyGradle);
         }
         if (testTransformer.getConfig().getExecutionConfig().isUseAnbox()) {
            visitor.getLines().add("   androidTestImplementation 'androidx.test:rules:1.4.0'");
         }
         visitor.getLines().add("}");
      }

      if (testTransformer.getConfig().getExecutionConfig().isExcludeLog4jToSlf4j() && visitor.getAllConfigurationsLine() != -1) {
         String excludeString = "exclude group: '" + MavenPomUtil.LOG4J_GROUPID + "', module: '" + MavenPomUtil.LOG4J_TO_SLF4J_ARTIFACTID + "'";
         visitor.addLine(visitor.getAllConfigurationsLine() + 1, excludeString);
      }
   }

   public void addKiekerLine(final File tempFolder, final GradleBuildfileVisitor visitor) {
      ArgLineBuilder argLineBuilder = new ArgLineBuilder(testTransformer, buildfile.getParentFile());
      addArgLine(visitor, argLineBuilder, tempFolder);
   }

   private void addArgLine(final GradleBuildfileVisitor visitor, final ArgLineBuilder argLineBuilder, File tempFolder) {
      if (visitor.getAndroidLine() != -1) {
         if (visitor.getUnitTestsAll() != -1) {
            visitor.addLine(visitor.getUnitTestsAll() - 1, argLineBuilder.buildSystemPropertiesGradle(tempFolder));
         } else if (visitor.getTestOptionsAndroid() != -1) {
            visitor.addLine(visitor.getTestOptionsAndroid() - 1, "unitTests.all{" + argLineBuilder.buildSystemPropertiesGradle(tempFolder) + "}");
         } else {
            visitor.addLine(visitor.getAndroidLine() - 1, "testOptions{ unitTests.all{" + argLineBuilder.buildSystemPropertiesGradle(tempFolder) + "} }");
         }
      } else {
         enhanceTestTask(visitor, argLineBuilder, tempFolder);
      }
      enhanceIntegrationTestTask(visitor, argLineBuilder, tempFolder);
   }

   private void enhanceIntegrationTestTask(final GradleBuildfileVisitor visitor, final ArgLineBuilder argLineBuilder,
         File tempFolder) {
      if (visitor.getIntegrationTestLine() != -1) {
         if (visitor.getIntegrationTestTaskProperties().getPropertiesLine() == -1) {
            visitor.addLine(visitor.getIntegrationTestLine() - 1, argLineBuilder.buildSystemPropertiesGradle(tempFolder));
         } else {
            for (Map.Entry<String, String> entry : argLineBuilder.getGradleSystemProperties(tempFolder).entrySet()) {
               String addedText = createTextForAdding(entry.getKey(), entry.getValue(), visitor.getIntegrationTestTaskProperties().isSystemPropertiesBlock());
               visitor.addLine(visitor.getIntegrationTestTaskProperties().getPropertiesLine(), addedText);
            }
         }
         TestTaskParser integrationTestTaskProperties = visitor.getIntegrationTestTaskProperties();
         adaptTask(visitor, argLineBuilder, integrationTestTaskProperties, visitor.getIntegrationTestLine() - 1);
      } else if (taskAnalyzer.isIntegrationTest()) {
         addTestPhaseBlock(visitor, argLineBuilder, tempFolder, "integrationTest {");

         if (visitor.getIntegrationTestTaskProperties() != null) {
            TestTaskParser integrationTestTaskProperties = visitor.getIntegrationTestTaskProperties();
            adaptTask(visitor, argLineBuilder, integrationTestTaskProperties, visitor.getLines().size() - 2);
         } else {
            adaptTask(visitor, argLineBuilder, null, visitor.getLines().size() - 2);
         }
      }
   }

   private void addTestPhaseBlock(final GradleBuildfileVisitor visitor, final ArgLineBuilder argLineBuilder, final File tempFolder, final String blockStart) {
      visitor.addLine(visitor.getLines().size(), blockStart);
      visitor.addLine(visitor.getLines().size(), argLineBuilder.buildSystemPropertiesGradle(tempFolder));
      visitor.addLine(visitor.getLines().size(), "}");
   }

   private void adaptTask(final GradleBuildfileVisitor visitor, final ArgLineBuilder argLineBuilder, final TestTaskParser testTaskParser, final int testTaskLine) {
      if (argLineBuilder.getJVMArgs() != null) {
         if (testTaskParser != null && testTaskParser.getJvmArgsLine() != -1) {
            String taskWithoutQuotations = testTaskParser.getTestJvmArgsText().substring(1, testTaskParser.getTestJvmArgsText().length() - 1);
            String testJvmArgsText = "'" + // args should start by ' 
                  taskWithoutQuotations 
                  .replace(", ", ",") // There should be no spaces inside the args
                  .replace(",", "','") // Args should be separated by ','
                  + "'"; // Args should be finished by '
            String adaptedText = argLineBuilder.getJVMArgs(testJvmArgsText);
            visitor.getLines().set(testTaskParser.getJvmArgsLine() - 1, adaptedText);
         } else {
            visitor.addLine(testTaskLine, argLineBuilder.getJVMArgs());
         }
      }
      if (testTaskParser != null && testTaskParser.getMaxHeapSizeLine() != -1 && testTransformer.getConfig().getExecutionConfig().getXmx() != null) {
         visitor.getLines().set(testTaskParser.getMaxHeapSizeLine() -1, "    maxHeapSize = \"" + testTransformer.getConfig().getExecutionConfig().getXmx() + "\"");
      }
   }

   private void enhanceTestTask(final GradleBuildfileVisitor visitor, final ArgLineBuilder argLineBuilder,
         File tempFolder) {
      if (visitor.getTestLine() != -1) {
         if (visitor.getTestTaskProperties().getPropertiesLine() == -1) {
            visitor.addLine(visitor.getTestLine() - 1, argLineBuilder.buildSystemPropertiesGradle(tempFolder));
         } else {
            for (Map.Entry<String, String> entry : argLineBuilder.getGradleSystemProperties(tempFolder).entrySet()) {
               String addedText = createTextForAdding(entry.getKey(), entry.getValue(), visitor.getTestTaskProperties().isSystemPropertiesBlock());
               visitor.addLine(visitor.getTestTaskProperties().getPropertiesLine(), addedText);
            }
         }
         TestTaskParser testTaskProperties = visitor.getTestTaskProperties();
         adaptTask(visitor, argLineBuilder, testTaskProperties, visitor.getTestLine() - 1);

      } else {
         addTestPhaseBlock(visitor, argLineBuilder, tempFolder, "test {");

         if (visitor.getTestTaskProperties() != null) {
            TestTaskParser testTaskProperties = visitor.getTestTaskProperties();
            adaptTask(visitor, argLineBuilder, testTaskProperties, visitor.getLines().size() - 2);
         } else {
            adaptTask(visitor, argLineBuilder, null, visitor.getLines().size() - 2);
         }

      }
   }
}
