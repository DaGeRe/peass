package de.dagere.peass.dependency.reader;

import java.io.File;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.InitialDependency;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
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
      for (Entry<String, VersionStaticSelection> version : dependencyResult.getVersions().entrySet()) {
         TestSet tests = version.getValue().getTests();
         for (TestCase testcase : tests.getTests()) {
            addPotentialTracefile(testcase, version.getKey());
         }
      }
   }

   private void addInitialVersion() {
      for (Entry<TestCase, InitialDependency> classDependency : dependencyResult.getInitialversion().getInitialDependencies().entrySet()) {
         TestCase testcase = classDependency.getKey();
         String initialVersion = dependencyResult.getInitialversion().getVersion();
         addPotentialTracefile(testcase, initialVersion);
      }
   }
   
   private void addPotentialTracefile(final TestCase testcase, final String initialVersion) {
      String shortVersion = TraceWriter.getShortVersion(initialVersion);
      File potentialTraceFile = new File(resultsFolders.getViewMethodDir(initialVersion, testcase), shortVersion);
      LOG.debug("Potential trace file: " + potentialTraceFile.getAbsolutePath() + " " + potentialTraceFile.exists());
      if (potentialTraceFile.exists()) {
         traceFileMapping.addTraceFile(testcase, potentialTraceFile);
      }
   }

}
