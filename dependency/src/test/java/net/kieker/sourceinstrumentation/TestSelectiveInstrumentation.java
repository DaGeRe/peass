package net.kieker.sourceinstrumentation;


import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import de.peass.TestConstants;
import de.peass.dependency.execution.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public class TestSelectiveInstrumentation {
   @Test
   public void testSingleSelectiveInstrumentation() throws Exception {
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2/");

      Set<String> includedPatterns = new HashSet<>();
      includedPatterns.add("public void de.peass.MainTest.testMe()");
      
      InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, includedPatterns, false, true);
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(kiekerConfiguration);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      TestSourceInstrumentation.testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/test/java/de/peass/MainTest.java"), 
            "public void de.peass.MainTest.testMe()", "OperationExecutionRecord");
      
      SourceInstrumentationTestUtil.testFileIsNotInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C0_0.java"), 
            "public void de.peass.C0_0.method0()");
      SourceInstrumentationTestUtil.testFileIsNotInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C1_0.java"), 
            "public void de.peass.C1_0.method0()");
   }
   
   
}
