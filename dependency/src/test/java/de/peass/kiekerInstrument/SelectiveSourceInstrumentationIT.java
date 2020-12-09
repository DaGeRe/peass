package de.peass.kiekerInstrument;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.peass.TestConstants;
import de.peass.dependency.execution.AllowedKiekerRecord;
import de.peass.dependency.execution.MavenTestExecutor;
import de.peass.utils.StreamGobbler;

public class SelectiveSourceInstrumentationIT {
   
   @BeforeEach
   public void before() throws IOException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
   }

   @Test
   public void testExecution() throws IOException {
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2/");

      File tempFolder = new File(TestConstants.CURRENT_FOLDER, "results");
      tempFolder.mkdir();

      
      Set<String> shouldInstrument = new HashSet<>();
      shouldInstrument.add("public void de.peass.MainTest.testMe()");
      shouldInstrument.add("public void de.peass.C0_0.method0()");
      
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION, shouldInstrument);
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
}
