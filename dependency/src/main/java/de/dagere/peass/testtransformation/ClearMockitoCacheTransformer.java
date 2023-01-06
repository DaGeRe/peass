package de.dagere.peass.testtransformation;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import de.dagere.peass.config.ExecutionConfig;

public class ClearMockitoCacheTransformer {

   private final ExecutionConfig executionConfig;
   private final ClassOrInterfaceDeclaration clazz;

   public ClearMockitoCacheTransformer(ExecutionConfig executionConfig, ClassOrInterfaceDeclaration clazz) {
      this.executionConfig = executionConfig;
      this.clazz = clazz;
   }

   public void transform() {
      createClearCacheMethod();

      List<String> toAddBeforeEachInitializers = new LinkedList<>();
      List<String> toAddBeforeAllInitializers = new LinkedList<>();
      for (FieldDeclaration field : clazz.getFields()) {
         for (VariableDeclarator variable : field.getVariables()) {
            Optional<Expression> initializer = variable.getInitializer();
            if (initializer.isPresent()) {
               String initializerText = variable.getInitializer().get().toString();
               if (initializerText.contains(".mock(")) {
                  if (field.isStatic()) {
                     toAddBeforeAllInitializers.add(variable.getNameAsString() + "=" + initializerText + ";");
                  } else {
                     toAddBeforeEachInitializers.add(variable.getNameAsString() + "=" + initializerText + ";");
                  }
                  variable.setInitializer((Expression) null);
                  field.setFinal(false);
               }
            }
         }
      }

      if (toAddBeforeEachInitializers.size() > 0) {
         MethodDeclaration firstBeforeEachMethod = getBeforeEachMethod();
         addInitializers(toAddBeforeEachInitializers, firstBeforeEachMethod);
      }
      
      if (toAddBeforeAllInitializers.size() > 0) {
         MethodDeclaration firstBeforeAllMethod = getBeforeAllMethod();
         addInitializers(toAddBeforeAllInitializers, firstBeforeAllMethod);
      }
   }

   private void addInitializers(List<String> toAddBeforeEachInitializers, MethodDeclaration firstBeforeEachMethod) {
      BlockStmt beforeEachMethodBody = firstBeforeEachMethod.getBody().get();
      for (String initialization : toAddBeforeEachInitializers) {
         Statement initStatement = StaticJavaParser.parseStatement(initialization);
         beforeEachMethodBody.addStatement(0, initStatement);
      }
   }

   private MethodDeclaration getBeforeEachMethod() {
      MethodDeclaration firstBeforeEachMethod = null;
      for (MethodDeclaration method : clazz.getMethods()) {
         if (method.getAnnotationByClass(BeforeEach.class).isPresent()) {
            return method;
         }
      }
      firstBeforeEachMethod = clazz.addMethod("_peass_setup_each", Keyword.PUBLIC);
      firstBeforeEachMethod.setBody(new BlockStmt());
      firstBeforeEachMethod.addAnnotation("org.junit.jupiter.api.BeforeEach");
      return firstBeforeEachMethod;
   }
   
   private MethodDeclaration getBeforeAllMethod() {
      MethodDeclaration firstBeforeEachMethod = null;
      for (MethodDeclaration method : clazz.getMethods()) {
         if (method.getAnnotationByClass(BeforeAll.class).isPresent()) {
            return method;
         }
      }
      firstBeforeEachMethod = clazz.addMethod("_peass_setup_all", Keyword.PUBLIC);
      firstBeforeEachMethod.setBody(new BlockStmt());
      firstBeforeEachMethod.addAnnotation("org.junit.jupiter.api.BeforeAll");
      return firstBeforeEachMethod;
   }

   private void createClearCacheMethod() {
      final MethodDeclaration newMethod;
      if (executionConfig.isExecuteBeforeClassInMeasurement()) {
         newMethod = clazz.addMethod("_peass_initializeMockito", Keyword.PUBLIC, Keyword.STATIC);

      } else {
         newMethod = clazz.addMethod("_peass_initializeMockito", Keyword.PUBLIC);
      }
      NormalAnnotationExpr afterWithMeasurementAnnotation = newMethod.addAndGetAnnotation("de.dagere.kopeme.junit.rule.annotations.AfterWithMeasurement");
      afterWithMeasurementAnnotation.addPair("priority", Integer.toString(5));
      newMethod.setBody(new BlockStmt());
      newMethod.getBody().get().addAndGetStatement(new MethodCallExpr("org.mockito.Mockito.clearAllCaches"));
   }
}
