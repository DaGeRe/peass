package de.peass.reading;

import java.io.File;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.dependency.ClazzFileFinder;

public class TestClazzFileFinder {
   
   private static final File SOURCE = new File("src/test/resources/clazzFinderExample/");
   
   @Test
   public void testClasses() {
      List<String> clazzes = ClazzFileFinder.getClasses(SOURCE);
      
      System.out.println(clazzes);
      
      Assert.assertThat(clazzes, Matchers.hasItem("de.TestMe1"));
      Assert.assertThat(clazzes, Matchers.hasItem("de.TestMe2"));
      Assert.assertThat(clazzes, Matchers.hasItem("de.Second"));
      Assert.assertThat(clazzes, Matchers.hasItem("de.TestMe2.Inner"));
   }
}
