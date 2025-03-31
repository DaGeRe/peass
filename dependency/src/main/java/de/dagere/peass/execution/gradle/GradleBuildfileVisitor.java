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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
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
   private TestTaskParser testTaskProperties = null;
   private TestTaskParser integrationTestTaskProperties = null;
   private int integrationTestLine = -1;

   private int junitLine = -1;
   private int junitPlatformLine = -1;
   
   private int androidLine = -1;
   private int compileOptionsLine = -1;
   private int sourceCompatibilityLine = -1;
   private int targetCompatibilityLine = -1;
   private int testOptionsAndroid = -1;
   private int unitTestsAll = -1;
   private int buildTools = -1;
   private int buildToolsVersion = -1;
   private int compileSdkVersion = -1;
   private int defaultConfigLine = -1;
   private int minSdkVersion = -1;
   private int targetSdkVersion = -1;
   private int multiDexEnabled = -1;
   private int gradleVersionLine = -1;
   private int androidPackagingOptions = -1;
   private int allConfigurationsLine = -1;
   private MethodCallExpression configurations = null;

   private List<Integer> excludeLines = new LinkedList<>();
   private boolean hasVersion = true;
   private boolean subprojectJava = false;
   private List<String> gradleFileContents;

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
      LOG.debug("Call: {}", call.getMethodAsString());
      if (call != null && call.getMethodAsString() != null) {
         // System.out.println(call.getMethodAsString());
         if (call.getMethodAsString().equals("dependencies")) {
            // System.out.println(call);
            dependencyLine = call.getLastLineNumber() + offset;
            parseDependencies(call);
         } else if (call.getMethodAsString().equals("buildscript")) {
            // continue parsing
         } else if (call.getMethodAsString().equals("test")) {
            testLine = call.getLastLineNumber() + offset;
            if (call.getArguments() instanceof ArgumentListExpression) {
               ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
               testTaskProperties = new TestTaskParser(arguments);
            }
         } else if (call.getMethodAsString().equals("integrationTest")) {
            integrationTestLine = call.getLastLineNumber() + offset;
            if (call.getArguments() instanceof ArgumentListExpression) {
               ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
               parseTaskDefinition(arguments);
            }
         } else if (call.getMethodAsString().equals("android")) {
            androidLine = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("compileOptions")) {
            compileOptionsLine = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("sourceCompatibility")) {
            sourceCompatibilityLine = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("targetCompatibility")) {
            targetCompatibilityLine = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("testOptions")) {
            testOptionsAndroid = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("unitTests.all")) {
            unitTestsAll = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("classpath")) {
            if (isGradleVersionLine(call)) {
               gradleVersionLine = call.getLastLineNumber() + offset;
            }
         } else if (call.getMethodAsString().equals("buildToolsVersion")) {
            buildToolsVersion = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("compileSdkVersion")) {
            compileSdkVersion = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("defaultConfig")) {
            defaultConfigLine = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("minSdkVersion")) {
            minSdkVersion = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("targetSdkVersion")) {
            targetSdkVersion = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("multiDexEnabled")) {
            multiDexEnabled = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("android.packagingOptions")) {
            androidPackagingOptions = call.getLastLineNumber() + offset;
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

   private boolean isGradleVersionLine(MethodCallExpression call) {
      Expression expression = call.getArguments();

      if (expression instanceof ArgumentListExpression) {
         ArgumentListExpression argumentList = (ArgumentListExpression) expression;

         boolean isGradleVersionNode = false;

         for (Expression e : argumentList.getExpressions()) {
            isGradleVersionNode |= e.getText().startsWith("com.android.tools.build:gradle");
         }

         return isGradleVersionNode;
      }

      return false;
   }

   private void parseDependencies(final MethodCallExpression call) {
      TupleExpression tuple = (TupleExpression) call.getArguments();
      Expression expression = tuple.getExpression(0);
      if (expression instanceof ClosureExpression) {
         ClosureExpression argumentListExpression = (ClosureExpression) expression;
         Statement code = argumentListExpression.getCode();
         if (code instanceof BlockStatement) {
            BlockStatement block = (BlockStatement) code;
            for (Statement statement : block.getStatements()) {
               if (statement instanceof ReturnStatement) {
                  ReturnStatement returnStatement = (ReturnStatement) statement;
                  if (returnStatement.getText().contains("org.junit.jupiter:junit-jupiter:")) {
                     junitLine = returnStatement.getLineNumber();
                  }
               } else if (statement instanceof ExpressionStatement) {
                  ExpressionStatement expressionStatement = (ExpressionStatement) statement;
                  if (expressionStatement.getText().contains("org.junit.jupiter:junit-jupiter:")) {
                     junitLine = expressionStatement.getLineNumber();
                  }
               }
            }
         }
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
         ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
         Expression first = arguments.getExpression(0);
         if (first instanceof ConstantExpression) {
            ConstantExpression expression = (ConstantExpression) first;
            if (expression.getValue().equals("integrationTest")) {
               integrationTestLine = call.getLastLineNumber() + offset;
               parseTaskDefinition(arguments);
            }
         }
         if (first instanceof MethodCallExpression) {
            MethodCallExpression methodCallExpression = (MethodCallExpression) first;
            Expression method = methodCallExpression.getMethod();
            if (method instanceof ConstantExpression) {
               ConstantExpression expression = (ConstantExpression) method;
               if (expression.getValue().equals("integrationTest")) {
                  integrationTestLine = call.getLastLineNumber() + offset;

                  parseTaskDefinition(arguments);
               }
            }
         }
      }
   }

   private void parseTaskDefinition(ArgumentListExpression arguments) {
      TestTaskParser parser = new TestTaskParser(arguments);
      integrationTestTaskProperties = parser;
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

   public TestTaskParser getTestTaskProperties() {
      return testTaskProperties;
   }
   
   public int getJunitPlatformLine() {
      return junitPlatformLine;
   }

   public int getJunitLine() {
      return junitLine;
   }

   public int getAndroidLine() {
      return androidLine;
   }

   public int getCompileOptionsLine() {
      return compileOptionsLine;
   }

   public void setCompileOptionsLine(int compileOptionsLine) {
      this.compileOptionsLine = compileOptionsLine;
   }

   public int getSourceCompatibilityLine() {
      return sourceCompatibilityLine;
   }

   public int getTargetCompatibilityLine() {
      return targetCompatibilityLine;
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

   public TestTaskParser getIntegrationTestTaskProperties() {
      return integrationTestTaskProperties;
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

   public int getCompileSdkVersion() {
      return compileSdkVersion;
   }

   public int getDefaultConfigLine() {
      return defaultConfigLine;
   }

   public void setDefaultConfigLine(int defaultConfigEnd) {
      defaultConfigLine = defaultConfigEnd;
   }

   public int getMinSdkVersion() {
      return minSdkVersion;
   }

   public int getTargetSdkVersion() {
      return targetSdkVersion;
   }

   public int getMultiDexEnabled() {
      return multiDexEnabled;
   }

   public int getGradleVersionLine() {
      return gradleVersionLine;
   }

   public int getAndroidPackagingOptions() {
      return androidPackagingOptions;
   }

   public void setAndroidPackagingOptions(int androidPackagingOptionsEnd) {
      androidPackagingOptions = androidPackagingOptionsEnd;
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

   public void addLine(final int lineIndex, final String textForAdding) {
      gradleFileContents.add(lineIndex, textForAdding + ADDEDBYPEASS);
      if (lineIndex < dependencyLine && dependencyLine != -1) {
         dependencyLine++;
      }

      if (lineIndex < testLine && testLine != -1) {
         testLine++;
      }
      if (testTaskProperties != null) {
         testTaskProperties.increaseLines(lineIndex);
      }

      if (lineIndex < integrationTestLine && integrationTestLine != -1) {
         integrationTestLine++;
      }
      if (integrationTestTaskProperties != null) {
         integrationTestTaskProperties.increaseLines(lineIndex);
      }

      if (lineIndex < androidLine && androidLine != -1) {
         androidLine++;
      }
      if (lineIndex < compileOptionsLine && compileOptionsLine != -1) {
         compileOptionsLine++;
      }
      if (lineIndex < sourceCompatibilityLine && sourceCompatibilityLine != -1) {
         sourceCompatibilityLine++;
      }
      if (lineIndex < targetCompatibilityLine && targetCompatibilityLine != -1) {
         targetCompatibilityLine++;
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
      if (lineIndex < compileSdkVersion && compileSdkVersion != -1) {
         compileSdkVersion++;
      }
      if (lineIndex < defaultConfigLine && defaultConfigLine != -1) {
         defaultConfigLine++;
      }
      if (lineIndex < minSdkVersion && minSdkVersion != -1) {
         minSdkVersion++;
      }
      if (lineIndex < targetSdkVersion && targetSdkVersion != -1) {
         targetSdkVersion++;
      }
      if (lineIndex < multiDexEnabled && multiDexEnabled != -1) {
         multiDexEnabled++;
      }
      if (lineIndex < androidPackagingOptions && androidPackagingOptions != -1) {
         androidPackagingOptions++;
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