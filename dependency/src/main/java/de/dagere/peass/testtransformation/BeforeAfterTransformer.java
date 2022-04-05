package de.dagere.peass.testtransformation;

import java.util.List;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import de.dagere.peass.config.ExecutionConfig;

public class BeforeAfterTransformer {
   
   public static void transformBeforeAfter(final ClassOrInterfaceDeclaration clazz, final ExecutionConfig config) {
      if (config.isOnlyMeasureWorkload()) {
         BeforeAfterTransformer.transformNoMeasurement(clazz);
      } else {
         if (config.isExecuteBeforeClassInMeasurement()) {
            BeforeAfterTransformer.transformWithMeasurement(clazz);
         }
      }
   }

   public static void transformWithMeasurement(final ClassOrInterfaceDeclaration clazz) {
      List<MethodDeclaration> beforeAllMethods = TestMethodFinder.findBeforeAllMethods(clazz);
      transformMethodAnnotations(beforeAllMethods, "de.dagere.kopeme.junit.rule.annotations.BeforeWithMeasurement", 2);
      
      List<MethodDeclaration> afterAllMethods = TestMethodFinder.findAfterAllMethods(clazz);
      transformMethodAnnotations(afterAllMethods, "de.dagere.kopeme.junit.rule.annotations.AfterWithMeasurement", 2);
   }

   public static void transformNoMeasurement(final ClassOrInterfaceDeclaration clazz) {
      List<MethodDeclaration> beforeEachMethods = TestMethodFinder.findBeforeEachMethods(clazz);
      transformMethodAnnotations(beforeEachMethods, "de.dagere.kopeme.junit.rule.annotations.BeforeNoMeasurement", 1);
      
      List<MethodDeclaration> beforeAllMethods = TestMethodFinder.findBeforeAllMethods(clazz);
      transformMethodAnnotations(beforeAllMethods, "de.dagere.kopeme.junit.rule.annotations.BeforeNoMeasurement", 2);
      
      List<MethodDeclaration> afterEachMethods = TestMethodFinder.findAfterEachMethods(clazz);
      transformMethodAnnotations(afterEachMethods, "de.dagere.kopeme.junit.rule.annotations.AfterNoMeasurement", 1);
      
      List<MethodDeclaration> afterAllMethods = TestMethodFinder.findAfterAllMethods(clazz);
      transformMethodAnnotations(afterAllMethods, "de.dagere.kopeme.junit.rule.annotations.AfterNoMeasurement", 2);
   }

   private static void transformMethodAnnotations(final List<MethodDeclaration> transformableMethods, final String name, final int priority) {
      for (MethodDeclaration method : transformableMethods) {
         final NormalAnnotationExpr beforeNoMeasurementAnnotation = new NormalAnnotationExpr();

         beforeNoMeasurementAnnotation.setName(name);
         method.getAnnotations().add(beforeNoMeasurementAnnotation);
         
         beforeNoMeasurementAnnotation.addPair("priority", Integer.toString(priority));

      }
   }
}
