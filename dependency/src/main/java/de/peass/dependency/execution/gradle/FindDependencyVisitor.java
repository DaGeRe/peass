package de.peass.dependency.execution.gradle;

import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;

public class FindDependencyVisitor extends CodeVisitorSupport {

   int dependencyLine = -1;
   int testLine = -1;
   int androidLine = -1;
   int testOptionsAndroid = -1;
   int unitTestsAll = -1;
   private int buildTools = -1;
   private int buildToolsVersion = -1;
   boolean useJava = false;
   boolean hasVersion = true;

   @Override
   public void visitMethodCallExpression(final MethodCallExpression call) {
      // LOG.info("Call: " + call.getMethodAsString());
      if (call != null && call.getMethodAsString() != null) {
         // System.out.println(call.getMethodAsString());
         if (call.getMethodAsString().equals("apply")) {
            final String text = call.getArguments().getText();
            if (text.contains("plugin:java") || text.contains("plugin:com.android.library") || text.contains("plugin:com.android.application")) {
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
         }
      }

      // LOG.info("Android: " + androidLine);
      super.visitMethodCallExpression(call);
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


}