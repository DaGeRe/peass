package de.dagere.peass.dependency.reader;

import java.io.File;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.InitialCallList;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.traces.TraceFileManager;
import de.dagere.peass.dependency.traces.TraceFileMapping;
import de.dagere.peass.dependency.traces.TraceWriter;
import de.dagere.peass.folders.ResultsFolders;

public class OldTraceReader {
   
   private static final Logger LOG = LogManager.getLogger(OldTraceReader.class);
   
   private final TraceFileMapping traceFileMapping;
   private final StaticTestSelection dependencyResult;
   private final ResultsFolders resultsFolders;
   
   public OldTraceReader(final TraceFileMapping traceFileMapping, final StaticTestSelection dependencyResult, final ResultsFolders resultsFolders) {
      this.traceFileMapping = traceFileMapping;
      this.dependencyResult = dependencyResult;
      this.resultsFolders = resultsFolders;
   }

   public void addTraces() {
      addInitialVersion();
      
      addRegularVersions();
      LOG.debug("Trace file finished reading");
   }

   private void addRegularVersions() {
      for (Entry<String, CommitStaticSelection> commit : dependencyResult.getCommits().entrySet()) {
         TestSet tests = commit.getValue().getTests();
         for (TestMethodCall testcase : tests.getTestMethods()) {
            addPotentialTracefile(testcase, commit.getKey());
         }
      }
   }

   private void addInitialVersion() {
      for (Entry<TestMethodCall, InitialCallList> classDependency : dependencyResult.getInitialcommit().getInitialDependencies().entrySet()) {
         TestMethodCall testcase = classDependency.getKey();
         String initialVersion = dependencyResult.getInitialcommit().getCommit();
         addPotentialTracefile(testcase, initialVersion);
      }
   }
   
   private void addPotentialTracefile(final TestMethodCall testcase, final String initialVersion) {
      String shortVersion = TraceWriter.getShortCommit(initialVersion);
      
      for (String ending : new String[] {"", TraceFileManager.TXT_ENDING, TraceFileManager.ZIP_ENDING}) {
         File potentialTraceFile = new File(resultsFolders.getViewMethodDir(initialVersion, testcase), shortVersion + ending);
         LOG.debug("Potential trace file: " + potentialTraceFile.getAbsolutePath() + " " + potentialTraceFile.exists());
         if (potentialTraceFile.exists()) {
            traceFileMapping.addTraceFile(testcase, potentialTraceFile);
         }
      }
   }

}
