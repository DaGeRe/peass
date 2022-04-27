package de.dagere.peass.execution.maven.pom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.execution.utils.RequiredDependency;
import de.dagere.peass.utils.StreamGobbler;

public class MavenPomUtil {

   public static final String LOG4J_GROUPID = "org.apache.logging.log4j";
   public static final String LOG4J_SLF4J_IMPL_ARTIFACTID = "log4j-slf4j-impl";
   public static final String LOG4J_TO_SLF4J_ARTIFACTID = "log4j-to-slf4j";
   public static final String KOPEME_VERSION = "1.1.1";
   public static final String KIEKER_VERSION = "1.15.1";
   public static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
   public static final String SUREFIRE_ARTIFACTID = "maven-surefire-plugin";
   public static final String COMPILER_ARTIFACTID = "maven-compiler-plugin";

   public static final String COMPILER_PLUGIN_VERSION = "3.10.1";

   public static final String JUPITER_VERSION = "5.8.2";

   private static final Logger LOG = LogManager.getLogger(MavenPomUtil.class);

   public static void cleanType(final File pomFile) {
      try {
         final Model model;
         try (FileInputStream inputStream = new FileInputStream(pomFile)) {
            final MavenXpp3Reader reader = new MavenXpp3Reader();
            model = reader.read(inputStream);
         }
         if (model.getPackaging().equals("pom") && model.getModules() == null || model.getModules().size() == 0) {
            model.setPackaging("jar");
            try (FileWriter fileWriter = new FileWriter(pomFile)) {
               final MavenXpp3Writer writer = new MavenXpp3Writer();
               writer.write(fileWriter, model);
            }
         }
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   public static void extendDependencies(final Model model, final boolean junit3, final boolean excludeLog4j) {
      updateJUnit(model);

      final List<Dependency> dependencies = model.getDependencies();

      for (RequiredDependency dependency : RequiredDependency.getAll(junit3)) {
         if (dependency.getMavenDependency().getArtifactId().contains("slf4j-impl")) {
            addLoggingImplementationDependency(dependencies, dependency);
         } else if (dependency.getMavenDependency().getArtifactId().contains("kopeme")) {
            dependencies.add(0, dependency.getMavenDependency());
            if (excludeLog4j) {
               Exclusion exclusion = new Exclusion();
               exclusion.setArtifactId(LOG4J_SLF4J_IMPL_ARTIFACTID);
               exclusion.setGroupId(LOG4J_GROUPID);
               dependencies.get(0).addExclusion(exclusion);
            }
         } else {
            dependencies.add(dependency.getMavenDependency());
         }

      }
   }

   private static void updateJUnit(final Model model) {
      for (final Dependency dependency : model.getDependencies()) {
         if (dependency.getArtifactId().equals("junit") && dependency.getGroupId().equals("junit")) {
            dependency.setVersion("4.13.2");
         }
         if (dependency.getArtifactId().equals("junit-jupiter") && dependency.getGroupId().equals("org.junit.jupiter")) {
            dependency.setVersion("5.8.2");
         }
      }
   }

   private static void addLoggingImplementationDependency(final List<Dependency> dependencies, final RequiredDependency dependency) {
      Dependency originalSlf4j = null;
      for (Dependency original : dependencies) {
         if (original.getArtifactId().contains("slf4j-impl")) {
            originalSlf4j = original;
         }
      }
      if (originalSlf4j != null) {
         originalSlf4j.setScope(null);
      } else {
         dependencies.add(dependency.getMavenDependency());
      }
   }

   public static boolean isMultiModuleProject(final File pom) throws FileNotFoundException, IOException, XmlPullParserException {
      try (FileInputStream inputStream = new FileInputStream(pom)) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         final Model model = reader.read(inputStream);
         return model.getModules() != null;
      }
   }

   /**
    * This gets a list of all dependent modules of one maven module, so only these can be included in measurement; since maven does not provide a way to easily determine the
    * project structure, we call the (currently effectless) pre-clean goal and parse the output (relying on constant output format)
    * 
    * @param projectFolder
    * @param pl
    * @return
    * @throws IOException
    */
   public static List<String> getDependentModules(final File projectFolder, final String pl) throws IOException {
      ProcessBuilder pb = new ProcessBuilder(EnvironmentVariables.fetchMavenCallGeneric(),
            "-B", "pre-clean", "-pl", pl, "-am");
      pb.directory(projectFolder);
      String output = StreamGobbler.getFullProcess(pb.start(), false);
      List<String> modules = new LinkedList<>();
      for (String line : output.split("\n")) {
         if (line.contains("---------------<")) {
            String[] parts = line.split(" ");
            String groupAnArtifactPart = parts[2];
            String artifact = groupAnArtifactPart.split(":")[1];
            modules.add(artifact);

         }
      }
      return modules;
   }

   public static ProjectModules getModules(final File pom, final ExecutionConfig config) {
      try {
         ProjectModules modules = new ModuleReader().readModuleFiles(pom);
         if (config.getPl() != null && !"".equals(config.getPl())) {
            List<String> includedModuleNames = getIncludedModuleNames(pom, config);

            for (Iterator<File> moduleIterator = modules.getModules().iterator(); moduleIterator.hasNext();) {
               File listFile = moduleIterator.next();
               String fileModuleName = listFile.getName();
               System.out.println("Name: " + fileModuleName + " " + includedModuleNames);
               String fileArtifactId = modules.getArtifactIds().get(listFile);
               System.out.println("Artifactid: " + fileArtifactId);
               if (!includedModuleNames.contains(fileModuleName) && !includedModuleNames.contains(fileArtifactId)) {
                  moduleIterator.remove();
               }
            }
         }

         return modules;
      } catch (IOException | XmlPullParserException e) {
         throw new RuntimeException(e);
      }
   }

   private static List<String> getIncludedModuleNames(final File pom, final ExecutionConfig config) throws IOException {
      List<String> includedModuleNames = new LinkedList<>();
      ProcessBuilder builder = new ProcessBuilder(EnvironmentVariables.fetchMavenCallGeneric(),
            "--batch-mode", "pre-clean",
            "-pl", config.getPl(), "-am");
      builder.directory(pom.getParentFile());
      Process process = builder.start();
      String output = StreamGobbler.getFullProcess(process, false);
      for (String line : output.split("\n")) {
         int startIndex = line.indexOf("-<");
         int endIndex = line.indexOf(">-");
         System.out.println(line + " " + startIndex);
         if (line.startsWith("[ERROR]")) {
            throw new RuntimeException("Unexpected line when reading modules: " + line);
         }
         if (startIndex != -1) {
            int expectedModuleNameStart = startIndex + 3;
            int expectedModuleNameEnd = endIndex - 1;
            if (expectedModuleNameEnd < expectedModuleNameStart || line.startsWith("[ERROR]")) {
               throw new RuntimeException("Unexpected line when reading modules: " + line);
            }
            String fullModuleName = line.substring(expectedModuleNameStart, expectedModuleNameEnd);
            String moduleName = fullModuleName.split(":")[1];
            includedModuleNames.add(moduleName);
         }
      }
      return includedModuleNames;
   }

   public static Charset getEncoding(final Model model) {
      Charset value = StandardCharsets.UTF_8;
      final Properties properties = model.getProperties();
      if (properties != null) {
         final String encoding = (String) properties.get("project.build.sourceEncoding");
         if (encoding != null) {
            if (encoding.equals("ISO-8859-1")) {
               value = StandardCharsets.ISO_8859_1;
            }
         }
      }
      return value;
   }

   public static Plugin findPlugin(final Model model, final String artifactId, final String groupId) {
      Plugin surefire = null;
      if (model.getBuild() == null) {
         model.setBuild(new Build());
      }
      if (model.getBuild().getPlugins() == null) {
         model.getBuild().setPlugins(new LinkedList<Plugin>());
      }
      for (final Plugin plugin : model.getBuild().getPlugins()) {
         if (plugin.getArtifactId().equals(artifactId) && plugin.getGroupId().equals(groupId)) {
            surefire = plugin;
            break;
         }
      }
      if (surefire == null) {
         surefire = new Plugin();
         surefire.setArtifactId(artifactId);
         surefire.setGroupId(groupId);
         model.getBuild().getPlugins().add(surefire);
      }
      return surefire;
   }

   public static void extendSurefire(final String additionalArgLine, final Model model, final boolean updateVersion) {
      final Plugin plugin = MavenPomUtil.findPlugin(model, SUREFIRE_ARTIFACTID, ORG_APACHE_MAVEN_PLUGINS);
      if (plugin.getConfiguration() == null) {
         plugin.setConfiguration(new Xpp3Dom("configuration"));
      }

      if (updateVersion) {
         LOG.trace("Surefire {} {}", plugin.getClass(), plugin.getConfiguration().getClass());
         plugin.setVersion(MavenTestExecutor.SUREFIRE_VERSION);
      }

      final Xpp3Dom conf = (Xpp3Dom) plugin.getConfiguration();
      // MavenPomUtil.addNode(conf, "forkmode", "pertest");
      MavenPomUtil.setConfNode(conf, "forkCount", "1");
      MavenPomUtil.setConfNode(conf, "reuseForks", "false");
      MavenPomUtil.setConfNode(conf, "runOrder", "alphabetical");
      // if (timeout != -1) {
      // MavenPomUtil.setConfNode(conf, "forkedProcessTimeoutInSeconds", "" + timeout);
      // }

      Xpp3Dom argLine = conf.getChild("argLine");
      if (argLine != null) {
         String changedArgLine = argLine.getValue().contains("-Xmx") ? argLine.getValue().replaceAll("-Xmx[0-9]{0,3}[mM]", "-Xmx1g") : argLine.getValue();
         changedArgLine = changedArgLine.replaceAll("$\\{argLine\\}", "");
         argLine.setValue(changedArgLine + " " + additionalArgLine);
      } else {
         argLine = new Xpp3Dom("argLine");
         argLine.setValue(additionalArgLine);
         conf.addChild(argLine);
      }
   }

   public static void extendCompiler(final Plugin plugin, final String boot_class_path) {
      if (boot_class_path == null || !new File(boot_class_path).exists()) {
         throw new RuntimeException("Boot-Classpath " + boot_class_path + " is not defined.");
      }
      if (plugin.getConfiguration() == null) {
         plugin.setConfiguration(new Xpp3Dom("configuration"));
      }
      LOG.debug("Compiler" + plugin.getClass() + " " + plugin.getConfiguration().getClass());
      plugin.setVersion(COMPILER_PLUGIN_VERSION);

      LOG.info("BOOT_LIBS: {}", boot_class_path);

      final Xpp3Dom conf = (Xpp3Dom) plugin.getConfiguration();

      final Xpp3Dom compilerArguments = findChild(conf, "compilerArguments");
      final Xpp3Dom bootclasspath = findChild(compilerArguments, "bootclasspath");
      bootclasspath.setValue(boot_class_path + "/resources.jar${path.separator}" + boot_class_path
            + "/rt.jar${path.separator}" + boot_class_path + "/sunrsasign.jar:" + boot_class_path
            + "/jsse.jar${path.separator}" + boot_class_path + "/jce.jar${path.separator}" + boot_class_path
            + "/charsets.jar${path.separator}" + boot_class_path + "/jfr.jar");

   }

   public static void setIncrementalBuild(final Plugin plugin, final boolean build) {
      if (plugin.getConfiguration() == null) {
         plugin.setConfiguration(new Xpp3Dom("configuration"));
      }
      LOG.debug("Compiler" + plugin.getClass() + " " + plugin.getConfiguration().getClass());
      plugin.setVersion(COMPILER_PLUGIN_VERSION);

      final Xpp3Dom conf = (Xpp3Dom) plugin.getConfiguration();

      final Xpp3Dom compilerArguments = findChild(conf, "useIncrementalCompilation");
      compilerArguments.setValue("" + build);

   }

   private static Xpp3Dom findChild(final Xpp3Dom conf, final String name) {
      Xpp3Dom compilerArguments = conf.getChild(name);
      if (compilerArguments == null) {
         compilerArguments = new Xpp3Dom(name);
         conf.addChild(compilerArguments);
      }
      return compilerArguments;
   }

   public static Xpp3Dom setConfNode(final Xpp3Dom conf, final String nodeName, final String value) {
      Xpp3Dom confProperty = conf.getChild(nodeName);
      if (confProperty != null) {
         confProperty.setValue(value);
      } else if (confProperty == null) {
         confProperty = new Xpp3Dom(nodeName);
         confProperty.setValue(value);
         conf.addChild(confProperty);
      }
      return confProperty;
   }
}
