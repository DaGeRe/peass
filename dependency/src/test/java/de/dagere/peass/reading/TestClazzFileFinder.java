package de.dagere.peass.reading;

import java.io.File;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.ClazzFileFinder;

public class TestClazzFileFinder {
   
   private static final File SOURCE = new File("src/test/resources/clazzFinderExample/");
   
   @Test
   public void testClasses() {
      List<String> clazzes = ClazzFileFinder.getClasses(SOURCE);
      
      System.out.println(clazzes);
      
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.TestMe1"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.TestMe2"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.Second"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.TestMe2$Inner"));
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.TestMe2$Inner$InnerInner"));
      
      MatcherAssert.assertThat(clazzes, Matchers.hasItem("de.TestMe2$InnerEnum"));
   }
}
