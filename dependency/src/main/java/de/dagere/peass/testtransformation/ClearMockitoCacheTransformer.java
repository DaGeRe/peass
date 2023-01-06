package de.dagere.peass.testtransformation;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
      for (FieldDeclaration field : clazz.getFields()) {
         for (VariableDeclarator variable : field.getVariables()) {
            Optional<Expression> initializer = variable.getInitializer();
            if (initializer.isPresent()) {
               toAddBeforeEachInitializers.add(variable.getNameAsString() + "=" + variable.getInitializer().get() + ";");
               variable.setInitializer((Expression) null);
               field.setFinal(false);
            }
         }
      }

      MethodDeclaration firstBeforeEachMethod = getBeforeEachMethod();
      
      BlockStmt methodBody = firstBeforeEachMethod.getBody().get();
      for (String initialization : toAddBeforeEachInitializers) {
         System.out.println(initialization);
         Statement initStatement = StaticJavaParser.parseStatement(initialization);
         methodBody.addStatement(0, initStatement);
      }
   }

   private MethodDeclaration getBeforeEachMethod() {
      MethodDeclaration firstBeforeEachMethod = null;
      for (MethodDeclaration method : clazz.getMethods()) {
         if (method.getAnnotationByClass(BeforeEach.class).isPresent()) {
            firstBeforeEachMethod = method;
            continue;
         }
      }
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
