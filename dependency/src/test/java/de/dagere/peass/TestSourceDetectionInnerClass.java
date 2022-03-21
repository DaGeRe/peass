package de.dagere.peass;

import java.io.File;
import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.dependency.traces.TraceElementContent;
import de.dagere.peass.dependency.traces.TraceReadUtils;

public class TestSourceDetectionInnerClass {
   @Test
   public void testInner() throws ParseException, IOException {
      final File file = new File(TestSourceDetection.SOURCE, "Test3_Inner.java");
      final CompilationUnit cu = JavaParserProvider.parse(file);

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
   public void testInnerWithSelfReference() throws ParseException, IOException {
      final File file = new File(TestSourceDetection.SOURCE, "Test3_Inner.java");
      final CompilationUnit cu = JavaParserProvider.parse(file);

      final TraceElementContent currentTraceElement = new TraceElementContent("Test3_Inner$InnerStuff", "<init>",
            new String[] { "Test3_Inner", "de.peass.InnerParameter1", "InnerParameter2" }, 1);
      final Node method = TraceReadUtils.getMethod(currentTraceElement, cu);

      System.out.println(method);

      Assert.assertNotNull(method);

      final TraceElementContent currentTraceElement2 = new TraceElementContent("Test3_Inner$InnerStuff$InnerInner", "doubleInnerMethod", new String[0], 1);
      final Node method2 = TraceReadUtils.getMethod(currentTraceElement2, cu);

      System.out.println(method2);

      Assert.assertNotNull(method2);
   }

   @Test
   public void testInnerWithWrongReference() throws ParseException, IOException {
      final File file = new File(TestSourceDetection.SOURCE, "Test3_Inner.java");
      final CompilationUnit cu = JavaParserProvider.parse(file);

      final TraceElementContent currentTraceElement = new TraceElementContent("Test3_Inner$InnerStuff", "<init>",
            new String[] { "SomeReferemce", "de.peass.InnerParameter1", "InnerParameter2" },
            1);
      final Node method = TraceReadUtils.getMethod(currentTraceElement, cu);

      Assert.assertNull(method);
   }

   @Test
   public void testAnonymousClazzes() throws ParseException, IOException {
      final File file = new File(TestSourceDetection.SOURCE, "Test1_Anonym.java");
      final CompilationUnit cu = JavaParserProvider.parse(file);

      final TraceElementContent currentTraceElement = new TraceElementContent("Test1_Anonym$1", "<init>", new String[0], 1);
      final Node method = TraceReadUtils.getMethod(currentTraceElement, cu);

      System.out.println(method);

      Assert.assertNull(method);

      final TraceElementContent traceElementRun1 = new TraceElementContent("Test1_Anonym$1", "run", new String[0], 1);
      final Node methodRun = TraceReadUtils.getMethod(traceElementRun1, cu);

      System.out.println(methodRun);

      Assert.assertNotNull(methodRun);
      MatcherAssert.assertThat(methodRun.toString(), Matchers.containsString("Run R3"));

      final TraceElementContent traceElementRun2 = new TraceElementContent("Test1_Anonym$2", "run", new String[0], 1);
      final Node methodRun2 = TraceReadUtils.getMethod(traceElementRun2, cu);

      System.out.println(methodRun2);

      Assert.assertNotNull(methodRun2);
      MatcherAssert.assertThat(methodRun2.toString(), Matchers.containsString("Run R1"));

      final TraceElementContent traceElementRun3 = new TraceElementContent("Test1_Anonym$3", "run", new String[0], 1);
      final Node methodRun3 = TraceReadUtils.getMethod(traceElementRun3, cu);

      System.out.println(methodRun3);

      Assert.assertNotNull(methodRun3);
      MatcherAssert.assertThat(methodRun3.toString(), Matchers.containsString("Run R2"));
   }

   @Test
   public void testNamedClazzes() throws ParseException, IOException {
      final File file = new File(TestSourceDetection.SOURCE, "Test2_Named.java");
      final CompilationUnit cu = JavaParserProvider.parse(file);

      final TraceElementContent currentTraceElement = new TraceElementContent("Test2_Named$MyStuff", "doMyStuff1", new String[0], 1);
      final Node methodRun = TraceReadUtils.getMethod(currentTraceElement, cu);

      System.out.println(methodRun);

      Assert.assertNotNull(methodRun);
      MatcherAssert.assertThat(methodRun.toString(), Matchers.containsString("stuff 1"));

      final TraceElementContent currentTraceElement2 = new TraceElementContent("Test2_Named$MyStuff2", "doMyStuff2", new String[0], 1);
      final Node methodRun2 = TraceReadUtils.getMethod(currentTraceElement2, cu);

      System.out.println(methodRun2);

      Assert.assertNotNull(methodRun2);
      MatcherAssert.assertThat(methodRun2.toString(), Matchers.containsString("stuff 2"));
   }
}
