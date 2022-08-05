package de.dagere.peass.analysis.data;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.io.Files;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.CommitDiff;

public class TestVersionDiff {
   
   private static final String MAVEN_PATH_FILE = "src/main/java/de/dagere/peass/Example.java";
   private static final String JAVA_PATH_FILE = "java/de/dagere/peass/Example.java";
   private static final String NO_SOURCE_FOLDER = "noSourceFolder/de/dagere/peass/Example2.java";
   private static final String TOMCAT_EXAMPLE = "test/de/dagere/peass/CoyoteAdapter.java";
   
   @TempDir
   private File tempDir;
   
   @Test
   public void testRegularMavenPath() throws IOException {
      createFile(MAVEN_PATH_FILE);
      createFile(NO_SOURCE_FOLDER);
      
      CommitDiff diff = createVersionDiff();
      
      ExecutionConfig config = new ExecutionConfig();
      diff.addChange(MAVEN_PATH_FILE, config);
      diff.addChange(NO_SOURCE_FOLDER, config);
      
      MatcherAssert.assertThat(diff.getChangedClasses(), IsIterableContaining.hasItem(new ChangedEntity("de.dagere.peass.Example")));
   }
   
   @Test
   public void testJavaContainigPath() throws IOException {
      createFile(JAVA_PATH_FILE);
      createFile(NO_SOURCE_FOLDER);
      
      CommitDiff diff = createVersionDiff();
      
      ExecutionConfig config = new ExecutionConfig();
      config.getClazzFolders().clear();
      config.getClazzFolders().add("java");
      diff.addChange(JAVA_PATH_FILE, config);
      diff.addChange(NO_SOURCE_FOLDER, config);
      
      MatcherAssert.assertThat(diff.getChangedClasses(), IsIterableContaining.hasItem(new ChangedEntity("de.dagere.peass.Example")));
      MatcherAssert.assertThat(diff.getChangedClasses(), Matchers.not(IsIterableContaining.hasItem(new ChangedEntity("de.dagere.peass.Example2"))));
   }
   
   @Test
   public void testTestContainigPath() throws IOException {
      createFile(TOMCAT_EXAMPLE);
      createFile(NO_SOURCE_FOLDER);
      
      CommitDiff diff = createVersionDiff();
      
      ExecutionConfig config = new ExecutionConfig();
      config.getClazzFolders().clear();
      config.getClazzFolders().add("java");
      config.getTestClazzFolders().clear();
      config.getTestClazzFolders().add("test");
      diff.addChange(TOMCAT_EXAMPLE, config);
      diff.addChange(NO_SOURCE_FOLDER, config);
      
      MatcherAssert.assertThat(diff.getChangedClasses(), IsIterableContaining.hasItem(new ChangedEntity("de.dagere.peass.CoyoteAdapter")));
      MatcherAssert.assertThat(diff.getChangedClasses(), Matchers.not(IsIterableContaining.hasItem(new ChangedEntity("de.dagere.peass.Example2"))));
   }

   private CommitDiff createVersionDiff() {
      List<File> modules = new LinkedList<>();
      modules.add(tempDir);
      CommitDiff diff = new CommitDiff(modules, tempDir);
      return diff;
   }

   private void createFile(final String mavenPathFile) throws IOException {
      File exampleClassFile = new File(tempDir, mavenPathFile);
      exampleClassFile.getParentFile().mkdirs();
      Files.touch(exampleClassFile);
   }
}
