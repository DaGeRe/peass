package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;

import de.dagere.peass.config.ExecutionConfig;

public class GradleBuildfileVisitor extends CodeVisitorSupport {

   private static final Logger LOG = LogManager.getLogger(GradleBuildfileVisitor.class);
   private static final String ADDEDBYPEASS = " // Added dynamically by Peass.";
   public static final String JUPITER_EXECUTION_CONFIG = "junit.jupiter.execution.parallel.enabled";
   public static final String JUPITER_EXECUTION_CONFIG_DEFAULT = "junit.jupiter.execution.parallel.mode.default";

   private int offset = 0;
   private int dependencyLine = -1;
   private int testLine = -1;
   private int testSystemPropertiesLine = -1;
   private int integrationTestLine = -1;
   private int integrationTestSystemPropertiesLine = -1;

   private int androidLine = -1;
   private int testOptionsAndroid = -1;
   private int unitTestsAll = -1;
   private int buildTools = -1;
   private int buildToolsVersion = -1;
   private int allConfigurationsLine = -1;
   private MethodCallExpression configurations = null;

   private List<Integer> excludeLines = new LinkedList<>();
   private boolean hasVersion = true;
   private boolean subprojectJava = false;
   private MutableBoolean testSystemPropertiesBlock = new MutableBoolean(false);
   private MutableBoolean integrationTestSystemPropertiesBlock= new MutableBoolean(false);
   private List<String> gradleFileContents;
   private Map<String, Integer> testExecutionProperties = new HashMap<>();
   private Map<String, Integer> integrationtestExecutionProperties = new HashMap<>();

   private final ExecutionConfig config;

   public GradleBuildfileVisitor(final File buildfile, ExecutionConfig config) throws IOException {
      this.config = config;
      gradleFileContents = Files.readAllLines(Paths.get(buildfile.toURI()));

      try (Stream<String> lines = Files.lines(buildfile.toPath())) {
         final AstBuilder builder = new AstBuilder();

         String content = lines.filter(line -> !line.trim().startsWith("import ") || (offset++) == -1)
               .collect(Collectors.joining("\n"));

         final List<ASTNode> nodes = builder.buildFromString(content);

         for (final ASTNode node : nodes) {
            node.visit(this);
         }
      }
   }

   @Override
   public void visitMethodCallExpression(final MethodCallExpression call) {
      LOG.trace("Call: {}", call.getMethodAsString());
      if (call != null && call.getMethodAsString() != null) {
         // System.out.println(call.getMethodAsString());
         if (call.getMethodAsString().equals("dependencies")) {
            // System.out.println(call);
            dependencyLine = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("buildscript")) {
            return; // never change buildscript entries
         } else if (call.getMethodAsString().equals("test")) {
            testLine = call.getLastLineNumber() + offset;
            if (call.getArguments() instanceof ArgumentListExpression) {
               ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
               testSystemPropertiesLine = parseTaskWithPotentialSystemProperties(arguments, testExecutionProperties , testSystemPropertiesBlock);
            }
         } else if (call.getMethodAsString().equals("integrationTest")) {
            testLine = call.getLastLineNumber() + offset;
            if (call.getArguments() instanceof ArgumentListExpression) {
               ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
               integrationTestSystemPropertiesLine = parseTaskWithPotentialSystemProperties(arguments, integrationtestExecutionProperties, integrationTestSystemPropertiesBlock);
            }
         } else if (call.getMethodAsString().equals("android")) {
            androidLine = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("testOptions")) {
            testOptionsAndroid = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("unitTests.all")) {
            unitTestsAll = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("buildToolsVersion")) {
            buildToolsVersion = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("task")) {
            parseNewTask(call);
         } else if (call.getMethodAsString().equals("register")) {
            // System.out.println(call.getClass());
            parseNewTask(call);
         } else if (call.getMethodAsString().equals("exclude")) {
            parseExcludes(call);
         } else if (call.getMethodAsString().equals("configurations")) {
            configurations = call;
         } else if (call.getMethodAsString().equals("all")) {
            if (configurations != null) {
               if (call.getLineNumber() >= configurations.getLineNumber() && call.getLineNumber() <= configurations.getLastLineNumber()) {
                  allConfigurationsLine = call.getLineNumber();
               }
            }
         }
      }

      super.visitMethodCallExpression(call);
   }

   private int parseTaskWithPotentialSystemProperties(ArgumentListExpression arguments, Map<String, Integer> executionProperties, MutableBoolean systemPropertiesBlock) {
      int propertiesLine = -1;
      for (Expression argument : arguments.getExpressions()) {
         if (argument instanceof ClosureExpression) {
            ClosureExpression closure = (ClosureExpression) argument;
            BlockStatement blockStatement = (BlockStatement) closure.getCode();
            for (Statement statement : blockStatement.getStatements()) {
               ExpressionStatement potentialSystemProperties = null;
               if (statement instanceof ReturnStatement) {
                  Expression expression = ((ReturnStatement) statement).getExpression();
                  if (expression.getText().contains("systemProperties") || expression.getText().contains("systemProperty")) {
                     potentialSystemProperties = new ExpressionStatement(((ReturnStatement) statement).getExpression());
                  }
               } else if (statement instanceof ExpressionStatement) {
                  potentialSystemProperties = (ExpressionStatement) statement;
               }
               if (potentialSystemProperties != null && potentialSystemProperties.getExpression() instanceof MethodCallExpression) {
                  propertiesLine = getPropertiesLine(executionProperties, propertiesLine, potentialSystemProperties ,systemPropertiesBlock);
               }
            }
         } else {
            if (argument instanceof MethodCallExpression) {
               MethodCallExpression expression = (MethodCallExpression) argument;
//               System.out.println(expression.getArguments());
               if (expression.getArguments() instanceof ArgumentListExpression) {
                  ArgumentListExpression innerArguments = (ArgumentListExpression) expression.getArguments();
                  return parseTaskWithPotentialSystemProperties(innerArguments, null, null);
               }
            }
         }

      }
      return propertiesLine;
   }

   private int getPropertiesLine(Map<String, Integer> executionProperties, int propertiesLine, ExpressionStatement potentialSystemProperties, MutableBoolean systemPropertiesBlock) {
      MethodCallExpression methodCallExpression = (MethodCallExpression) potentialSystemProperties.getExpression();
      String method = methodCallExpression.getMethodAsString();
      ArgumentListExpression propertiesArguments = (ArgumentListExpression) methodCallExpression.getArguments();
      if (method.equals("systemProperties")) {
         MapExpression map = (MapExpression) propertiesArguments.getExpression(0);
         if (executionProperties != null) {
            systemPropertiesBlock.setTrue();
            for (MapEntryExpression expression : map.getMapEntryExpressions()) {
               String key = expression.getKeyExpression().getText();
               String value = expression.getValueExpression().getText();
               propertiesLine = expression.getLineNumber();
               addExecutionProperties(executionProperties, expression.getLineNumber(), key, value);
            }
         }
      } else if (method.equals("systemProperty")) {
         if (executionProperties != null) {
            systemPropertiesBlock.setFalse();
            propertiesLine = propertiesArguments.getExpression(0).getLineNumber();
            String key = propertiesArguments.getExpression(0).getText();
            String value = propertiesArguments.getExpression(1).getText();
            addExecutionProperties(executionProperties, propertiesLine, key, value);
         }
      }
      return propertiesLine;
   }

   private static void addExecutionProperties(Map<String, Integer> executionProperties, int line, String key, String value) {
      if (key.startsWith(JUPITER_EXECUTION_CONFIG_DEFAULT) && value.contains("concurrent")) {
         executionProperties.put(JUPITER_EXECUTION_CONFIG_DEFAULT, line);
      }
      if (key.startsWith(JUPITER_EXECUTION_CONFIG) && value.contains("true")) {
         executionProperties.put(JUPITER_EXECUTION_CONFIG, line);
      }
   }

   private void parseExcludes(final MethodCallExpression call) {
      TupleExpression tuple = (TupleExpression) call.getArguments();
      Expression expression = tuple.getExpression(0);
      if (expression instanceof NamedArgumentListExpression) {
         NamedArgumentListExpression argumentListExpression = (NamedArgumentListExpression) expression;

         Map<String, String> map = new HashMap<>();
         for (MapEntryExpression innerMapEntryExpression : argumentListExpression.getMapEntryExpressions()) {
            String key = innerMapEntryExpression.getKeyExpression().getText();
            String value = innerMapEntryExpression.getValueExpression().getText();

            map.put(key, value);
         }
         if ("junit".equals(map.get("group")) && "junit".equals(map.get("module"))) {
            excludeLines.add(call.getLineNumber() + offset);
         }
         if ("org.junit.vintage".equals(map.get("group")) && "junit-vintage-engine".equals(map.get("module"))) {
            excludeLines.add(call.getLineNumber() + offset);
         }
      }
   }

   private void parseNewTask(final MethodCallExpression call) {
      if (call.getArguments() instanceof ArgumentListExpression) {
         ArgumentListExpression list = (ArgumentListExpression) call.getArguments();
         Expression first = list.getExpression(0);
         if (first instanceof ConstantExpression) {
            ConstantExpression expression = (ConstantExpression) first;
            if (expression.getValue().equals("integrationTest")) {
               integrationTestLine = call.getLastLineNumber() + offset;
            }
         }
         if (first instanceof MethodCallExpression) {
            MethodCallExpression methodCallExpression = (MethodCallExpression) first;
            Expression method = methodCallExpression.getMethod();
            if (method instanceof ConstantExpression) {
               ConstantExpression expression = (ConstantExpression) method;
               if (expression.getValue().equals("integrationTest")) {
                  integrationTestLine = call.getLastLineNumber() + offset;
               }
            }
         }

         int propertiesLine = parseTaskWithPotentialSystemProperties(list, null, null);
         if (integrationTestSystemPropertiesLine == -1) {
            integrationTestSystemPropertiesLine = propertiesLine;
         }
      }
   }

   @Override
   public void visitMapEntryExpression(final MapEntryExpression expression) {
      final String name = expression.getKeyExpression().getText();
      if (name.equals("buildTools")) {
         buildTools = expression.getLineNumber();
      }
      super.visitMapEntryExpression(expression);
   }

   public int getDependencyLine() {
      return dependencyLine;
   }

   public int getTestLine() {
      return testLine;
   }

   public int getAndroidLine() {
      return androidLine;
   }

   public int getTestOptionsAndroid() {
      return testOptionsAndroid;
   }

   public int getUnitTestsAll() {
      return unitTestsAll;
   }

   public int getIntegrationTestLine() {
      return integrationTestLine;
   }

   public boolean isHasVersion() {
      return hasVersion;
   }

   public boolean isAndroid() {
      return getBuildTools() != -1 || androidLine != -1 || getBuildToolsVersion() != -1;
   }

   public void setHasVersion(final boolean hasVersion) {
      this.hasVersion = hasVersion;
   }

   public boolean hasVersion() {
      return hasVersion;
   }

   public int getBuildTools() {
      return buildTools;
   }

   public int getBuildToolsVersion() {
      return buildToolsVersion;
   }

   public boolean isSubprojectJava() {
      return subprojectJava;
   }

   public List<String> getLines() {
      return gradleFileContents;
   }

   public List<Integer> getExcludeLines() {
      return excludeLines;
   }

   public int getAllConfigurationsLine() {
      return allConfigurationsLine;
   }

   public int getTestSystemPropertiesLine() {
      return testSystemPropertiesLine;
   }

   public int getIntegrationTestSystemPropertiesLine() {
      return integrationTestSystemPropertiesLine;
   }

   public Map<String, Integer> getTestExecutionProperties() {
      return testExecutionProperties;
   }

   public Map<String, Integer> getIntegrationtestExecutionProperties() {
      return integrationtestExecutionProperties;
   }


   public Boolean hasTestSystemPropertiesBlock() {
      return testSystemPropertiesBlock.getValue();
   }

   public Boolean hasIntegrationTestSystemPropertiesBlock() {
      return integrationTestSystemPropertiesBlock.getValue();
   }

   public void addLine(final int lineIndex, final String textForAdding) {
      System.out.println("Adding: " + lineIndex + " " + textForAdding);
      System.out.println("integrationtest: " + integrationTestSystemPropertiesLine);

      gradleFileContents.add(lineIndex, textForAdding + ADDEDBYPEASS);
      if (lineIndex < dependencyLine && dependencyLine != -1) {
         dependencyLine++;
      }
      if (lineIndex < testLine && testLine != -1) {
         testLine++;
      }
      if (lineIndex < testSystemPropertiesLine && testSystemPropertiesLine != -1) {
         testSystemPropertiesLine++;
         for (Map.Entry<String, Integer> entry : testExecutionProperties.entrySet()) {
            entry.setValue(entry.getValue() + 1);
         }
      }

      if (lineIndex < integrationTestLine && integrationTestLine != -1) {
         integrationTestLine++;
      }
      if (lineIndex < integrationTestSystemPropertiesLine && integrationTestSystemPropertiesLine != -1) {
         integrationTestSystemPropertiesLine++;
         for (Map.Entry<String, Integer> entry : integrationtestExecutionProperties.entrySet()) {
            entry.setValue(entry.getValue() + 1);
         }
      }

      if (lineIndex < androidLine && androidLine != -1) {
         androidLine++;
      }
      if (lineIndex < testOptionsAndroid && testOptionsAndroid != -1) {
         testOptionsAndroid++;
      }
      if (lineIndex < unitTestsAll && unitTestsAll != -1) {
         unitTestsAll++;
      }
      if (lineIndex < buildTools && buildTools != -1) {
         buildTools++;
      }
      if (lineIndex < buildToolsVersion && buildToolsVersion != -1) {
         buildToolsVersion++;
      }
      if (lineIndex < allConfigurationsLine && allConfigurationsLine != -1) {
         allConfigurationsLine++;
      }
      List<Integer> newExcludeLines = new LinkedList<>();
      for (Integer excludeLine : excludeLines) {
         if (lineIndex < excludeLine) {
            newExcludeLines.add(excludeLine + 1);
         }
      }
      excludeLines = newExcludeLines;
   }

   public void clearLine(final Integer lineNumber) {
      gradleFileContents.set(lineNumber - 1, "");
   }
}