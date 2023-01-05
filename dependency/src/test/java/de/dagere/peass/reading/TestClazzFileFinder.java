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
      
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.dagere.TestMe1"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.dagere.TestMe2"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.dagere.Second"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.dagere.TestMe2$Inner"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.dagere.TestMe2$Inner$InnerInner"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.dagere.LocalClass"));
      
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.dagere.TestMe2$InnerEnum"));
   }
   
   @Test
   public void testGetSourceFile() throws FileNotFoundException {
      
      ExecutionConfig config = new ExecutionConfig();
      config.getClazzFolders().add("src/main/java");
      config.getClazzFolders().add("src/java");
      
      File sourceFileClass = new ClazzFileFinder(config).getSourceFile(SOURCE, new ChangedEntity("de.dagere.LocalClass"));
      Assert.assertNotNull(sourceFileClass);
      
      File sourceFileEnum = new ClazzFileFinder(config).getSourceFile(SOURCE, new ChangedEntity("de.dagere.LocalEnum"));
      Assert.assertNotNull(sourceFileEnum);
      
      File sourceFileInterface = new ClazzFileFinder(config).getSourceFile(SOURCE, new ChangedEntity("de.dagere.LocalInterface"));
      Assert.assertNotNull(sourceFileInterface);
      
      ChangedEntity exampleEntity = new ChangedEntity("de.dagere.LocalClass#myMethod(int)");
      String text = FileComparisonUtil.getMethodSource(SOURCE, exampleEntity, exampleEntity.getMethod(), config);
      MatcherAssert.assertThat(text, Matchers.containsString("this.i = i;"));
   }
}
