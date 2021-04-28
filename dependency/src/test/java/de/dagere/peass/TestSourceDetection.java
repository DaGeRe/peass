package de.dagere.peass;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.dependency.traces.TraceReadUtils;

public class TestSourceDetection {
   public static final File SOURCE = new File("src" + File.separator + "test" + File.separator + "resources" + File.separator + "detection");

   @Test
   public void testAnonymousList() throws FileNotFoundException {
      final File file = new File(SOURCE, "Test1_Anonym.java");
      final CompilationUnit cu = JavaParserProvider.parse(file);
      final List<NodeList<BodyDeclaration<?>>> anonymous = TraceReadUtils.getAnonymusClasses(cu);

      Assert.assertEquals(3, anonymous.size());

      Assert.assertThat(anonymous.get(0).get(0).toString(), Matchers.containsString("Run R3"));
      Assert.assertThat(anonymous.get(1).get(0).toString(), Matchers.containsString("Run R1"));
      Assert.assertThat(anonymous.get(2).get(0).toString(), Matchers.containsString("Run R2"));
   }

   @Test
   public void testNamedList() throws FileNotFoundException {
      final File file = new File(SOURCE, "Test2_Named.java");
      final CompilationUnit cu = JavaParserProvider.parse(file);
      final Map<String, TypeDeclaration<?>> named = TraceReadUtils.getNamedClasses(cu, "");

      Assert.assertEquals(3, named.size());

      Assert.assertThat(named.get("Test2_Named$MyStuff").toString(), Matchers.containsString("stuff 1"));
      Assert.assertThat(named.get("Test2_Named$MyStuff2").toString(), Matchers.containsString("stuff 2"));
   }

   @Test
   public void testDirectoryWalker() throws FileNotFoundException {
      final File file = new File(SOURCE, "DirectoryWalkerTestCase.java");
      final CompilationUnit cu = JavaParserProvider.parse(file);
      final Map<String, TypeDeclaration<?>> named = TraceReadUtils.getNamedClasses(cu, "");

      Assert.assertEquals(4, named.size());

      Assert.assertThat(named.get("DirectoryWalkerTestCase$TestFileFinder").toString(), Matchers.containsString("List results"));
   }
}
