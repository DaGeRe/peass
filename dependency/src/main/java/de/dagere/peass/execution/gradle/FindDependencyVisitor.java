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
import org.codehaus.groovy.ast.stmt.Statement;

public class FindDependencyVisitor extends CodeVisitorSupport {

   private static final Logger LOG = LogManager.getLogger(FindDependencyVisitor.class);

   private int offset = 0;
   private int dependencyLine = -1;
   private int testLine = -1;
   private int integrationTestLine = -1;
   private int androidLine = -1;
   private int testOptionsAndroid = -1;
   private int unitTestsAll = -1;
   private int buildTools = -1;
   private int buildToolsVersion = -1;
   private int allConfigurationsLine = -1;
   private MethodCallExpression configurations = null;

   private List<Integer> excludeLines = new LinkedList<>();
   private boolean useJava = false;
   private boolean useSpringBoot = false;
   private boolean hasVersion = true;
   private boolean subprojectJava = false;
   private List<String> gradleFileContents;

   public FindDependencyVisitor(final File buildfile) throws IOException {
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
         if (call.getMethodAsString().equals("plugins")) {
            parsePluginsSection(call);
         } else if (call.getMethodAsString().equals("apply")) {
            final String text = call.getArguments().getText();
            checkPluginName(text);
         } else if (call.getMethodAsString().equals("dependencies")) {
            // System.out.println(call);
            dependencyLine = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("test")) {
            testLine = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("android")) {
            androidLine = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("testOptions")) {
            testOptionsAndroid = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("unitTests.all")) {
            unitTestsAll = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("buildToolsVersion")) {
            buildToolsVersion = call.getLastLineNumber() + offset;
         } else if (call.getMethodAsString().equals("subprojects")) {
            parseSubprojectsSection(call);
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
      }
   }

   private void parseSubprojectsSection(final MethodCallExpression call) {
      Expression expression = call.getArguments();
      if (expression instanceof ArgumentListExpression) {
         ArgumentListExpression list = (ArgumentListExpression) expression;
         for (Expression pluginExpression : list.getExpressions()) {
            ClosureExpression closurePluginExpression = (ClosureExpression) pluginExpression;
            for (Statement statement : ((BlockStatement) closurePluginExpression.getCode()).getStatements()) {
               String text = statement.getText();
               if (isJavaPlugin(text)) {
                  subprojectJava = true;
               }
            }
         }
      }
   }

   private void parsePluginsSection(final MethodCallExpression call) {
      Expression expression = call.getArguments();
      if (expression instanceof ArgumentListExpression) {
         ArgumentListExpression list = (ArgumentListExpression) expression;
         for (Expression pluginExpression : list.getExpressions()) {
            ClosureExpression closurePluginExpression = (ClosureExpression) pluginExpression;
            BlockStatement blockStatement = (BlockStatement) closurePluginExpression.getCode();
            for (Statement statement : blockStatement.getStatements()) {
               String text = statement.getText();
               checkPluginName(text);
            }
         }
      }
   }

   private void checkPluginName(final String text) {
      if (isJavaPlugin(text)) {
         useJava = true;
      } else if (text.contains("org.springframework.boot")) {
         useSpringBoot = true;
      }
   }

   private boolean isJavaPlugin(final String text) {
      if (text.contains("plugin:java") ||
            text.contains("this.id(java)") ||
            text.contains("this.id(java-library)") ||
            text.contains("ConstantExpression[java-library]") ||
            text.contains("plugin:com.android.library") ||
            text.contains("plugin:com.android.application") ||
            text.contains("application") ||
            text.contains("com.android.application")) {
         return true;
      } else {
         return false;
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

   public boolean isUseJava() {
      return useJava;
   }

   public boolean isUseSpringBoot() {
      return useSpringBoot;
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

   public void addLine(final int lineIndex, final String textForAdding) {
      gradleFileContents.add(lineIndex, textForAdding);
      if (lineIndex < dependencyLine && dependencyLine != -1) {
         dependencyLine++;
      }
      if (lineIndex < testLine && testLine != -1) {
         testLine++;
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