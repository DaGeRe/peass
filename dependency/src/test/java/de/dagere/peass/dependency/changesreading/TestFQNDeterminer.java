package de.dagere.peass.dependency.changesreading;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.CompilationUnit;

public class TestFQNDeterminer {

   @Test
   public void testTypeItself() throws FileNotFoundException {
      File file = new File("src/main/java/de/dagere/peass/SelectStarter.java");
      CompilationUnit unit = JavaParserProvider.parse(file);
      String fqn = FQNDeterminer.getParameterFQN(unit, "SelectStarter");
      Assert.assertEquals("de.dagere.peass.SelectStarter", fqn);
   }

   @Test
   public void testInnerClass() throws FileNotFoundException {
      File file = new File("src/test/resources/clazzFinderExample/src/main/java/de/TestMe2.java");
      CompilationUnit unit = JavaParserProvider.parse(file);

      String fqnTest = FQNDeterminer.getParameterFQN(unit, "TestMe2");
      Assert.assertEquals("de.TestMe2", fqnTest);

      String fqnInner = FQNDeterminer.getParameterFQN(unit, "Inner");
      Assert.assertEquals("de.TestMe2$Inner", fqnInner);

      String fqnInnerInner = FQNDeterminer.getParameterFQN(unit, "InnerInner");
      Assert.assertEquals("de.TestMe2$Inner$InnerInner", fqnInnerInner);

      String fqnEnum = FQNDeterminer.getParameterFQN(unit, "InnerEnum");
      Assert.assertEquals("de.TestMe2$InnerEnum", fqnEnum);

      String fqnSecond = FQNDeterminer.getParameterFQN(unit, "Second");
      Assert.assertEquals("de.Second", fqnSecond);
   }

   @Test
   public void testImportedClass() throws FileNotFoundException {
      File file = new File("src/main/java/de/dagere/peass/SelectStarter.java");
      CompilationUnit unit = JavaParserProvider.parse(file);
      String fqn = FQNDeterminer.getParameterFQN(unit, "StaticTestSelection");
      Assert.assertEquals("de.dagere.peass.dependency.persistence.StaticTestSelection", fqn);

      String fqn2 = FQNDeterminer.getParameterFQN(unit, "CommandLine");
      Assert.assertEquals("picocli.CommandLine", fqn2);
   }
   
   @Test
   public void testSimpleType() throws FileNotFoundException {
      File file = new File("src/main/java/de/dagere/peass/SelectStarter.java");
      CompilationUnit unit = JavaParserProvider.parse(file);
      String fqn = FQNDeterminer.getParameterFQN(unit, "int");
      Assert.assertEquals("int", fqn);

      String fqn2 = FQNDeterminer.getParameterFQN(unit, "double");
      Assert.assertEquals("double", fqn2);
   }

   @Test
   public void testPackageClass() throws FileNotFoundException {
      File file = new File("src/main/java/de/dagere/peass/SelectStarter.java");
      CompilationUnit unit = JavaParserProvider.parse(file);
      String fqn = FQNDeterminer.getParameterFQN(unit, "SelectStarter");
      Assert.assertEquals("de.dagere.peass.SelectStarter", fqn);
   }

   @Test
   public void testJavaLangClass() throws FileNotFoundException {
      File file = new File("src/main/java/de/dagere/peass/SelectStarter.java");
      CompilationUnit unit = JavaParserProvider.parse(file);
      String fqn = FQNDeterminer.getParameterFQN(unit, "Object");
      Assert.assertEquals("java.lang.Object", fqn);

      String fqn2 = FQNDeterminer.getParameterFQN(unit, "String");
      Assert.assertEquals("java.lang.String", fqn2);
   }

   @Test
   public void testJavaLangGenericClass() throws FileNotFoundException {
      File file = new File("src/main/java/de/dagere/peass/SelectStarter.java");
      CompilationUnit unit = JavaParserProvider.parse(file);
      String fqn = FQNDeterminer.getParameterFQN(unit, "Class");
      Assert.assertEquals("java.lang.Class", fqn);
   }
}
