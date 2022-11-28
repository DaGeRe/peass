package de.dagere.peass.execution.gradle;

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

   public GradleBuildfileEditor(final JUnitTestTransformer testTransformer, final File buildfile, final ProjectModules modules) {
      this.testTransformer = testTransformer;
      this.buildfile = buildfile;
      this.modules = modules;
   }

   public GradleBuildfileVisitor addDependencies(final File tempFolder, EnvironmentVariables env) {
      GradleBuildfileVisitor visitor = null;
      try {
         LOG.debug("Editing buildfile: {}", buildfile.getAbsolutePath());
         visitor = GradleParseUtil.parseBuildfile(buildfile, testTransformer.getConfig().getExecutionConfig());

         GradleTaskAnalyzer executor = new GradleTaskAnalyzer(buildfile.getParentFile(), testTransformer.getProjectFolder(), env);

         if (executor.isUseJava()) {
            editGradlefileContents(tempFolder, visitor, executor);
         } else {
            LOG.debug("Buildfile itself does not contain Java plugin, checking parent projects");
            boolean isUseJava = isParentUseJava(buildfile, modules);
            if (isUseJava) {
               editGradlefileContents(tempFolder, visitor, executor);
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

   private void editGradlefileContents(final File tempFolder, final GradleBuildfileVisitor visitor, GradleTaskAnalyzer taskAnalyzer) {
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

      if (testTransformer.getConfig().getExecutionConfig().isUseAnbox()){
         final String COMPILE_SDK_VERSION = "    compileSdkVersion 29";
         final String MIN_SDK_VERSION     = "        minSdkVersion 26";
         final String TARGET_SDK_VERSION  = "        targetSdkVersion 29";
         final String MULTIDEX_ENABLED    = "        multiDexEnabled = true";

         if (visitor.getCompileSdkVersion() != -1) {
            // assumption: compileSdkVersion always exists
            final int lineIndex = visitor.getCompileSdkVersion() - 1;
            visitor.getLines().set(lineIndex, COMPILE_SDK_VERSION);
         }

         if (visitor.getMinSdkVersion() != -1) {
            final int lineIndex = visitor.getMinSdkVersion() - 1;
            visitor.getLines().set(lineIndex, MIN_SDK_VERSION);
         } else {
            addLineWithinDefaultConfig(visitor, MIN_SDK_VERSION);
         }

         if (visitor.getTargetSdkVersion() != -1) {
            final int lineIndex = visitor.getTargetSdkVersion() - 1;
            visitor.getLines().set(lineIndex, TARGET_SDK_VERSION);
         } else {
            addLineWithinDefaultConfig(visitor, TARGET_SDK_VERSION);
         }

         if (visitor.getMultiDexEnabled() != -1) {
            final int lineIndex = visitor.getMultiDexEnabled() - 1;
            visitor.getLines().set(lineIndex, MULTIDEX_ENABLED);
         } else {
            addLineWithinDefaultConfig(visitor, MULTIDEX_ENABLED);
         }

         addAndroidPackagingOptions(visitor);
      }

      GradleParseUtil.removeExclusions(visitor);

      addDependencies(visitor);

      addKiekerLine(tempFolder, visitor, taskAnalyzer);
   }
   private void addAndroidPackagingOptions(final GradleBuildfileVisitor visitor) {
      String[] excludeFiles = {
         "'META-INF/DEPENDENCIES'",
         "'META-INF/LICENSE.md'",
         "'META-INF/NOTICE.md'",
         "'META-INF/jing-copying.html'",
         "'META-INF/LICENSE-notice.md'",
      };
      if (visitor.getAndroidPackagingOptions() != -1) {
         addExcludeFiles(visitor, excludeFiles);
      } else {
         visitor.addLine(visitor.getAndroidLine() - 1, "    android.packagingOptions {");
         addExcludeFiles(visitor, excludeFiles);
         int androidPackagingOptionsEnd = visitor.getAndroidLine();
         visitor.addLine(androidPackagingOptionsEnd - 1, "    }");

         visitor.setAndroidPackagingOptions(androidPackagingOptionsEnd);
      }
   }

   private void addLineWithinDefaultConfig(GradleBuildfileVisitor visitor, String textForAdding) {
      if (visitor.getDefaultConfigLine() != -1) {
         visitor.addLine(visitor.getDefaultConfigLine() - 1, textForAdding);
      } else {
         visitor.addLine(visitor.getAndroidLine() - 1, "    defaultConfig {");
         visitor.addLine(visitor.getAndroidLine() - 1, textForAdding);

         int defaultConfigEnd = visitor.getAndroidLine();
         visitor.addLine(defaultConfigEnd - 1, "    }");

         visitor.setDefaultConfigLine(defaultConfigEnd);
      }
   }

   private void addExcludeFiles(final GradleBuildfileVisitor visitor, String[] excludeFiles){
      for (String file : excludeFiles) {
         String packagingOption = "        exclude " + file;
         visitor.addLine(visitor.getAndroidLine() - 1, packagingOption);
      }
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
                  "        exclude group: '"+ "net.kieker-monitoring" + "', module: '" + "kieker'",
                  "        exclude group: '"+ "org.hamcrest" + "', module: '" + "hamcrest'", 
                  "        exclude group: '"+ "org.aspectj" + "', module: '" + "aspectjrt'", 
                  "        exclude group: '"+ "org.apache.logging.log4j" + "', module: '" + "log4j-core'", 
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

   public void addKiekerLine(final File tempFolder, final GradleBuildfileVisitor visitor, GradleTaskAnalyzer taskAnalyzer) {
      ArgLineBuilder argLineBuilder = new ArgLineBuilder(testTransformer, buildfile.getParentFile());
      addArgLine(visitor, argLineBuilder, tempFolder, taskAnalyzer);
   }

   private void addArgLine(final GradleBuildfileVisitor visitor, final ArgLineBuilder argLineBuilder, File tempFolder, GradleTaskAnalyzer taskAnalyzer) {
      if (visitor.getAndroidLine() != -1) {
         if (visitor.getUnitTestsAll() != -1) {
            visitor.addLine(visitor.getUnitTestsAll() - 1, argLineBuilder.buildArglineGradle(tempFolder));
         } else if (visitor.getTestOptionsAndroid() != -1) {
            visitor.addLine(visitor.getTestOptionsAndroid() - 1, "unitTests.all{" + argLineBuilder.buildArglineGradle(tempFolder) + "}");
         } else {
            visitor.addLine(visitor.getAndroidLine() - 1, "testOptions{ unitTests.all{" + argLineBuilder.buildArglineGradle(tempFolder) + "} }");
         }
      } else {
         enhanceTestTask(visitor, argLineBuilder, tempFolder);
      }
      enhanceIntegrationTestTask(visitor, argLineBuilder, tempFolder, taskAnalyzer);
   }

   private void enhanceIntegrationTestTask(final GradleBuildfileVisitor visitor, final ArgLineBuilder argLineBuilder,
         File tempFolder, GradleTaskAnalyzer taskAnalyzer) {
      if (visitor.getIntegrationTestLine() != -1) {
         if (visitor.getIntegrationTestSystemPropertiesLine() == -1) {
            visitor.addLine(visitor.getIntegrationTestLine() - 1, argLineBuilder.buildArglineGradle(tempFolder));
         } else {
            for (Map.Entry<String, String> entry : argLineBuilder.getGradleSystemProperties(tempFolder).entrySet()) {
               visitor.addLine(visitor.getIntegrationTestSystemPropertiesLine(), "  '" + entry.getKey() + "'             : '" + entry.getValue() + "',");
            }
            if (argLineBuilder.getJVMArgs() != null) {
               visitor.addLine(visitor.getIntegrationTestLine(), argLineBuilder.getJVMArgs());
            }
         }
      } else if (taskAnalyzer.isIntegrationTest()) {
         visitor.getLines().add("integrationTest { " + argLineBuilder.buildArglineGradle(tempFolder) + "}");
      }
   }

   private void enhanceTestTask(final GradleBuildfileVisitor visitor, final ArgLineBuilder argLineBuilder,
         File tempFolder) {
      if (visitor.getTestLine() != -1) {
         if (visitor.getTestSystemPropertiesLine() == -1) {
            visitor.addLine(visitor.getTestLine() - 1, argLineBuilder.buildArglineGradle(tempFolder));
         } else {
            for (Map.Entry<String, String> entry : argLineBuilder.getGradleSystemProperties(tempFolder).entrySet()) {
               visitor.addLine(visitor.getTestSystemPropertiesLine(), "  '" + entry.getKey() + "'             : '" + entry.getValue() + "',");
            }
            if (argLineBuilder.getJVMArgs() != null) {
               visitor.addLine(visitor.getTestLine(), argLineBuilder.getJVMArgs());
            }

         }

      } else {
         visitor.getLines().add("test { " + argLineBuilder.buildArglineGradle(tempFolder) + "}");
      }
   }
}
