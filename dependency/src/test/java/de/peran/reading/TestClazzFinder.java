package de.peran.reading;

import java.io.File;
import java.util.List;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Test;

import de.peass.dependency.ClazzFinder;

public class TestClazzFinder {
   
   private static final File SOURCE = new File("src/test/resources/clazzFinderExample/");
   
   @Test
   public void testClasses() {
      List<String> clazzes = ClazzFinder.getClasses(SOURCE);
      
      System.out.println(clazzes);
      
      Assert.assertThat(clazzes, IsCollectionContaining.hasItem("de.TestMe1"));
      Assert.assertThat(clazzes, IsCollectionContaining.hasItem("de.TestMe2"));
      Assert.assertThat(clazzes, IsCollectionContaining.hasItem("de.Second"));
      Assert.assertThat(clazzes, IsCollectionContaining.hasItem("de.TestMe2.Inner"));
   }
}
