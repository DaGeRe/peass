package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.maven.OnePomPreparer;
import de.dagere.peass.testtransformation.JUnitVersions;
import de.dagere.peass.testtransformation.TestTransformer;

public class TestOnePomPreparer {
   
   private static final File EXAMINED_POM = new File(TestConstants.CURRENT_FOLDER, "pom.xml");
   
   @Test
   public void testSimpleLog4jUpdate() throws IOException, XmlPullParserException {
      File pom = new File(TestConstants.TEST_RESOURCES + "/mavenPomUtil/pom-simple-log4j.xml");
      FileUtils.copyFile(pom, EXAMINED_POM);
      
      executePomPreparing();
      
      final Model model = getModel();
      
      Assert.assertEquals("4.13.2", model.getDependencies().get(1).getVersion());
      Assert.assertEquals("2.18.0", model.getDependencies().get(2).getVersion());
   }
   
   @Test
   public void testJUnitUpdate() throws IOException, XmlPullParserException {
      File pom = new File(TestConstants.TEST_RESOURCES + "/mavenPomUtil/pom-junit.xml");
      FileUtils.copyFile(pom, EXAMINED_POM);
      
      executePomPreparing();
      
      final Model model = getModel();
      
      Assert.assertEquals("4.13.2", model.getDependencies().get(1).getVersion());
   }
   
   @Test
   public void testDependencyManagementLog4jUpdate() throws IOException, XmlPullParserException {
      File pom = new File(TestConstants.TEST_RESOURCES + "/mavenPomUtil/pom-dependencyManagement-log4j.xml");
      FileUtils.copyFile(pom, EXAMINED_POM);
      
      executePomPreparing();
      
      final Model model = getModel();
      
      Assert.assertEquals("2.18.0", model.getDependencyManagement().getDependencies().get(0).getVersion());
   }

   private Model getModel() throws IOException, XmlPullParserException, FileNotFoundException {
      final Model model;
      try (FileInputStream fileInputStream = new FileInputStream(EXAMINED_POM)) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         model = reader.read(fileInputStream);
      }
      return model;
   }
   
   private void executePomPreparing() throws FileNotFoundException, IOException, XmlPullParserException {
      TestTransformer mock = Mockito.mock(TestTransformer.class);
      Mockito.when(mock.getConfig()).thenReturn(new MeasurementConfig(2));
      JUnitVersions junitVersions = new JUnitVersions();
      junitVersions.setJunit4(true);
      Mockito.when(mock.getJUnitVersions()).thenReturn(junitVersions);
      
      OnePomPreparer preparer = new OnePomPreparer(mock);
      preparer.editOneBuildfile(true, EXAMINED_POM, new File("/dev/null"));
   }
   
}
