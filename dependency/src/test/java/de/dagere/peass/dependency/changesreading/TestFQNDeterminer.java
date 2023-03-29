package de.dagere.peass.dependency.changesreading;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.CompilationUnit;

public class TestFQNDeterminer {

   @Test
   public void testImportedClass() throws FileNotFoundException {
      
      File dependencyReaderFile = new File("src/main/java/de/dagere/peass/dependency/reader/DependencyReader.java");
      CompilationUnit unit = JavaParserProvider.parse(dependencyReaderFile);
      String fqn = FQNDeterminer.getParameterFQN(unit, "StaticTestSelection");
      Assert.assertEquals("de.dagere.peass.dependency.persistence.StaticTestSelection", fqn);

      File selectFile = new File("src/main/java/de/dagere/peass/SelectStarter.java");
      CompilationUnit selectUnit = JavaParserProvider.parse(selectFile);
      String fqn2 = FQNDeterminer.getParameterFQN(selectUnit, "CommandLine");
      Assert.assertEquals("picocli.CommandLine", fqn2);
   }
   
   @Test
   public void testPackageClass() throws FileNotFoundException {
      File file = new File("src/main/java/de/dagere/peass/SelectStarter.java");
      CompilationUnit unit = JavaParserProvider.parse(file);
      String fqn = FQNDeterminer.getParameterFQN(unit, "SelectStarter");
      Assert.assertEquals("de.dagere.peass.SelectStarter", fqn);
   }

   

   
}
