package de.dagere.peass.dependency.traces;

import java.io.ByteArrayInputStream;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import de.dagere.peass.dependency.traces.requitur.content.TraceElementContent;

public class TestParameterComparator {

   @Test
   public void testNoParameterComparison() {
      String methodSource = "class Clazz{ class MyInner{ public void myMethod(){} } }";
      CompilationUnit declaration = new JavaParser().parse(new ByteArrayInputStream(methodSource.getBytes())).getResult().get();

      ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration.getChildNodes().get(0);
      ClassOrInterfaceDeclaration myInner = clazz.findAll(ClassOrInterfaceDeclaration.class).get(1);
      CallableDeclaration<?> method = myInner.findAll(CallableDeclaration.class).get(0);

      TraceElementContent traceElementMethod = new TraceElementContent("Clazz$MyInner", "myMethod", new String[0], 0);
      boolean isEqualMethod = new ParameterComparator(clazz).parametersEqual(traceElementMethod, method);
      Assert.assertTrue(isEqualMethod);
   }
   
   @Test
   public void testSimpleConstructor() {
      String methodSource = "class Clazz{ class MyInner{ public MyInner(){} } }";
      CompilationUnit declaration = new JavaParser().parse(new ByteArrayInputStream(methodSource.getBytes())).getResult().get();

      ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration.getChildNodes().get(0);
      ClassOrInterfaceDeclaration myInner = clazz.findAll(ClassOrInterfaceDeclaration.class).get(1);
      CallableDeclaration<?> method = myInner.findAll(CallableDeclaration.class).get(0);

      TraceElementContent traceElementConstructorWrong = new TraceElementContent("Clazz$MyInner", "<init>", new String[] { "Clazz" }, 0);
      boolean isEqualConstructorWrong = new ParameterComparator(clazz).parametersEqual(traceElementConstructorWrong, method);
      Assert.assertTrue(isEqualConstructorWrong);
   }

   @Test
   public void testStaticComparison() {
      String methodSource = "class Clazz{ class MyInner{ public static void myMethod(int i){} } }";
      CompilationUnit declaration = new JavaParser().parse(new ByteArrayInputStream(methodSource.getBytes())).getResult().get();

      ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration.getChildNodes().get(0);
      ClassOrInterfaceDeclaration innerClass = clazz.findAll(ClassOrInterfaceDeclaration.class).get(1);
      CallableDeclaration<?> method = innerClass.findAll(CallableDeclaration.class).get(0);

      TraceElementContent wrongTraceElement = new TraceElementContent("Clazz$MyInner", "myMethod", new String[0], 0);
      boolean isEqualWrong = new ParameterComparator(clazz).parametersEqual(wrongTraceElement, method);
      Assert.assertFalse(isEqualWrong);

      TraceElementContent correctTraceElement = new TraceElementContent("Clazz$MyInner", "myMethod", new String[] { "int" }, 0);
      boolean isEqualCorrect = new ParameterComparator(clazz).parametersEqual(correctTraceElement, method);
      Assert.assertTrue(isEqualCorrect);
   }

   @Test
   public void testDoubleInnerComparison() {
      TraceElementContent traceElement = new TraceElementContent("Clazz$MyInner$SecondInner", "<init>", new String[] { "Clazz$MyInner", "int" }, 0);
      String methodSource = "class Clazz{ class MyInner{ class SecondInner { public SecondInner(int i){} } } } ";
      CompilationUnit declaration = new JavaParser().parse(new ByteArrayInputStream(methodSource.getBytes())).getResult().get();

      ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration.getChildNodes().get(0);
      ClassOrInterfaceDeclaration myInner = clazz.findAll(ClassOrInterfaceDeclaration.class).get(1);
      ClassOrInterfaceDeclaration secondInnerClass = myInner.findAll(ClassOrInterfaceDeclaration.class).get(1);
      CallableDeclaration<?> method = secondInnerClass.findAll(CallableDeclaration.class).get(0);

      boolean isEqual1 = new ParameterComparator(clazz).parametersEqual(traceElement, method);
      Assert.assertTrue(isEqual1);
   }
   
   @Test
   public void testWrongConstructorComparison() {
      String methodSource = "class Clazz{ class MyInner{ public MyInner(){ } } }";
      CompilationUnit declaration = new JavaParser().parse(new ByteArrayInputStream(methodSource.getBytes())).getResult().get();

      ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration.getChildNodes().get(0);
      ClassOrInterfaceDeclaration myInner = clazz.findAll(ClassOrInterfaceDeclaration.class).get(1);
      CallableDeclaration<?> method = myInner.findAll(CallableDeclaration.class).get(0);

      TraceElementContent traceElementConstructorWrong = new TraceElementContent("Clazz$MyInner", "<init>", new String[] { "Clazz", "int" }, 0);
      boolean isEqualConstructorWrong = new ParameterComparator(clazz).parametersEqual(traceElementConstructorWrong, method);
      Assert.assertFalse(isEqualConstructorWrong);
   }
}
