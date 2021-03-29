package de.peass.dependency.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.plexus.util.IOUtil;

import de.dagere.kopeme.parsing.GradleParseHelper;
import de.peass.dependency.execution.gradle.AndroidVersionUtil;
import de.peass.dependency.execution.gradle.FindDependencyVisitor;
import de.peass.testtransformation.JUnitTestTransformer;

public class GradleParseUtil {

   private static final Logger LOG = LogManager.getLogger(GradleParseUtil.class);

   public static void writeInitGradle(final File init) {
      if (!init.exists()) {
         try (FileWriter fw = new FileWriter(init)) {
            final PrintWriter pw = new PrintWriter(fw);
            pw.write("allprojects{");
            pw.write(" repositories {");
            pw.write("  mavenLocal();");
            pw.write("  maven { url 'https://maven.google.com' };");
            fw.write(" }");
            fw.write("}");
            pw.flush();
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }

   public static FindDependencyVisitor setAndroidTools(final File buildfile) {
      FindDependencyVisitor visitor = null;
      try {
         LOG.debug("Editing: {}", buildfile);
         visitor = parseBuildfile(buildfile);
         final List<String> gradleFileContents = Files.readAllLines(Paths.get(buildfile.toURI()));

         if (visitor.getBuildTools() != -1) {
            updateBuildTools(visitor);
         }

         if (visitor.getBuildToolsVersion() != -1) {
            updateBuildToolsVersion(visitor);
         }

         Files.write(buildfile.toPath(), gradleFileContents, StandardCharsets.UTF_8);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return visitor;
   }

   public static FindDependencyVisitor parseBuildfile(final File buildfile) throws IOException, FileNotFoundException {

      try (FileInputStream inputStream = new FileInputStream(buildfile)) {
         final AstBuilder builder = new AstBuilder();
         final List<ASTNode> nodes = builder.buildFromString(IOUtil.toString(inputStream, "UTF-8"));

         FindDependencyVisitor visitor = new FindDependencyVisitor(buildfile);
         for (final ASTNode node : nodes) {
            node.visit(visitor);
         }
         return visitor;
      }
   }

   public static void updateBuildTools(final FindDependencyVisitor visitor) {
      final int lineIndex = visitor.getBuildTools() - 1;
      final String versionLine = visitor.getLines().get(lineIndex).trim().replaceAll("'", "").replace("\"", "");
      final String versionString = versionLine.split(":")[1].trim();
      if (AndroidVersionUtil.isLegelBuildTools(versionString)) {
         final String runningVersion = AndroidVersionUtil.getRunningVersion(versionString);
         if (runningVersion != null) {
            visitor.getLines().set(lineIndex, "'buildTools': '" + runningVersion + "'");
         } else {
            visitor.setHasVersion(false);
         }
      }
   }

   public static void updateBuildToolsVersion(final FindDependencyVisitor visitor) {
      final int lineIndex = visitor.getBuildToolsVersion() - 1;
      final String versionLine = visitor.getLines().get(lineIndex).trim().replaceAll("'", "").replace("\"", "");
      final String versionString = versionLine.split(" ")[1].trim();
      if (AndroidVersionUtil.isLegalBuildToolsVersion(versionString)) {
         LOG.info(lineIndex + " " + versionLine);
         final String runningVersion = AndroidVersionUtil.getRunningVersion(versionString);
         if (runningVersion != null) {
            visitor.getLines().set(lineIndex, "buildToolsVersion " + runningVersion);
         } else {
            visitor.setHasVersion(false);
         }
      }
   }

   public static FindDependencyVisitor addDependencies(final JUnitTestTransformer testTransformer, final File buildfile, final File tempFolder, final ProjectModules modules) {
      FindDependencyVisitor visitor = null;
      try {
         LOG.debug("Editing buildfile: {}", buildfile.getAbsolutePath());
         visitor = parseBuildfile(buildfile);
         if (visitor.isUseJava() == true) {
            editGradlefileContents(testTransformer, tempFolder, visitor);
         } else {
            LOG.debug("Buildfile itself does not contain Java plugin, checking parent projects");
            boolean isUseJava = isParentUseJava(buildfile, modules);
            if (isUseJava) {
               editGradlefileContents(testTransformer, tempFolder, visitor);
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
         FindDependencyVisitor parentVisitor = parseBuildfile(parentBuildfile);
         if (parentVisitor.isSubprojectJava()) {
            isUseJava = true;
         }
      }
      return isUseJava;
   }

   private static void editGradlefileContents(final JUnitTestTransformer testTransformer, final File tempFolder, final FindDependencyVisitor visitor) {
      if (visitor.getBuildTools() != -1) {
         updateBuildTools(visitor);
      }

      if (visitor.getBuildToolsVersion() != -1) {
         updateBuildToolsVersion(visitor);
      }

      addDependencies(visitor);

      addKiekerLine(testTransformer, tempFolder, visitor);
   }

   private static void addDependencies(final FindDependencyVisitor visitor) {
      if (visitor.getDependencyLine() != -1) {
         for (RequiredDependency dependency : RequiredDependency.getAll(false)) {
            final String dependencyGradle = "implementation '" + dependency.getGradleDependency() + "'";
            visitor.addLine(visitor.getDependencyLine() - 1, dependencyGradle);
         }
      } else {
         visitor.getLines().add("dependencies { ");
         for (RequiredDependency dependency : RequiredDependency.getAll(false)) {
            final String dependencyGradle = "implementation '" + dependency.getGradleDependency() + "'";
            visitor.getLines().add(dependencyGradle);
         }
         visitor.getLines().add("}");
      }
   }

   public static void addKiekerLine(final JUnitTestTransformer testTransformer, final File tempFolder, final FindDependencyVisitor visitor) {
      if (tempFolder != null) {
         final String javaagentArgument = new ArgLineBuilder(testTransformer).buildArglineGradle(tempFolder);
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

   public static ProjectModules getModules(final File projectFolder) throws FileNotFoundException, IOException {
      final File settingsFile = new File(projectFolder, "settings.gradle");
      final List<File> modules = new LinkedList<>();
      if (settingsFile.exists()) {
         try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
               parseModuleLine(projectFolder, modules, line);
            }
         }
      } else {
         LOG.debug("settings-file {} not found", settingsFile);
      }
      modules.add(projectFolder);
      return new ProjectModules(modules);
   }

   private static void parseModuleLine(final File projectFolder, final List<File> modules, final String line) {
      final String[] splitted = line.replaceAll("[ ,]+", " ").split(" ");
      if (splitted[0].equals("include")) {
         for (int candidateIndex = 1; candidateIndex < splitted.length; candidateIndex++) {
            final String candidate = splitted[candidateIndex].substring(1, splitted[candidateIndex].length() - 1);
            final File module = new File(projectFolder, candidate.replace(':', File.separatorChar));
            if (module.exists()) {
               modules.add(module);
            } else {
               LOG.error(line + " not found! Was looking in " + module.getAbsolutePath());
            }
         }

      }
   }
}
