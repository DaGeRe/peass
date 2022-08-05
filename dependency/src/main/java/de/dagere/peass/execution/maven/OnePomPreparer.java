package de.dagere.peass.execution.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.execution.kieker.ArgLineBuilder;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.testtransformation.TestTransformer;

public class OnePomPreparer {
   private final TestTransformer testTransformer;

   public OnePomPreparer(TestTransformer testTransformer) {
      this.testTransformer = testTransformer;
   }

   public Charset editOneBuildfile(final boolean update, final File pomFile, File lastTmpFile) throws FileNotFoundException, IOException, XmlPullParserException {
      final Model model;
      try (FileInputStream fileInputStream = new FileInputStream(pomFile)) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         model = reader.read(fileInputStream);
      }

      if (model.getBuild() == null) {
         model.setBuild(new Build());
      }
      final String argline = new ArgLineBuilder(testTransformer, pomFile.getParentFile()).buildArglineMaven(lastTmpFile);

      MavenPomUtil.extendSurefire(argline, model, update);

      // TODO Move back to extend dependencies, if stable Kieker version supports <init>
      if (model.getDependencies() == null) {
         model.setDependencies(new LinkedList<Dependency>());
      }
      MavenPomUtil.extendDependencies(model, testTransformer.getJUnitVersions(), testTransformer.getConfig().getExecutionConfig().isExcludeLog4jSlf4jImpl());

      try (FileWriter fileWriter = new FileWriter(pomFile)) {
         final MavenXpp3Writer writer = new MavenXpp3Writer();
         writer.write(fileWriter, model);
      }

      Charset lastEncoding = MavenPomUtil.getEncoding(model);
      return lastEncoding;
   }
}
