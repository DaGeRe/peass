package de.peass;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import de.peass.dependency.analysis.FileComparisonUtil;
import de.peass.dependency.analysis.data.TraceElement;
import de.peass.dependency.traces.TraceReadUtils;
import de.peass.dependency.traces.requitur.content.TraceElementContent;

public class TestSourceReading {

   @Test
   public void testAnonymousClass() throws FileNotFoundException {
      final CompilationUnit cu = FileComparisonUtil.parse(new File("src/test/resources/methodFinding/AnonymousClassExample.java"));

      final TraceElementContent anonymousTrace = new TraceElementContent("AnonymousClassExample$1", "run", new String[0], 1);
      final Node anonymousMethod = TraceReadUtils.getMethod(anonymousTrace, cu);

      Assert.assertNotNull(anonymousMethod);

      final TraceElementContent elementConstuctor = new TraceElementContent("AnonymousClassExample$MyPrivateClass", "<init>", new String[0], 1);
      final Node methodConstructor = TraceReadUtils.getMethod(elementConstuctor, cu);

      Assert.assertNotNull(methodConstructor);

      final TraceElementContent elementInnerMethod = new TraceElementContent("AnonymousClassExample$MyPrivateClass", "doSomething", new String[0], 1);
      final Node innerMethod = TraceReadUtils.getMethod(elementInnerMethod, cu);

      Assert.assertNotNull(innerMethod);
   }
   
   @Test
   public void testInnerConstructor() throws FileNotFoundException {
      final CompilationUnit cu = FileComparisonUtil.parse(new File("src/test/resources/methodFinding/AnonymousClassExample.java"));

      final TraceElementContent anonymousTrace = new TraceElementContent("AnonymousClassExample$MyPrivateClass", "<init>", new String[] {"int"}, 1);
      final Node anonymousMethod = TraceReadUtils.getMethod(anonymousTrace, cu);
      
      System.out.println(anonymousMethod);
      
      Assert.assertNotNull(anonymousMethod);
   }

   @Test
   public void testParameters() throws FileNotFoundException {
      final CompilationUnit cu = FileComparisonUtil.parse(new File("src/test/resources/methodFinding/AnonymousClassExample.java"));

      final TraceElement te = new TraceElement("AnonymousClassExample", "parameterMethod", 1);
      te.setParameterTypes(new String[] {"int"});
      final TraceElementContent anonymousTrace = new TraceElementContent(te);
      final Node anonymousMethod = TraceReadUtils.getMethod(anonymousTrace, cu);
      Assert.assertNotNull(anonymousMethod);
      
      final TraceElement te2 = new TraceElement("AnonymousClassExample", "parameterMethod", 1);
      te2.setParameterTypes(new String[] {"String"});
      final TraceElementContent anonymousTrace2 = new TraceElementContent(te2);
      final Node anonymousMethod2 = TraceReadUtils.getMethod(anonymousTrace2, cu);
      Assert.assertNotNull(anonymousMethod2);
      
      final TraceElement te3 = new TraceElement("AnonymousClassExample", "parameterMethod", 1);
      te3.setParameterTypes(new String[] {"Long"});
      final TraceElementContent anonymousTrace3 = new TraceElementContent(te3);
      final Node anonymousMethod3 = TraceReadUtils.getMethod(anonymousTrace3, cu);
      Assert.assertNull(anonymousMethod3);
      
      final TraceElement teSmall = new TraceElement("AnonymousClassExample", "parameterMethod", 1);
      teSmall.setParameterTypes(new String[] {});
      final TraceElementContent anonymousTraceSmall = new TraceElementContent(teSmall);
      final Node anonymousMethodSmall = TraceReadUtils.getMethod(anonymousTraceSmall, cu);
      Assert.assertNull(anonymousMethodSmall);
   }
   
   @Test
   public void testVarArgs() throws FileNotFoundException {
      final CompilationUnit cu = FileComparisonUtil.parse(new File("src/test/resources/methodFinding/AnonymousClassExample.java"));
      
      final TraceElement teVarArg = new TraceElement("AnonymousClassExample", "parameterMethod", 1);
      teVarArg.setParameterTypes(new String[] {"Object", "String"});
      final TraceElementContent anonymousTraceVarArg = new TraceElementContent(teVarArg);
      final Node anonymousMethodVarArg = TraceReadUtils.getMethod(anonymousTraceVarArg, cu);
      Assert.assertNotNull(anonymousMethodVarArg);
      
      final TraceElement teVarArg2 = new TraceElement("AnonymousClassExample", "parameterMethod", 1);
      teVarArg2.setParameterTypes(new String[] {"Object", "String", "String"});
      final TraceElementContent anonymousTraceVarArg2 = new TraceElementContent(teVarArg2);
      final Node anonymousMethodVarArg2 = TraceReadUtils.getMethod(anonymousTraceVarArg2, cu);
      Assert.assertNotNull(anonymousMethodVarArg2);
      
      final TraceElement teVarArg3 = new TraceElement("AnonymousClassExample", "parameterMethod", 1);
      teVarArg3.setParameterTypes(new String[] {"Object"});
      final TraceElementContent anonymousTraceVarArg3 = new TraceElementContent(teVarArg3);
      final Node anonymousMethodVarArg3 = TraceReadUtils.getMethod(anonymousTraceVarArg3, cu);
      Assert.assertNotNull(anonymousMethodVarArg3);
      
      final TraceElement teVarArg4 = new TraceElement("AnonymousClassExample", "parameterMethod", 1);
      teVarArg4.setParameterTypes(new String[] {"Object", "String", "String", "String"});
      final TraceElementContent anonymousTraceVarArg4 = new TraceElementContent(teVarArg4);
      final Node anonymousMethodVarArg4 = TraceReadUtils.getMethod(anonymousTraceVarArg4, cu);
      Assert.assertNotNull(anonymousMethodVarArg4);
      
      final TraceElement teVarArgWrong = new TraceElement("AnonymousClassExample", "parameterMethod", 1);
      teVarArgWrong.setParameterTypes(new String[] {"Object", "Long"});
      final TraceElementContent anonymousTraceVarArgWrong = new TraceElementContent(teVarArgWrong);
      final Node anonymousMethodVarArgWrong = TraceReadUtils.getMethod(anonymousTraceVarArgWrong, cu);
      Assert.assertNull(anonymousMethodVarArgWrong);
      
      final TraceElement teVarArgWrong2 = new TraceElement("AnonymousClassExample", "parameterMethod", 1);
      teVarArgWrong2.setParameterTypes(new String[] {"Object", "String", "Long"});
      final TraceElementContent anonymousTraceVarArgWrong2 = new TraceElementContent(teVarArgWrong2);
      final Node anonymousMethodVarArgWrong2 = TraceReadUtils.getMethod(anonymousTraceVarArgWrong2, cu);
      Assert.assertNull(anonymousMethodVarArgWrong2);
   }
}
