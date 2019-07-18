package de.peass.measurement.searchcause;

import java.io.File;

import de.peass.dependency.analysis.data.TestCase;

public class CauseSearcherConfig {
   public File projectFolder;
   public String version;
   public String predecessor;
   public TestCase testCase;

   public CauseSearcherConfig(File projectFolder, String version, String predecessor, TestCase testCase) {
      this.projectFolder = projectFolder;
      this.version = version;
      this.predecessor = predecessor;
      this.testCase = testCase;
   }
}