package net.kieker.sourceinstrumentation;

import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import net.kieker.sourceinstrumentation.instrument.ReachabilityDecider;

public class TestUnreachabilityDecider {
   
   @Test
   public void testException() {
      CompilationUnit unit = new JavaParser().parse("class Test { public void test() { throw new RuntimeException(); } }").getResult().get();
      MethodDeclaration method = unit.getClassByName("Test").get().getMethodsByName("test").get(0);
      boolean isUnreachable = ReachabilityDecider.isAfterUnreachable(method.getBody().get());
      Assert.assertTrue(isUnreachable);
   }
   
   @Test
   public void testCatchException() {
      CompilationUnit unit = new JavaParser().parse("class Test { public void test() { try { throw new RuntimeException(); } catch (Throwable t) {throw new RuntimeException(); } } }").getResult().get();
      MethodDeclaration method = unit.getClassByName("Test").get().getMethodsByName("test").get(0);
      boolean isUnreachable = ReachabilityDecider.isAfterUnreachable(method.getBody().get());
      Assert.assertTrue(isUnreachable);
   }
   
   @Test
   public void testCatchNotUnreachable() {
      CompilationUnit unit = new JavaParser().parse("class Test { public void test() { try { } catch (Throwable t) {throw new RuntimeException(); } } }").getResult().get();
      MethodDeclaration method = unit.getClassByName("Test").get().getMethodsByName("test").get(0);
      boolean isUnreachable = ReachabilityDecider.isAfterUnreachable(method.getBody().get());
      Assert.assertFalse(isUnreachable);
   }

   @Test
   public void testWhileLoop() {
      CompilationUnit unit = new JavaParser().parse("class Test { public void test() { while (true) { System.out.println();} } }").getResult().get();
      MethodDeclaration method = unit.getClassByName("Test").get().getMethodsByName("test").get(0);
      boolean isUnreachable = ReachabilityDecider.isAfterUnreachable(method.getBody().get());
      Assert.assertTrue(isUnreachable);
   }

   @Test
   public void testDoWhileLoop() {
      CompilationUnit unit = new JavaParser().parse("class Test { public void test() { do { System.out.println();} while (true);} }").getResult().get();
      MethodDeclaration method = unit.getClassByName("Test").get().getMethodsByName("test").get(0);
      boolean isUnreachable = ReachabilityDecider.isAfterUnreachable(method.getBody().get());
      Assert.assertTrue(isUnreachable);
   }
   
   @Test
   public void testRegularLoop() {
      CompilationUnit unit = new JavaParser().parse("class Test { public void test() { do { System.out.println();} while (a=3);} }").getResult().get();
      MethodDeclaration method = unit.getClassByName("Test").get().getMethodsByName("test").get(0);
      boolean isUnreachable = ReachabilityDecider.isAfterUnreachable(method.getBody().get());
      Assert.assertFalse(isUnreachable);
   }
}
