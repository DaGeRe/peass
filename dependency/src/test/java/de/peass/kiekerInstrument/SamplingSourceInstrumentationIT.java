package de.peass.kiekerInstrument;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.peass.TestConstants;
import de.peass.dependency.execution.AllowedKiekerRecord;
import de.peass.dependency.execution.MavenPomUtil;
import de.peass.utils.StreamGobbler;

public class SamplingSourceInstrumentationIT {

   File tempFolder;
   
   @BeforeEach
   public void before() throws IOException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2/");

      tempFolder = new File(TestConstants.CURRENT_FOLDER, "results");
      tempFolder.mkdir();
   }

   @Test
   public void testExecution() throws IOException, XmlPullParserException {
      Set<String> shouldInstrument = new HashSet<>();
      shouldInstrument.add("public void de.peass.MainTest.testMe()");
      shouldInstrument.add("public void de.peass.C0_0.method0()");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION, shouldInstrument, true);

      extendMaven();

      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      final ProcessBuilder pb = new ProcessBuilder("mvn", "test",
            "-Djava.io.tmpdir=" + tempFolder.getAbsolutePath(),
            "-Dkieker.monitoring.adaptiveMonitoring.enabled=false");
      pb.directory(TestConstants.CURRENT_FOLDER);

      Process process = pb.start();
      StreamGobbler.showFullProcess(process);

      File resultFolder = tempFolder.listFiles()[0];
      File resultFile = resultFolder.listFiles((FileFilter) new WildcardFileFilter("*.dat"))[0];

      String monitorLogs = FileUtils.readFileToString(resultFile, StandardCharsets.UTF_8);
      Assert.assertThat(monitorLogs, Matchers.containsString("public void de.peass.MainTest.testMe()"));
      Assert.assertThat(monitorLogs, Matchers.containsString("public void de.peass.C0_0.method0()"));
      Assert.assertThat(monitorLogs, Matchers.not(Matchers.containsString("public void de.peass.C1_0.method0()")));
      Assert.assertThat(monitorLogs, Matchers.not(Matchers.containsString("public void de.peass.AddRandomNumbers.addSomething()")));
   }

   private void extendMaven() throws IOException, XmlPullParserException, FileNotFoundException {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      final File pomFile = new File(TestConstants.CURRENT_FOLDER, "pom.xml");
      final Model model = reader.read(new FileInputStream(pomFile));
      MavenPomUtil.extendDependencies(model, false);
      final MavenXpp3Writer writer = new MavenXpp3Writer();
      writer.write(new FileWriter(pomFile), model);
   }
}
