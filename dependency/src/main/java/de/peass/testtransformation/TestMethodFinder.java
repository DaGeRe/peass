package de.peass.testtransformation;

import java.util.LinkedList;
import java.util.List;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

public class TestMethodFinder {
   public static List<MethodDeclaration> getJUnit5TestMethods(final ClassOrInterfaceDeclaration clazz) {
      List<MethodDeclaration> testMethods = new LinkedList<>();
      for (final MethodDeclaration method : clazz.getMethods()) {
         boolean performanceTestFound = false;
         boolean testFound = false;
         for (final AnnotationExpr annotation : method.getAnnotations()) {
            final String currentName = annotation.getNameAsString();
            if (currentName.equals("de.dagere.kopeme.annotations.PerformanceTest") || currentName.equals("PerformanceTest")) {
               performanceTestFound = true;
            }
            if (currentName.equals("org.junit.Test") || currentName.equals("org.junit.jupiter.api.Test") || currentName.equals("Test")) {
               testFound = true;
            }
         }
         if (testFound && !performanceTestFound) {
            testMethods.add(method);
         }
      }
      return testMethods;
   }
   
   public static List<MethodDeclaration> findJUnit4TestMethods(final ClassOrInterfaceDeclaration clazz) {
      List<MethodDeclaration> testMethods = new LinkedList<>();
      for (final MethodDeclaration method : clazz.getMethods()) {
         boolean performanceTestFound = false;
         boolean testFound = false;
         for (final AnnotationExpr annotation : method.getAnnotations()) {
            final String currentName = annotation.getNameAsString();
            if (currentName.equals("de.dagere.kopeme.annotations.PerformanceTest") || currentName.equals("PerformanceTest")) {
               performanceTestFound = true;
            }
            if (currentName.equals("org.junit.Test") || currentName.equals("org.junit.jupiter.api.Test") || currentName.equals("Test")) {
               testFound = true;
            }
         }
         
         if (testFound && !performanceTestFound) {
            testMethods.add(method);
         }
      }
      return testMethods;
   }
}
