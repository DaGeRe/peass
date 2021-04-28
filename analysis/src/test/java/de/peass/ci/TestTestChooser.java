package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.InitialDependency;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.utils.Constants;
import de.peass.ci.helper.GitProjectBuilder;

public class TestTestChooser {
   
   private String testVersion;
   
   private GitProjectBuilder builder;
   
   @BeforeEach
   public void cleanDependencies() throws IOException, InterruptedException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER.getParentFile());
      builder = new GitProjectBuilder(TestConstants.CURRENT_FOLDER, new File("../dependency/src/test/resources/dependencyIT/basic_state"));
   }
   
   @Test
   public void testBasicChoosing() throws Exception {
      Dependencies dependencies = createDependencies();
      
      TestChooser chooser = new TestChooser(false, new File("target/view_it"), new PeASSFolders(TestConstants.CURRENT_FOLDER), testVersion, new File("target/views"), new File("target/properties"), 1, new ExecutionConfig(1), new EnvironmentVariables());
      Set<TestCase> tests = chooser.getTestSet(dependencies);
      
      Assert.assertEquals(tests.iterator().next(), new TestCase("defaultpackage.TestMe#testMe"));
   }
   
   @Test
   public void testViewChoosing() throws Exception {
      Dependencies dependencies = createDependencies();
      
      TestChooser chooser = new TestChooser(true, new File("target/view_it"), new PeASSFolders(TestConstants.CURRENT_FOLDER), testVersion, new File("target/views"), new File("target/properties"), 1, new ExecutionConfig(1), new EnvironmentVariables());
      Set<TestCase> tests = chooser.getTestSet(dependencies);
      
      Assert.assertEquals(tests.iterator().next(), new TestCase("defaultpackage.TestMe#testMe"));
   }

   private Dependencies createDependencies() throws InterruptedException, IOException {
      builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/normal_change"), "Version 1");
      
      Dependencies dependencies = new Dependencies();
      final InitialDependency initial = new InitialDependency();
      initial.getEntities().add(new ChangedEntity("defaultpackage.NormalDependency#executeThing", ""));
      dependencies.getInitialversion().setVersion(builder.getTags().get(0));
      dependencies.getInitialversion().getInitialDependencies().put(new ChangedEntity("defaultpackage.TestMe#testMe", ""), initial);
      
      final Version version = new Version();
      version.getChangedClazzes().put(new ChangedEntity("defaultpackage.NormalDependency#executeThing", ""), new TestSet("defaultpackage.TestMe#testMe"));
      testVersion = builder.getTags().get(1);
      dependencies.getVersions().put(testVersion, version);
      
      Constants.OBJECTMAPPER.writeValue(TestContinuousDependencyReader.dependencyFile, dependencies);
      
      VersionComparator.setDependencies(dependencies);
      return dependencies;
   }
}
