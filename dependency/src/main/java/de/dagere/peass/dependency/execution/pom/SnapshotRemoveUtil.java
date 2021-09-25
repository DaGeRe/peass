package de.dagere.peass.dependency.execution.pom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Apache Commons projects which depend on each other sometimes use SNAPSHOT-dependencies to other projects; since they are not in maven central, this leads to fails. In order to
 * avoid this issue, the -SNAPSHOT is cleared in the beginning. For old version, the release should have happened, therefore this works for most of the releases.
 */
public class SnapshotRemoveUtil {

   public static void cleanSnapshotDependencies(final File pomFile) {
      try {
         final Model model;
         try (FileInputStream inputStream = new FileInputStream(pomFile)) {
            final MavenXpp3Reader reader = new MavenXpp3Reader();
            model = reader.read(inputStream);
         }
         Build build = model.getBuild();
         if (build == null) {
            build = new Build();
            model.setBuild(build);
         }

         removeDependencySnapshots(model);
         removePluginSnapshots(build);
         removePluginManagementSnapshots(build);
         try (FileWriter fileWriter = new FileWriter(pomFile)) {
            final MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(fileWriter, model);
         }
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private static void removePluginManagementSnapshots(final Build build) {
      if (build.getPluginManagement() != null) {
         if (build.getPluginManagement().getPlugins() != null) {
            for (final Plugin plugin : build.getPluginManagement().getPlugins()) {
               handlePlugin(plugin);
            }
         }
      }
   }

   private static void removePluginSnapshots(final Build build) {
      final List<Plugin> plugins = build.getPlugins();
      if (plugins != null) {
         for (final Plugin plugin : plugins) {
            handlePlugin(plugin);
         }
      }
   }

   private static void removeDependencySnapshots(final Model model) {
      final String selfGroupId = model.getGroupId();
      final List<Dependency> dependencies = model.getDependencies();
      if (dependencies != null) {
         for (final Dependency dep : dependencies) {
            if (dep.getVersion() != null) {
               String artifactId = dep.getArtifactId();
               if (!artifactId.equals("kopeme-junit") &&
                     !artifactId.equals("kopeme-junit3") &&
                     !artifactId.equals("kieker-monitoring")) {
                  String groupId = dep.getGroupId();
                  if (groupId != null &&
                        (!groupId.startsWith(selfGroupId) &&
                              !selfGroupId.startsWith(groupId))) {
                     if (dep.getVersion().endsWith("-SNAPSHOT")) {
                        dep.setVersion(dep.getVersion().replaceAll("-SNAPSHOT", ""));
                     }
                  }
               }
            }
         }
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
            MavenPomUtil.setConfNode(conf, "doUpdate", "false");
         }
      }
   }
}
