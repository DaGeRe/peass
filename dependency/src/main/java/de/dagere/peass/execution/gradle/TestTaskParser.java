package de.dagere.peass.execution.gradle;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;

public class TestTaskParser {

   private int jvmArgsLine = -1;
   private String testJvmArgsText;
   private int propertiesLine = -1;
   private int maxHeapSizeLine = -1;
   private boolean systemPropertiesBlock = false;
   private final Map<String, Integer> executionProperties = new LinkedHashMap<>();

   public TestTaskParser(ArgumentListExpression arguments) {
      parseTaskWithPotentialSystemProperties(arguments);
   }

   public void parseTaskWithPotentialSystemProperties(ArgumentListExpression arguments) {
      for (Expression argument : arguments.getExpressions()) {
         if (argument instanceof ClosureExpression) {
            ClosureExpression closure = (ClosureExpression) argument;
            BlockStatement blockStatement = (BlockStatement) closure.getCode();
            for (Statement statement : blockStatement.getStatements()) {
               ExpressionStatement potentialSystemProperties = null;
               if (statement instanceof ReturnStatement) {
                  ReturnStatement returnStatement = (ReturnStatement) statement;
                  Expression expression = returnStatement.getExpression();
                  String expressionText = expression.getText();
                  if (expressionText.contains("systemProperties") || expressionText.contains("systemProperty")) {
                     potentialSystemProperties = new ExpressionStatement(expression);
                  } else if (expressionText.contains("jvmArgs")) {
                     parseJvmArgs(expression);
                  } else if (expressionText.contains("maxHeapSize")) {
                     maxHeapSizeLine = expression.getLineNumber();
                  }
               } else if (statement instanceof ExpressionStatement) {
                  potentialSystemProperties = (ExpressionStatement) statement;
                  Expression expression = potentialSystemProperties.getExpression();
                  if (expression.getText().contains("jvmArgs")) {
                     parseJvmArgs(expression);
                  } else if (expression.getText().contains("maxHeapSize")) {
                     maxHeapSizeLine = expression.getLineNumber();
                  }
               }
               if (potentialSystemProperties != null && potentialSystemProperties.getExpression() instanceof MethodCallExpression) {
                  propertiesLine = getPropertiesLine(potentialSystemProperties);
               }
            }
         } else {
            if (argument instanceof MethodCallExpression) {
               MethodCallExpression expression = (MethodCallExpression) argument;
               // System.out.println(expression.getArguments());
               if (expression.getArguments() instanceof ArgumentListExpression) {
                  ArgumentListExpression innerArguments = (ArgumentListExpression) expression.getArguments();
                  parseTaskWithPotentialSystemProperties(innerArguments);
               }
            }
         }

      }
   }

   private void parseJvmArgs(Expression expression) {
      jvmArgsLine = expression.getLineNumber();
      if (expression instanceof BinaryExpression) {
         BinaryExpression binaryExpression = (BinaryExpression) expression;
         Expression rightExpression = binaryExpression.getRightExpression();
         testJvmArgsText = rightExpression.getText();
      }
   }

   private int getPropertiesLine(ExpressionStatement potentialSystemProperties) {
      int propertiesLine = getPropertiesLine();

      MethodCallExpression methodCallExpression = (MethodCallExpression) potentialSystemProperties.getExpression();
      String method = methodCallExpression.getMethodAsString();
      ArgumentListExpression propertiesArguments = (ArgumentListExpression) methodCallExpression.getArguments();
      if (method.equals("systemProperties")) {
         MapExpression map = (MapExpression) propertiesArguments.getExpression(0);
         if (executionProperties != null) {
            systemPropertiesBlock = true;
            for (MapEntryExpression expression : map.getMapEntryExpressions()) {
               String key = expression.getKeyExpression().getText();
               String value = expression.getValueExpression().getText();
               propertiesLine = expression.getLineNumber();
               addExecutionProperties(executionProperties, expression.getLineNumber(), key, value);
            }
         }
      } else if (method.equals("systemProperty")) {
         if (executionProperties != null) {
            systemPropertiesBlock = false;
            propertiesLine = propertiesArguments.getExpression(0).getLineNumber();
            String key = propertiesArguments.getExpression(0).getText();
            String value = propertiesArguments.getExpression(1).getText();
            addExecutionProperties(executionProperties, propertiesLine, key, value);
         }
      }
      return propertiesLine;
   }

   private static void addExecutionProperties(Map<String, Integer> executionProperties, int line, String key, String value) {
      if (key.startsWith(GradleBuildfileVisitor.JUPITER_EXECUTION_CONFIG_DEFAULT) && value.contains("concurrent")) {
         executionProperties.put(GradleBuildfileVisitor.JUPITER_EXECUTION_CONFIG_DEFAULT, line);
      }
      if (key.startsWith(GradleBuildfileVisitor.JUPITER_EXECUTION_CONFIG) && value.contains("true")) {
         executionProperties.put(GradleBuildfileVisitor.JUPITER_EXECUTION_CONFIG, line);
      }
   }
   
   public void increaseLines(int addedLineIndex) {
      if (addedLineIndex < jvmArgsLine && jvmArgsLine != -1) {
         jvmArgsLine++;
      }
      
      if (addedLineIndex < propertiesLine && propertiesLine != -1) {
         propertiesLine++;
         for (Map.Entry<String, Integer> entry : executionProperties.entrySet()) {
            entry.setValue(entry.getValue() + 1);
         }
      }
      
      if (addedLineIndex < maxHeapSizeLine && maxHeapSizeLine != -1) {
         maxHeapSizeLine++;
      }
   }

   public int getJvmArgsLine() {
      return jvmArgsLine;
   }

   public String getTestJvmArgsText() {
      return testJvmArgsText;
   }

   public int getPropertiesLine() {
      return propertiesLine;
   }
   
   public int getMaxHeapSizeLine() {
      return maxHeapSizeLine;
   }
   
   public boolean isSystemPropertiesBlock() {
      return systemPropertiesBlock;
   }
   
   public Map<String, Integer> getExecutionProperties() {
      return executionProperties;
   }
}
