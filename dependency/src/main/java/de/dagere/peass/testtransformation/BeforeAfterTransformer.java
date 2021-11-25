package de.dagere.peass.testtransformation;

import java.util.List;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

public class BeforeAfterTransformer {

   public static void transformWithMeasurement(final ClassOrInterfaceDeclaration clazz) {
      List<MethodDeclaration> beforeMethods = TestMethodFinder.findBeforeMethods(clazz);
      transformMethodAnnotations(beforeMethods, "de.dagere.kopeme.junit.rule.annotations.BeforeWithMeasurement");
      
      List<MethodDeclaration> afterMethods = TestMethodFinder.findAfterMethods(clazz);
      transformMethodAnnotations(afterMethods, "de.dagere.kopeme.junit.rule.annotations.AfterWithMeasurement");
   }

   public static void transformBefore(final ClassOrInterfaceDeclaration clazz) {
      List<MethodDeclaration> beforeMethods = TestMethodFinder.findBeforeMethods(clazz);
      transformMethodAnnotations(beforeMethods, "de.dagere.kopeme.junit.rule.annotations.BeforeNoMeasurement");
   }

   public static void transformAfter(final ClassOrInterfaceDeclaration clazz) {
      List<MethodDeclaration> beforeMethods = TestMethodFinder.findAfterMethods(clazz);
      transformMethodAnnotations(beforeMethods, "de.dagere.kopeme.junit.rule.annotations.AfterNoMeasurement");
   }

   private static void transformMethodAnnotations(final List<MethodDeclaration> beforeMethods, final String name) {
      for (MethodDeclaration method : beforeMethods) {
         final NormalAnnotationExpr beforeNoMeasurementAnnotation = new NormalAnnotationExpr();

         beforeNoMeasurementAnnotation.setName(name);
         method.setAnnotation(0, beforeNoMeasurementAnnotation);

      }
   }
}
