package de.dagere.peass.testtransformation;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.TestRule;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.dagere.peass.config.MeasurementConfig;

public class JUnit4Helper {

   private static final Logger LOG = LogManager.getLogger(JUnit4Helper.class);

   public static void editJUnit4(final CompilationUnit unit, final MeasurementConfig config, final DataCollectorList datacollectorlist) {
      unit.addImport("de.dagere.kopeme.annotations.MaximalRelativeStandardDeviation");
      unit.addImport("org.junit.rules.TestRule");
      unit.addImport("org.junit.Rule");
      unit.addImport("de.dagere.kopeme.junit4.rule.KoPeMeRule");

      final ClassOrInterfaceDeclaration clazz = ParseUtil.getClasses(unit).get(0);

      JUnit4Helper.addKoPeMeRuleIfNecessary(clazz);

      List<MethodDeclaration> testMethods = TestMethodFinder.findJUnit4TestMethods(clazz);
      new TestMethodHelper(config, datacollectorlist).prepareTestMethods(testMethods);

      BeforeAfterTransformer.transformBeforeAfter(clazz, config.getExecutionConfig());
   }

   public static void addKoPeMeRuleIfNecessary(final ClassOrInterfaceDeclaration clazz) {
      final boolean fieldFound = JUnit4Helper.hasKoPeMeRule(clazz) || hasKoPeMeRunner(clazz);
      if (!fieldFound) {
         addRule(clazz);
      }
   }

   public static boolean hasKoPeMeRule(final ClassOrInterfaceDeclaration clazz) {
      boolean fieldFound = false;
      for (final FieldDeclaration field : clazz.getFields()) {
         // System.out.println(field + " " + field.getClass());
         boolean annotationFound = false;
         for (final AnnotationExpr ano : field.getAnnotations()) {
            // System.err.println(ano.getNameAsString());
            if (ano.getNameAsString().equals("Rule")) {
               annotationFound = true;
            }
         }
         if (annotationFound) {
            for (final Node node : field.getChildNodes()) {
               if (node instanceof VariableDeclarator) {
                  final VariableDeclarator potentialInitializer = (VariableDeclarator) node;
                  if (potentialInitializer.getInitializer().isPresent() && potentialInitializer.getInitializer().get().isObjectCreationExpr()) {
                     final Expression initializer = potentialInitializer.getInitializer().get();
                     if (initializer instanceof ObjectCreationExpr) {
                        final ObjectCreationExpr expression = (ObjectCreationExpr) initializer;
                        // System.out.println(expression.getTypeAsString());
                        if (expression.getTypeAsString().equals("KoPeMeRule")) {
                           fieldFound = true;
                        }
                     }
                  }
               }
            }
         }
      }
      return fieldFound;
   }

   public static boolean hasKoPeMeRunner(final ClassOrInterfaceDeclaration clazz) {
      boolean kopemeTestrunner = false;
      if (clazz.getAnnotations().size() > 0) {
         for (final AnnotationExpr annotation : clazz.getAnnotations()) {
            if (annotation.getNameAsString().contains("RunWith") && annotation instanceof SingleMemberAnnotationExpr) {
               final SingleMemberAnnotationExpr singleMember = (SingleMemberAnnotationExpr) annotation;
               final Expression expr = singleMember.getMemberValue();
               if (expr.toString().equals("PerformanceTestRunnerJUnit.class")) {
                  kopemeTestrunner = true;
               }
            }
         }
      }
      return kopemeTestrunner;
   }

   public static void addRule(final ClassOrInterfaceDeclaration clazz) {
      final NodeList<Expression> arguments = new NodeList<>();
      arguments.add(new ThisExpr());
      final Expression initializer = new ObjectCreationExpr(null, new ClassOrInterfaceType("KoPeMeRule"), arguments);
      final FieldDeclaration fieldDeclaration = clazz.addFieldWithInitializer(TestRule.class, "kopemeRule", initializer, Modifier.publicModifier().getKeyword());
      final NormalAnnotationExpr annotation = new NormalAnnotationExpr();
      annotation.setName("Rule");
      fieldDeclaration.getAnnotations().add(annotation);
   }
}
