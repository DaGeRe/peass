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
   private int testJvmArgsLine = -1;
   private String testJvmArgsText;
   private int testSystemPropertiesLine = -1;
   private int integrationTestLine = -1;
   private int integrationTestJvmArgsLine = -1;
   private String integrationTestJvmArgsText;
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
   private MutableBoolean integrationTestSystemPropertiesBlock = new MutableBoolean(false);
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
               TestTaskParser parser = new TestTaskParser(arguments, testExecutionProperties, testSystemPropertiesBlock);
               testSystemPropertiesLine =  parser.getPropertiesLine();
               testJvmArgsLine = parser.getJvmArgsLine();
               testJvmArgsText = parser.getTestJvmArgsText();
            }
         } else if (call.getMethodAsString().equals("integrationTest")) {
            integrationTestLine = call.getLastLineNumber() + offset;
            if (call.getArguments() instanceof ArgumentListExpression) {
               ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
               parseTaskDefinition(arguments, integrationtestExecutionProperties, integrationTestSystemPropertiesBlock);
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
         ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
         Expression first = arguments.getExpression(0);
         if (first instanceof ConstantExpression) {
            ConstantExpression expression = (ConstantExpression) first;
            if (expression.getValue().equals("integrationTest")) {
               integrationTestLine = call.getLastLineNumber() + offset;
               parseTaskDefinition(arguments, null, null);
            }
         }
         if (first instanceof MethodCallExpression) {
            MethodCallExpression methodCallExpression = (MethodCallExpression) first;
            Expression method = methodCallExpression.getMethod();
            if (method instanceof ConstantExpression) {
               ConstantExpression expression = (ConstantExpression) method;
               if (expression.getValue().equals("integrationTest")) {
                  integrationTestLine = call.getLastLineNumber() + offset;
                  
                  parseTaskDefinition(arguments, null, null);
               }
            }
         }
      }
   }

   private void parseTaskDefinition(ArgumentListExpression arguments, Map<String, Integer> executionProperties, MutableBoolean systemPropertiesBlock) {
      TestTaskParser parser = new TestTaskParser(arguments, executionProperties, systemPropertiesBlock);
      if (parser.getPropertiesLine() != -1) {
         integrationTestSystemPropertiesLine =  parser.getPropertiesLine();
      }
      if (parser.getJvmArgsLine() != -1) {
         integrationTestJvmArgsLine = parser.getJvmArgsLine();
         integrationTestJvmArgsText = parser.getTestJvmArgsText();
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

   public int getTestJvmArgsLine() {
      return testJvmArgsLine;
   }

   public String getTestJvmArgsText() {
      return testJvmArgsText;
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
   
   public int getIntegrationTestJvmArgsLine() {
      return integrationTestJvmArgsLine;
   }
   
   public String getIntegrationTestJvmArgsText() {
      return integrationTestJvmArgsText;
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
      if (lineIndex < testJvmArgsLine && testJvmArgsLine != -1) {
         testJvmArgsLine++;
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
      
      if (lineIndex < integrationTestJvmArgsLine && integrationTestJvmArgsLine != -1) {
         integrationTestJvmArgsLine++;
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