package de.dagere.peass.dependency.execution.pom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;


public class TestExtendDependencies {
   @Test
   public void testRegularRemoval() throws FileNotFoundException, IOException, XmlPullParserException {
      File pomFile = new File("src/test/resources/example-snapshot-removal-pom/pom.xml");

      File testPom = new File("target/pom.xml");
      FileUtils.copyFile(pomFile, testPom);

      try (FileInputStream inputStream = new FileInputStream(testPom)) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         final Model model = reader.read(inputStream);

         MavenPomUtil.extendDependencies(model, false);

         Dependency kopemeDependency = model.getDependencies().get(0);

         Assert.assertEquals("kopeme-junit", kopemeDependency.getArtifactId());
      }

   }
}
