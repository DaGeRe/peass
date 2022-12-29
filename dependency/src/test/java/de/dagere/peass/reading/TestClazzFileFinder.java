package de.dagere.peass.reading;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.changesreading.FileComparisonUtil;

public class TestClazzFileFinder {
   
   private static final File SOURCE = new File("src/test/resources/clazzFinderExample/");
   
   @Test
   public void testClasses() {
      ExecutionConfig config = new ExecutionConfig();
      config.getClazzFolders().add("src/main/java");
      config.getClazzFolders().add("src/java");
      
      List<String> clazzes = new ClazzFileFinder(config).getClasses(SOURCE);
      
      System.out.println(clazzes);
      
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.TestMe1"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.TestMe2"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.Second"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.TestMe2$Inner"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.TestMe2$Inner$InnerInner"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.LocalClass"));
      
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.TestMe2$InnerEnum"));
   }
   
   @Test
   public void testGetSourceFile() throws FileNotFoundException {
      
      ExecutionConfig config = new ExecutionConfig();
      config.getClazzFolders().add("src/main/java");
      config.getClazzFolders().add("src/java");
      
      File sourceFileClass = new ClazzFileFinder(config).getSourceFile(SOURCE, new ChangedEntity("de.LocalClass"));
      Assert.assertNotNull(sourceFileClass);
      
      File sourceFileEnum = new ClazzFileFinder(config).getSourceFile(SOURCE, new ChangedEntity("de.LocalEnum"));
      Assert.assertNotNull(sourceFileEnum);
      
      File sourceFileInterface = new ClazzFileFinder(config).getSourceFile(SOURCE, new ChangedEntity("de.LocalInterface"));
      Assert.assertNotNull(sourceFileInterface);
      
      ChangedEntity exampleEntity = new ChangedEntity("de.LocalClass#myMethod(int)");
      String text = FileComparisonUtil.getMethodSource(SOURCE, exampleEntity, exampleEntity.getMethod(), config);
      MatcherAssert.assertThat(text, Matchers.containsString("this.i = i;"));
   }
}
