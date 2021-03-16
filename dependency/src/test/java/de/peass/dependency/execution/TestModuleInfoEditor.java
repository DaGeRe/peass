package de.peass.dependency.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import de.peass.dependency.moduleinfo.ModuleInfoEditor;

public class TestModuleInfoEditor {

   private final File experimentFile = new File("target" + File.separator + "module-info.java");

   @Test
   public void testModuleInfoEditing() throws IOException {
      File originalPom = new File("src" + File.separator + "test" + File.separator + "resources" + File.separator + "module-example.java");

      FileUtils.copyFile(originalPom, experimentFile);

      ModuleInfoEditor.addKiekerRequires(experimentFile);
      
      boolean found = containsRequireKieker();
      Assert.assertTrue(found);
   }

   private boolean containsRequireKieker() throws IOException, FileNotFoundException {
      boolean found = false;
      try (BufferedReader reader = new BufferedReader(new FileReader(experimentFile))) {
        
         String line;
         while ((line = reader.readLine()) != null) {
            System.out.println(line);
            if (line.contains("requires kieker")) {
               found = true;
            }
         }
         
      }
      return found;
   }
}
