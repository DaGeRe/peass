package de.dagere.peass.dependency.traces;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dagere.peass.dependency.analysis.data.TestCase;

public class TraceFileMapping {
   private final Map<String, List<File>> mapping = new HashMap<>();

   public void addTraceFile(final TestCase test, final File traceFile) {
      List<File> testTraceFiles = mapping.get(test.toString());
      if (testTraceFiles == null) {
         testTraceFiles = new LinkedList<>();
         mapping.put(test.toString(), testTraceFiles);
      }
      if (testTraceFiles.size() > 1) {
         testTraceFiles.remove(0);
      }
      testTraceFiles.add(traceFile);
   }
   
   public int size() {
      return mapping.size();
   }
   
   public List<File> getTestcaseMap(final TestCase test){
      return mapping.get(test.toString());
   }
}