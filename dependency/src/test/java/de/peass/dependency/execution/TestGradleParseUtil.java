package de.peass.dependency.execution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class TestGradleParseUtil {
   
   @Test
   public void testModuleGetting() throws FileNotFoundException, IOException {
      List<File> modules = GradleParseUtil.getModules(new File("src/test/resources/gradle-multimodule-example"));
      
      Assert.assertThat(modules.size(), Matchers.is(2));
   }
   
   @Test
   public void testModuleGettingSpaces() throws FileNotFoundException, IOException {
      List<File> modules = GradleParseUtil.getModules(new File("src/test/resources/gradle-multimodule-example-spaces"));
      
      Assert.assertThat(modules.size(), Matchers.is(2));
   }
}
