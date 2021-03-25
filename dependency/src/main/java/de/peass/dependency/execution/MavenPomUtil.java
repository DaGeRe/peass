package de.peass.dependency.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenPomUtil {

   public static final String KOPEME_VERSION = "0.14-SNAPSHOT";
   public static final String KIEKER_VERSION = "1.15-SNAPSHOT";
   public static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
   public static final String SUREFIRE_ARTIFACTID = "maven-surefire-plugin";
   public static final String COMPILER_ARTIFACTID = "maven-compiler-plugin";

   public static final String COMPILER_PLUGIN_VERSION = "3.8.1";

   private static final Logger LOG = LogManager.getLogger(MavenPomUtil.class);

   /**
    * Apache Commons projects which depend on each other sometimes use SNAPSHOT-dependencies to other projects; since they are not in maven central, this leads to fails. In order
    * to avoid this issue, the -SNAPSHOT is cleared in the beginning. For old version, the release should have happened, therefore this works for most of the releases.
    */
   public static void cleanSnapshotDependencies(final File pomFile) {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      try (FileInputStream inputStream = new FileInputStream(pomFile)) {
         final Model model = reader.read(inputStream);
         Build build = model.getBuild();
         if (build == null) {
            build = new Build();
            model.setBuild(build);
         }

         removeSnapshots(model);
         final List<Plugin> plugins = build.getPlugins();
         if (plugins != null) {
            for (final Plugin plugin : plugins) {
               handlePlugin(plugin);
            }
         }
         if (build.getPluginManagement() != null) {
            if (build.getPluginManagement().getPlugins() != null) {
               for (final Plugin plugin : build.getPluginManagement().getPlugins()) {
                  handlePlugin(plugin);
               }
            }
         }

         try (FileWriter fileWriter = new FileWriter(pomFile)) {
            final MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(fileWriter, model);
         }

      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private static void removeSnapshots(final Model model) {
      final String selfGroupId = model.getGroupId();
      final List<Dependency> dependencies = model.getDependencies();
      if (dependencies != null) {
         for (final Dependency dep : dependencies) {
            if (dep.getVersion() != null) {
               if (!dep.getArtifactId().equals("kopeme-junit") &&
                     !dep.getArtifactId().equals("kopeme-junit3") &&
                     !dep.getArtifactId().equals("kieker-monitoring") &&
                     !dep.getGroupId().startsWith(selfGroupId) &&
                     !selfGroupId.startsWith(dep.getGroupId())) {

                  if (dep.getVersion().endsWith("-SNAPSHOT")) {
                     dep.setVersion(dep.getVersion().replaceAll("-SNAPSHOT", ""));
                  }
               }
            }
         }
      }
   }

   public static void cleanType(final File pomFile) {
      try (FileInputStream inputStream = new FileInputStream(pomFile)) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         final Model model = reader.read(inputStream);
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

   private static void handlePlugin(final Plugin plugin) {
      if (plugin.getVersion() != null) {
         if (plugin.getVersion().endsWith("-SNAPSHOT")) {
            plugin.setVersion(plugin.getVersion().replaceAll("-SNAPSHOT", ""));
         }
      }
      if (plugin.getArtifactId().equals("buildnumber-maven-plugin")) {
         if (plugin.getConfiguration() != null) {
            final Xpp3Dom conf = (Xpp3Dom) plugin.getConfiguration();
            setConfNode(conf, "doUpdate", "false");
         }
      }
   }

   public static void extendDependencies(final Model model, final boolean junit3) {
      for (final Dependency dependency : model.getDependencies()) {
         if (dependency.getArtifactId().equals("junit") && dependency.getGroupId().equals("junit")) {
            dependency.setVersion("4.13");
         }
         if (dependency.getArtifactId().equals("junit-jupiter") && dependency.getGroupId().equals("org.junit.jupiter")) {
            dependency.setVersion("5.7.0");
         }
      }

      final List<Dependency> dependencies = model.getDependencies();

      for (RequiredDependency dependency : RequiredDependency.getAll(junit3)) {
         dependencies.add(dependency.getMavenDependency());
      }
   }

   public static boolean isMultiModuleProject(final File pom) throws FileNotFoundException, IOException, XmlPullParserException {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      try (FileInputStream inputStream = new FileInputStream(pom)) {
         final Model model = reader.read(inputStream);
         return model.getModules() != null;
      }

   }

   public static ProjectModules getModules(final File pom) throws FileNotFoundException, IOException, XmlPullParserException {
      try (FileInputStream inputStream = new FileInputStream(pom)) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         final Model model = reader.read(inputStream);
         final List<File> modules = new LinkedList<>();
         if (model.getModules() != null && model.getModules().size() > 0) {
            for (final String module : model.getModules()) {
               final File moduleFile = new File(pom.getParentFile(), module);
               modules.add(moduleFile);
            }
         } else {
            modules.add(pom.getParentFile());
         }
         return new ProjectModules(modules);
      }
   }

   public static List<String> getModuleNames(final File pom) throws FileNotFoundException, IOException, XmlPullParserException {
      try (FileInputStream inputStream = new FileInputStream(pom)) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         final Model model = reader.read(inputStream);
         final List<String> modules = new LinkedList<>();
         if (model.getModules() != null && model.getModules().size() > 0) {
            for (final String module : model.getModules()) {
               modules.add(module);
            }
         } else {
            modules.add("");
         }
         return modules;
      }
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

   public static void extendSurefire(final String additionalArgLine, final Model model, final boolean updateVersion, final long timeout) {
      final Plugin plugin = MavenPomUtil.findPlugin(model, SUREFIRE_ARTIFACTID, ORG_APACHE_MAVEN_PLUGINS);
      if (plugin.getConfiguration() == null) {
         plugin.setConfiguration(new Xpp3Dom("configuration"));
      }

      if (updateVersion) {
         LOG.debug("Surefire" + plugin.getClass() + " " + plugin.getConfiguration().getClass());
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

   protected static Xpp3Dom setConfNode(final Xpp3Dom conf, final String nodeName, final String value) {
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

   public static ProjectModules getGenericModules(final File projectFolder) throws FileNotFoundException, IOException, XmlPullParserException {
      final File pomXml = new File(projectFolder, "pom.xml");
      if (pomXml.exists()) {
         return MavenPomUtil.getModules(pomXml);
      } else if (new File(projectFolder, "build.gradle").exists()) {
         return GradleParseUtil.getModules(projectFolder);
      } else {
         return null;
      }
   }

}
