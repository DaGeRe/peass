package de.peass.ci;

import java.io.File;
import java.util.LinkedList;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import de.peass.analysis.changes.ProjectChanges;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.execution.ProjectModules;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.measurement.analysis.AnalyseFullData;
import de.peass.measurement.analysis.ProjectStatistics;
import de.peass.vcs.GitCommit;

public class TestChangeReading {
   @Test
   public void testReading() throws InterruptedException {
      
      LinkedList<GitCommit> versions = new LinkedList<GitCommit>();
      versions.add(new GitCommit("946e4318267b56838aa35da0a2a4e5c0528bfe04", "", "", ""));
      versions.add(new GitCommit("fd79b2039667c09167c721b2823425629fad6d11", "", "", ""));
      VersionComparator.setVersions(versions);
      
      File baseFolder = new File("src/test/resources/dataReading/android-example-correct");
      ModuleClassMapping mapping = new ModuleClassMapping(baseFolder, new ProjectModules(new File(baseFolder, "app")));
      AnalyseFullData afd = new AnalyseFullData(new File("target/test.json"), new ProjectStatistics(), mapping);
      
      afd.analyseFolder(new File("src/test/resources/dataReading/measurementsFull/measurements/"));
      
      ProjectChanges changes = afd.getProjectChanges();
      Set<String> changedClazzes = changes.getVersion("946e4318267b56838aa35da0a2a4e5c0528bfe04").getTestcaseChanges().keySet();
     
      MatcherAssert.assertThat(changedClazzes, Matchers.contains("app§com.example.android_example.ExampleUnitTest"));
   }
}
