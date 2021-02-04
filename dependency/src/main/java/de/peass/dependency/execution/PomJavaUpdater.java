package de.peass.dependency.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomJavaUpdater {

   private static final Logger LOG = LogManager.getLogger(PomJavaUpdater.class);
   
   private static final int CURRENT_MAVEN_DEFAULT = 5;

   public static void fixCompilerVersion(final File pom) throws FileNotFoundException, IOException, XmlPullParserException {
      int version = getCompilerVersion(pom);
      if (version < 8) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         final Model model = reader.read(new FileInputStream(pom));
         setCompiler(model, MavenTestExecutor.DEFAULT_JAVA_VERSION);

         final MavenXpp3Writer writer = new MavenXpp3Writer();
         writer.write(new FileWriter(pom), model);
      }
   }

   private static int getCompilerVersion(final File pom) throws FileNotFoundException, IOException, XmlPullParserException {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      final Model model = reader.read(new FileInputStream(pom));
      final Plugin compilerPlugin = MavenPomUtil.findPlugin(model, MavenPomUtil.COMPILER_ARTIFACTID, MavenPomUtil.ORG_APACHE_MAVEN_PLUGINS);

      if (compilerPlugin != null) {
         final Xpp3Dom conf = (Xpp3Dom) compilerPlugin.getConfiguration();
         if (conf != null) {
            Xpp3Dom confProperty = conf.getChild("source");
            if (confProperty != null) {
               String value = confProperty.getValue();
               if (value.contains(".")) {
                  return Integer.parseInt(value.substring(value.indexOf(".")));
               } else {
                  return Integer.parseInt(value);
               }
            } else {
               return CURRENT_MAVEN_DEFAULT;
            }
         } else {
            return CURRENT_MAVEN_DEFAULT;
         }

      } else {
         return CURRENT_MAVEN_DEFAULT;
      }
   }

   private static void setCompiler(final Model model, final String version) {
      final Plugin compilerPlugin = MavenPomUtil.findPlugin(model, MavenPomUtil.COMPILER_ARTIFACTID, MavenPomUtil.ORG_APACHE_MAVEN_PLUGINS);
      if (compilerPlugin.getConfiguration() == null) {
         compilerPlugin.setConfiguration(new Xpp3Dom("configuration"));
      }
      LOG.debug("Compiler" + model.getClass() + " " + compilerPlugin.getConfiguration().getClass());
      model.setVersion("3.6.1");

      // Only set java version to 8 if not java 11 or above is specified
      final Xpp3Dom conf = (Xpp3Dom) compilerPlugin.getConfiguration();
      MavenPomUtil.setConfNode(conf, "source", version);
      MavenPomUtil.setConfNode(conf, "target", version);
   }
}
