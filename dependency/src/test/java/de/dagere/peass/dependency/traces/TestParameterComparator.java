package de.dagere.peass.dependency.traces;

import java.io.ByteArrayInputStream;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;

public class TestParameterComparator {

   @Test
   public void testNoParameterComparison() {
      String methodSource = "class Clazz{ class MyInner{ public void myMethod(){} } }";
      CompilationUnit declaration = new JavaParser().parse(new ByteArrayInputStream(methodSource.getBytes())).getResult().get();

      ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration.getChildNodes().get(0);
      ClassOrInterfaceDeclaration myInner = clazz.findAll(ClassOrInterfaceDeclaration.class).get(1);
      CallableDeclaration<?> method = myInner.findAll(CallableDeclaration.class).get(0);

      TraceElementContent traceElementMethod = new TraceElementContent("Clazz$MyInner", "myMethod", new String[0], 0);
      boolean isEqualMethod = new ParameterComparator(clazz).parametersEqual(traceElementMethod.toEntity(), method);
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
      boolean isEqualConstructorWrong = new ParameterComparator(clazz).parametersEqual(traceElementConstructorWrong.toEntity(), method);
      Assert.assertTrue(isEqualConstructorWrong);
   }
   
   @Test
   public void testStaticInnerConstructor() {
      String methodSource = "class Clazz{ static class MyInner{ public MyInner(int a){} } }";
      CompilationUnit declaration = new JavaParser().parse(new ByteArrayInputStream(methodSource.getBytes())).getResult().get();

      ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration.getChildNodes().get(0);
      ClassOrInterfaceDeclaration myInner = clazz.findAll(ClassOrInterfaceDeclaration.class).get(1);
      CallableDeclaration<?> method = myInner.findAll(CallableDeclaration.class).get(0);

      TraceElementContent traceElementConstructorWrong = new TraceElementContent("Clazz$MyInner", "<init>", new String[] { }, 0);
      boolean isEqualConstructor = new ParameterComparator(clazz).parametersEqual(traceElementConstructorWrong.toEntity(), method);
      Assert.assertFalse(isEqualConstructor);
   }

   @Test
   public void testStaticComparison() {
      String methodSource = "class Clazz{ class MyInner{ public static void myMethod(int i){} } }";
      CompilationUnit declaration = new JavaParser().parse(new ByteArrayInputStream(methodSource.getBytes())).getResult().get();

      ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration.getChildNodes().get(0);
      ClassOrInterfaceDeclaration innerClass = clazz.findAll(ClassOrInterfaceDeclaration.class).get(1);
      CallableDeclaration<?> method = innerClass.findAll(CallableDeclaration.class).get(0);

      TraceElementContent wrongTraceElement = new TraceElementContent("Clazz$MyInner", "myMethod", new String[0], 0);
      boolean isEqualWrong = new ParameterComparator(clazz).parametersEqual(wrongTraceElement.toEntity(), method);
      Assert.assertFalse(isEqualWrong);

      TraceElementContent correctTraceElement = new TraceElementContent("Clazz$MyInner", "myMethod", new String[] { "int" }, 0);
      boolean isEqualCorrect = new ParameterComparator(clazz).parametersEqual(correctTraceElement.toEntity(), method);
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

      boolean isEqual1 = new ParameterComparator(clazz).parametersEqual(traceElement.toEntity(), method);
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
      boolean isEqualConstructorWrong = new ParameterComparator(clazz).parametersEqual(traceElementConstructorWrong.toEntity(), method);
      Assert.assertFalse(isEqualConstructorWrong);
   }

   @Test
   public void testWrongMethodComparison() {
      String methodSource = "class Clazz{ class MyInner{ public void doStuff(){ } } }";
      CompilationUnit declaration = new JavaParser().parse(new ByteArrayInputStream(methodSource.getBytes())).getResult().get();

      ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration.getChildNodes().get(0);
      ClassOrInterfaceDeclaration myInner = clazz.findAll(ClassOrInterfaceDeclaration.class).get(1);
      CallableDeclaration<?> method = myInner.findAll(CallableDeclaration.class).get(0);

      TraceElementContent traceElementMethodWrong = new TraceElementContent("Clazz$MyInner", "doStuff", new String[] { "Clazz", "int" }, 0);
      boolean isEqualMethodWrong = new ParameterComparator(clazz).parametersEqual(traceElementMethodWrong.toEntity(), method);
      Assert.assertFalse(isEqualMethodWrong);
   }
   
   @Test
   public void testEnumMethodComparison() {
      String methodSource = "class Clazz{ enum MyInner{ A, B; public void doStuff(int a){ } } }";
      CompilationUnit declaration = new JavaParser().parse(new ByteArrayInputStream(methodSource.getBytes())).getResult().get();

      System.out.println(declaration);
      
      ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration.getChildNodes().get(0);
      EnumDeclaration myInner = clazz.findAll(EnumDeclaration.class).get(0);
      CallableDeclaration<?> method = myInner.findAll(CallableDeclaration.class).get(0);

      TraceElementContent traceElementMethodWrong = new TraceElementContent("Clazz$MyInner", "doStuff", new String[] { "int" }, 0);
      boolean isEqualMethodWrong = new ParameterComparator(clazz).parametersEqual(traceElementMethodWrong.toEntity(), method);
      Assert.assertTrue(isEqualMethodWrong);
   }
}
