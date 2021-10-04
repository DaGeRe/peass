package net.kieker.sourceinstrumentation.it;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.utils.StreamGobbler;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.SourceInstrumentationTestUtil;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

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

      
      Set<String> includedPatterns = new HashSet<>();
      includedPatterns.add("public void de.peass.MainTest.testMe()");
      includedPatterns.add("public void de.peass.C0_0.method0()");
      includedPatterns.add("public void de.peass.AddRandomNumbers.*()");
      // Kieker pattern parser currently does not accept new, even if it the "return value" which is inside of the records signature
      includedPatterns.add("* de.peass.C0_0.<init>()");
      
      Set<String> excluded = new HashSet<>();
      excluded.add("public void de.peass.AddRandomNumbers.addSomething()");
      
      InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(AllowedKiekerRecord.DURATION, true, true, false, includedPatterns, excluded, true, 1000, false);
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(kiekerConfiguration);
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
      MatcherAssert.assertThat(monitorLogs, Matchers.containsString("public void de.peass.MainTest.testMe()"));
      MatcherAssert.assertThat(monitorLogs, Matchers.containsString("public void de.peass.C0_0.method0()"));
      MatcherAssert.assertThat(monitorLogs, Matchers.containsString("new de.peass.C0_0.<init>()"));
      MatcherAssert.assertThat(monitorLogs, Matchers.not(Matchers.containsString("public void de.peass.C1_0.method0()")));
      MatcherAssert.assertThat(monitorLogs, Matchers.not(Matchers.containsString("public void de.peass.AddRandomNumbers.addSomething()")));
   }
}
