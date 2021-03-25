package de.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;

public class FindDependencyVisitor extends CodeVisitorSupport {

   private static final Logger LOG = LogManager.getLogger(FindDependencyVisitor.class);

   private int dependencyLine = -1;
   private int testLine = -1;
   private int androidLine = -1;
   private int testOptionsAndroid = -1;
   private int unitTestsAll = -1;
   private int buildTools = -1;
   private int buildToolsVersion = -1;
   private boolean useJava = false;
   private boolean hasVersion = true;
   private boolean subprojectJava = false;
   private List<String> gradleFileContents;

   public FindDependencyVisitor(final File buildfile) throws IOException {
      gradleFileContents = Files.readAllLines(Paths.get(buildfile.toURI()));
   }

   @Override
   public void visitMethodCallExpression(final MethodCallExpression call) {
      LOG.trace("Call: {}", call.getMethodAsString());
      if (call != null && call.getMethodAsString() != null) {
         // System.out.println(call.getMethodAsString());
         if (call.getMethodAsString().equals("plugins")) {
            Expression expression = call.getArguments();
            if (expression instanceof ArgumentListExpression) {
               ArgumentListExpression list = (ArgumentListExpression) expression;
               for (Expression pluginExpression : list.getExpressions()) {
                  ClosureExpression closurePluginExpression = (ClosureExpression) pluginExpression;
                  for (Statement statement : ((BlockStatement) closurePluginExpression.getCode()).getStatements()) {
                     String text = statement.getText();
                     if (isJavaPlugin(text)) {
                        useJava = true;
                     }
                  }
               }
            }
         } else if (call.getMethodAsString().equals("apply")) {
            final String text = call.getArguments().getText();
            if (isJavaPlugin(text)) {
               useJava = true;
            }
         } else if (call.getMethodAsString().equals("dependencies")) {
            // System.out.println(call);
            dependencyLine = call.getLastLineNumber();
         } else if (call.getMethodAsString().equals("test")) {
            testLine = call.getLastLineNumber();
         } else if (call.getMethodAsString().equals("android")) {
            androidLine = call.getLastLineNumber();
         } else if (call.getMethodAsString().equals("testOptions")) {
            testOptionsAndroid = call.getLastLineNumber();
         } else if (call.getMethodAsString().equals("unitTests.all")) {
            unitTestsAll = call.getLastLineNumber();
         } else if (call.getMethodAsString().equals("buildToolsVersion")) {
            buildToolsVersion = call.getLastLineNumber();
         } else if (call.getMethodAsString().equals("subprojects")) {
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
      }

      // LOG.info("Android: " + androidLine);
      super.visitMethodCallExpression(call);
   }

   private boolean isJavaPlugin(final String text) {
      if (text.contains("plugin:java") ||
            text.contains("plugin:com.android.library") ||
            text.contains("plugin:com.android.application") ||
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

   public boolean isUseJava() {
      return useJava;
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

   public void addLine(final int lineIndex, final String textForAdding) {
      gradleFileContents.add(lineIndex, textForAdding);
      if (lineIndex < dependencyLine) {
         dependencyLine++;
      }
      if (lineIndex < testLine) {
         testLine++;
      }
      if (lineIndex < androidLine) {
         androidLine++;
      }
      if (lineIndex < testOptionsAndroid) {
         testOptionsAndroid++;
      }
      if (lineIndex < unitTestsAll) {
         unitTestsAll++;
      }
      if (lineIndex < buildTools) {
         buildTools++;
      }
      if (lineIndex < buildToolsVersion) {
         buildToolsVersion++;
      }
   }

}