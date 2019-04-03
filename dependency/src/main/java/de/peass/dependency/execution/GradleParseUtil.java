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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.plexus.util.IOUtil;

public class GradleParseUtil {

   private static final Logger LOG = LogManager.getLogger(GradleParseUtil.class);
   private static Map<Integer, String> versions = new LinkedHashMap<>();
   private static Set<String> acceptedVersion = new HashSet<>();

   static {
      final ClassLoader classLoader = GradleParseUtil.class.getClassLoader();
      final File versionFile = new File(classLoader.getResource("versions.txt").getFile());
      if (versionFile.exists()) {
         try {
            final List<String> runningAndroidVersions = Files.readAllLines(Paths.get(versionFile.toURI()));
            for (final String line : runningAndroidVersions) {
               final String version = line.substring(line.indexOf(';') + 1);
               versions.put(getMajorVersion(version), version);
               acceptedVersion.add(version);
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
      } else {
         LOG.error("No version file existing!");
      }

      final File gradle = new File(System.getenv("user.home"), ".gradle");
      if (!gradle.exists()) {
         gradle.mkdir();
      }
   }

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

   static class FindDependencyVisitor extends CodeVisitorSupport {

      int dependencyLine = -1;
      int testLine = -1;
      int androidLine = -1;
      int testOptionsAndroid = -1;
      int unitTestsAll = -1;
      int buildTools = -1;
      int buildToolsVersion = -1;
      boolean useJava = false;
      boolean hasVersion = true;

      @Override
      public void visitMethodCallExpression(final MethodCallExpression call) {
         // LOG.info("Call: " + call.getMethodAsString());
         if (call != null && call.getMethodAsString() != null) {
            // System.out.println(call.getMethodAsString());
            if (call.getMethodAsString().equals("apply")) {
               final String text = call.getArguments().getText();
               if (text.contains("plugin:java") || text.contains("plugin:com.android.library") || text.contains("plugin:com.android.application")) {
                  useJava = true;
               }
            } else if (call.getMethodAsString().equals("dependencies")) {
               // System.out.println(call);
               dependencyLine = call.getLastLineNumber();
            } else if (call.getMethodAsString().equals("test")) {
               testLine = call.getLastLineNumber();
            } else if (call.getMethodAsString().equals("android")) {
               androidLine = call.getLastLineNumber();
            } else if (call.getMethodAsString().equals("testOptions")) {
               testOptionsAndroid = call.getLastLineNumber();
            } else if (call.getMethodAsString().equals("unitTests.all")) {
               unitTestsAll = call.getLastLineNumber();
            } else if (call.getMethodAsString().equals("buildToolsVersion")) {
               buildToolsVersion = call.getLastLineNumber();
            }
         }

         // LOG.info("Android: " + androidLine);
         super.visitMethodCallExpression(call);
      }

      @Override
      public void visitMapEntryExpression(final MapEntryExpression expression) {
         final String name = expression.getKeyExpression().getText();
         if (name.equals("buildTools")) {
            buildTools = expression.getLineNumber();
         }
         super.visitMapEntryExpression(expression);
      }

      public boolean isAndroid() {
         return buildTools != -1 || androidLine != -1 || buildToolsVersion != -1;
      }

      public void setHasVersion(final boolean hasVersion) {
         this.hasVersion = hasVersion;
      }

      public boolean hasVersion() {
         return hasVersion;
      }

   }

   public static int getMajorVersion(final String versionString) {
      final int dotIndex = versionString.indexOf('.');
      if (dotIndex != -1) {
         final String part = versionString.substring(0, dotIndex);
         final int parsed = Integer.parseInt(part);
         return parsed;
      } else {
         return Integer.parseInt(versionString);
      }

   }

   public static FindDependencyVisitor setAndroidTools(final File buildfile) {
      FindDependencyVisitor visitor = null;
      try {
         LOG.debug("Editing: {}", buildfile);
         final AstBuilder builder = new AstBuilder();
         final List<ASTNode> nodes = builder.buildFromString(IOUtil.toString(new FileInputStream(buildfile), "UTF-8"));
         final List<String> gradleFileContents = Files.readAllLines(Paths.get(buildfile.toURI()));
         visitor = new FindDependencyVisitor();
         for (final ASTNode node : nodes) {
            node.visit(visitor);
         }

         if (visitor.buildTools != -1) {
            final String versionLine = gradleFileContents.get(visitor.buildTools - 1).trim().replaceAll("'", "").replace("\"", "");
            final String part = versionLine.split(":")[1].trim();
            if (!acceptedVersion.contains(part) && !part.equals("rootProject.buildToolsVersion") && !part.equals("rootProject.compileSdkVersion")
                  && !part.equals("androidCompileSdkVersion.toInteger")) {
               final int version = getMajorVersion(part);
               final String runningVersion = versions.get(version);
               if (runningVersion != null) {
                  gradleFileContents.set(visitor.buildTools - 1, "'buildTools': '" + runningVersion + "'");
               } else {
                  visitor.setHasVersion(false);
               }
            }
         }

         if (visitor.buildToolsVersion != -1) {
            final String versionLine = gradleFileContents.get(visitor.buildToolsVersion - 1).trim().replaceAll("'", "").replace("\"", "");
            final String part = versionLine.split(" ")[1].trim();
            if (!acceptedVersion.contains(part) && !part.equals("rootProject.buildToolsVersion") && !part.equals("rootProject.compileSdkVersion")
                  && !part.equals("androidBuildToolsVersion") && !part.equals("androidCompileSdkVersion.toInteger()")) {
               LOG.info(versionLine);
               final int version = getMajorVersion(part);
               final String runningVersion = versions.get(version);
               if (runningVersion != null) {
                  gradleFileContents.set(visitor.buildToolsVersion - 1, "buildToolsVersion " + runningVersion);
               } else {
                  visitor.setHasVersion(false);
               }
            }
         }

         Files.write(buildfile.toPath(), gradleFileContents, StandardCharsets.UTF_8);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return visitor;
   }

   public static FindDependencyVisitor addDependency(final File buildfile, final String dependency, final String tempFolder) {
      FindDependencyVisitor visitor = null;
      try {
         final AstBuilder builder = new AstBuilder();
         final List<ASTNode> nodes = builder.buildFromString(IOUtil.toString(new FileInputStream(buildfile), "UTF-8"));
         visitor = new FindDependencyVisitor();
         for (final ASTNode node : nodes) {
            node.visit(visitor);
         }
         final List<String> gradleFileContents = Files.readAllLines(Paths.get(buildfile.toURI()));
         if (visitor.useJava == true) {
            final String dependencyTest = "testCompile '" + dependency + "'"; // TODO testImplementation vs. other name based on gradle version
            if (visitor.dependencyLine != -1) {
               gradleFileContents.add(visitor.dependencyLine - 1, dependencyTest);
            } else {
               gradleFileContents.add("dependencies { " + dependencyTest + "}");
            }
            if (tempFolder != null) {
               final String javaagentArgument = "jvmArgs=[\"" + MavenTestExecutor.JAVA_AGENT + ":" + MavenTestExecutor.KIEKER_FOLDER_GRADLE + "\",\"" + tempFolder
                     + "\"]";
               if (visitor.androidLine != -1) {
                  if (visitor.unitTestsAll != -1) {
                     gradleFileContents.add(visitor.unitTestsAll - 1, javaagentArgument);
                  } else if (visitor.testOptionsAndroid != -1) {
                     gradleFileContents.add(visitor.testOptionsAndroid - 1, "unitTests.all{" + javaagentArgument + "}");
                  } else {
                     gradleFileContents.add(visitor.androidLine - 1, "testOptions{ unitTests.all{" + javaagentArgument + "} }");
                  }
               } else {
                  if (visitor.testLine != -1) {
                     gradleFileContents.add(visitor.dependencyLine - 1, javaagentArgument);
                  } else {
                     gradleFileContents.add("test { " + javaagentArgument + "}");
                  }
               }
            }
            if (visitor.buildTools != -1) {
               final String versionLine = gradleFileContents.get(visitor.buildTools - 1).trim().replaceAll("'", "").replace("\"", "");
               final String part = versionLine.split(":")[1].trim();
               if (!acceptedVersion.contains(part) && !part.equals("rootProject.buildToolsVersion") && !part.equals("rootProject.compileSdkVersion")
                     && !part.equals("androidCompileSdkVersion.toInteger")) {
                  final int version = getMajorVersion(part);
                  final String runningVersion = versions.get(version);
                  if (runningVersion != null) {
                     gradleFileContents.set(visitor.buildTools - 1, "'buildTools': '" + runningVersion + "'");
                  }
               }
            }

            if (visitor.buildToolsVersion != -1) {
               final String versionLine = gradleFileContents.get(visitor.buildToolsVersion - 1).trim().replaceAll("'", "").replace("\"", "");
               final String part = versionLine.split(" ")[1].trim();
               if (!acceptedVersion.contains(part) && !part.equals("rootProject.buildToolsVersion") && !part.equals("rootProject.compileSdkVersion")
                     && !part.equals("androidBuildToolsVersion") && !part.equals("androidCompileSdkVersion.toInteger()")) {
                  LOG.info(versionLine);
                  final int version = getMajorVersion(part);
                  final String runningVersion = versions.get(version);
                  if (runningVersion != null) {
                     gradleFileContents.set(visitor.buildToolsVersion - 1, "buildToolsVersion " + runningVersion);
                  }
               }
            }
         }

         Files.write(buildfile.toPath(), gradleFileContents, StandardCharsets.UTF_8);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return visitor;
   }

   public static List<File> getModules(final File projectFolder) throws FileNotFoundException, IOException {
      final File settingsFile = new File(projectFolder, "settings.gradle");
      final List<File> modules = new LinkedList<>();
      if (settingsFile.exists()) {
         try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
               final String[] splitted = line.split(" ");
               if (splitted.length == 2 && splitted[0].equals("include")) {
                  final String candidate = splitted[1].substring(2, splitted[1].length() - 1);
                  final File module = new File(projectFolder, candidate.replace(':', File.separatorChar));
                  if (module.exists()) {
                     modules.add(module);
                  } else {
                     LOG.error(line + " not found!");
                  }
               }
            }
         }
      } else {
         LOG.debug("settings-file {} not found", settingsFile);
         modules.add(projectFolder);
      }
      return modules;
   }
}
