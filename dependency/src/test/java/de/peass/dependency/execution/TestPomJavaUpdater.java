package de.peass.dependency.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestPomJavaUpdater {

   private final File experimentFile = new File("target" + File.separator + "pom.xml");
   private static final File dependencyITFolder = new File("src" + File.separator + "test" + File.separator + "resources" + File.separator + "dependencyIT");

   @Test
   public void testNoCompilerPluginPom() throws IOException, XmlPullParserException {
      File originalPom = new File(dependencyITFolder, "normal_change" + File.separator + "pom.xml");

      FileUtils.copyFile(originalPom, experimentFile);

      PomJavaUpdater.fixCompilerVersion(experimentFile);

      final Xpp3Dom conf = getConfig();
      Xpp3Dom confPropertySource = conf.getChild("source");
      Assert.assertEquals("1.8", confPropertySource.getValue());
      Xpp3Dom confPropertyTarget = conf.getChild("target");
      Assert.assertEquals("1.8", confPropertyTarget.getValue());
   }

   private Xpp3Dom getConfig() throws IOException, XmlPullParserException, FileNotFoundException {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      final Model model = reader.read(new FileInputStream(experimentFile));
      final Plugin compilerPlugin = MavenPomUtil.findPlugin(model, MavenPomUtil.COMPILER_ARTIFACTID, MavenPomUtil.ORG_APACHE_MAVEN_PLUGINS);
      final Xpp3Dom conf = (Xpp3Dom) compilerPlugin.getConfiguration();
      return conf;
   }

   @Test
   public void testJava11Pom() throws IOException, XmlPullParserException {
      File originalPom = new File(dependencyITFolder, "pom-11.xml");

      FileUtils.copyFile(originalPom, experimentFile);

      PomJavaUpdater.fixCompilerVersion(experimentFile);

      final Xpp3Dom conf = getConfig();
      Xpp3Dom confPropertySource = conf.getChild("source");
      Assert.assertEquals("11", confPropertySource.getValue());
      Xpp3Dom confPropertyTarget = conf.getChild("target");
      Assert.assertEquals("11", confPropertyTarget.getValue());
   }
}
