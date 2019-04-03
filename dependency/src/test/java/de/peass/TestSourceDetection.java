package de.peass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import de.peass.dependency.analysis.FileComparisonUtil;
import de.peass.dependency.traces.TraceReadUtils;
import de.peass.dependency.traces.requitur.content.TraceElementContent;

public class TestSourceDetection {
   private static final File SOURCE = new File("src/test/resources/detection/");

   @Test
   public void testInner() throws ParseException, IOException {
      final File file = new File(SOURCE, "Test3_Inner.java");
      final CompilationUnit cu = FileComparisonUtil.parse(file);

      final TraceElementContent currentTraceElement = new TraceElementContent("Test3_Inner$InnerStuff", "<init>", new String[] { "de.peass.InnerParameter1", "InnerParameter2" },
            1);
      final Node method = TraceReadUtils.getMethod(currentTraceElement, cu);

      System.out.println(method);

      Assert.assertNotNull(method);

      final TraceElementContent currentTraceElement2 = new TraceElementContent("Test3_Inner$InnerStuff$InnerInner", "doubleInnerMethod", new String[0], 1);
      final Node method2 = TraceReadUtils.getMethod(currentTraceElement2, cu);

      System.out.println(method2);

      Assert.assertNotNull(method2);
   }

   @Test
   public void testAnonymousClazzes() throws ParseException, IOException {
      final File file = new File(SOURCE, "Test1_Anonym.java");
      final CompilationUnit cu = FileComparisonUtil.parse(file);

      final TraceElementContent currentTraceElement = new TraceElementContent("Test1_Anonym$1", "<init>", new String[0], 1);
      final Node method = TraceReadUtils.getMethod(currentTraceElement, cu);

      System.out.println(method);

      Assert.assertNull(method);

      final TraceElementContent traceElementRun1 = new TraceElementContent("Test1_Anonym$1", "run", new String[0], 1);
      final Node methodRun = TraceReadUtils.getMethod(traceElementRun1, cu);

      System.out.println(methodRun);

      Assert.assertNotNull(methodRun);
      Assert.assertThat(methodRun.toString(), Matchers.containsString("Run R3"));

      final TraceElementContent traceElementRun2 = new TraceElementContent("Test1_Anonym$2", "run", new String[0], 1);
      final Node methodRun2 = TraceReadUtils.getMethod(traceElementRun2, cu);

      System.out.println(methodRun2);

      Assert.assertNotNull(methodRun2);
      Assert.assertThat(methodRun2.toString(), Matchers.containsString("Run R1"));

      final TraceElementContent traceElementRun3 = new TraceElementContent("Test1_Anonym$3", "run", new String[0], 1);
      final Node methodRun3 = TraceReadUtils.getMethod(traceElementRun3, cu);

      System.out.println(methodRun3);

      Assert.assertNotNull(methodRun3);
      Assert.assertThat(methodRun3.toString(), Matchers.containsString("Run R2"));
   }

   @Test
   public void testNamedClazzes() throws ParseException, IOException {
      final File file = new File(SOURCE, "Test2_Named.java");
      final CompilationUnit cu = FileComparisonUtil.parse(file);

      final TraceElementContent currentTraceElement = new TraceElementContent("Test2_Named$MyStuff", "doMyStuff1", new String[0], 1);
      final Node methodRun = TraceReadUtils.getMethod(currentTraceElement, cu);

      System.out.println(methodRun);

      Assert.assertNotNull(methodRun);
      Assert.assertThat(methodRun.toString(), Matchers.containsString("stuff 1"));

      final TraceElementContent currentTraceElement2 = new TraceElementContent("Test2_Named$MyStuff2", "doMyStuff2", new String[0], 1);
      final Node methodRun2 = TraceReadUtils.getMethod(currentTraceElement2, cu);

      System.out.println(methodRun2);

      Assert.assertNotNull(methodRun2);
      Assert.assertThat(methodRun2.toString(), Matchers.containsString("stuff 2"));
   }

   @Test
   public void testAnonymousList() throws FileNotFoundException {
      final File file = new File(SOURCE, "Test1_Anonym.java");
      final CompilationUnit cu = FileComparisonUtil.parse(file);
      final List<NodeList<BodyDeclaration<?>>> anonymous = TraceReadUtils.getAnonymusClasses(cu);

      Assert.assertEquals(3, anonymous.size());

      Assert.assertThat(anonymous.get(0).get(0).toString(), Matchers.containsString("Run R3"));
      Assert.assertThat(anonymous.get(1).get(0).toString(), Matchers.containsString("Run R1"));
      Assert.assertThat(anonymous.get(2).get(0).toString(), Matchers.containsString("Run R2"));
   }

   @Test
   public void testNamedList() throws FileNotFoundException {
      final File file = new File(SOURCE, "Test2_Named.java");
      final CompilationUnit cu = FileComparisonUtil.parse(file);
      final Map<String, TypeDeclaration<?>> named = TraceReadUtils.getNamedClasses(cu, "");

      Assert.assertEquals(3, named.size());

      Assert.assertThat(named.get("Test2_Named$MyStuff").toString(), Matchers.containsString("stuff 1"));
      Assert.assertThat(named.get("Test2_Named$MyStuff2").toString(), Matchers.containsString("stuff 2"));
   }

   @Test
   public void testDirectoryWalker() throws FileNotFoundException {
      final File file = new File(SOURCE, "DirectoryWalkerTestCase.java");
      final CompilationUnit cu = FileComparisonUtil.parse(file);
      final Map<String, TypeDeclaration<?>> named = TraceReadUtils.getNamedClasses(cu, "");

      Assert.assertEquals(4, named.size());

      Assert.assertThat(named.get("DirectoryWalkerTestCase$TestFileFinder").toString(), Matchers.containsString("List results"));
   }
}
