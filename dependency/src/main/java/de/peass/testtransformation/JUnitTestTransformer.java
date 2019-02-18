/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peass.testtransformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.TestRule;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.peass.dependency.ClazzFinder;
import de.peass.dependency.analysis.data.ChangedEntity;

/**
 * Transforms JUnit-Tests to performance tests.
 * 
 * @author reichelt
 *
 */
public class JUnitTestTransformer {

   private static final int DEFAULT_EXECUTIONS = 10;
   public static final int TIMEOUT_SECONDS = 300; // 5 Minutes should be fine
   private static final int DEFAULT_TIMEOUT = 1 * TIMEOUT_SECONDS * 1000;

   private static final Logger LOG = LogManager.getLogger(JUnitTestTransformer.class);

   protected DataCollectorList datacollectorlist;
   protected int warmupExecutions, iterations;
   protected long timeoutTime;
   protected boolean logFullData = true;
   protected File projectFolder;
   protected boolean useKieker = false;
   protected Charset charset = StandardCharsets.UTF_8;
   protected int repetitions = 1;

   /**
    * Initializes TestTransformer with folder.
    * 
    * @param projectFolder Folder, where tests should be transformed
    */
   public JUnitTestTransformer(final File projectFolder) {
      this.projectFolder = projectFolder;
      datacollectorlist = DataCollectorList.STANDARD;
      iterations = DEFAULT_EXECUTIONS;
      warmupExecutions = DEFAULT_EXECUTIONS;
      timeoutTime = DEFAULT_TIMEOUT;
   }

   public boolean isUseKieker() {
      return useKieker;
   }

   public void setUseKieker(final boolean useKieker) {
      this.useKieker = useKieker;
   }

   public int getRepetitions() {
      return repetitions;
   }

   public void setRepetitions(final int repetitions) {
      this.repetitions = repetitions;
   }

   private Map<File, CompilationUnit> loadedFiles;
   private Map<File, Integer> junitVersions;

   public void determineVersions(final List<File> modules) {
      loadedFiles = new HashMap<>();
      junitVersions = new HashMap<>();

      for (final File module : modules) {
         final File testFolder = new File(module, "src/test/");
         if (testFolder.exists()) {
            determineVersions(testFolder);
         } else {
            LOG.error("Test folder " + testFolder.getAbsolutePath() + " does not exist.");
         }
      }
   }

   /**
    * Generates Performance-Test, i.e. transforms the current unit-tests to performance tests by adding annotations to the Java-files.
    * 
    * @throws FileNotFoundException
    */
   public void transformTests() {
      if (!projectFolder.exists()) {
         LOG.error("Path " + projectFolder + " not found");
      }
      LOG.trace("Searching: {}", projectFolder);

      LOG.debug("JUnit Versions Determined");
      for (final Map.Entry<File, Integer> fileVersionEntry : junitVersions.entrySet()) {
         if (fileVersionEntry.getValue() == 3) {
            editJUnit3(fileVersionEntry.getKey());
         } else if (fileVersionEntry.getValue() == 4 || fileVersionEntry.getValue() == 34 || fileVersionEntry.getValue() == 5) {
            editJUnit4(fileVersionEntry.getKey());
         }
      }
   }

   public int getVersion(final File clazzFile) {
      return junitVersions.get(clazzFile);
   }

   private void determineVersions(final File testFolder) {
      final Map<String, List<File>> extensions = new HashMap<>();
      for (final File javaFile : FileUtils.listFiles(testFolder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE)) {
         try {
            final CompilationUnit unit = JavaParser.parse(javaFile);
            loadedFiles.put(javaFile, unit);
            final boolean isJUnit4 = isJUnit(unit, 4);
            if (isJUnit4) {
               junitVersions.put(javaFile, 4);
               // editJUnit4(javaFile);
            }
            final boolean isJUnit5 = isJUnit(unit, 5);
            if (isJUnit5) {
               junitVersions.put(javaFile, 5);
               // editJUnit4(javaFile);
            }
            final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);
            if (clazz != null) { // for @interface cases
               // We only need to consider classes with one extends, since classes can not have multiple extends and we search for classes that may extend TestCase (indirectly)
               LOG.trace("Transforming: {}", clazz.getNameAsString());
               if (clazz.getExtendedTypes().size() == 1) {
                  final ClassOrInterfaceType extend = clazz.getExtendedTypes(0);
                  final String extensionName = extend.getNameAsString().intern();
                  List<File> extensionsOfBase = extensions.get(extensionName);
                  if (extensionsOfBase == null) {
                     extensionsOfBase = new LinkedList<>();
                     extensions.put(extensionName, extensionsOfBase);
                  }
                  extensionsOfBase.add(javaFile);
               }
            }
         } catch (final FileNotFoundException e) {
            e.printStackTrace();
         }
      }

      addJUnit3Test("TestCase", extensions, junitVersions);
   }

   Map<Integer, String> junitTestAnnotations = new HashMap<>();
   {
      junitTestAnnotations.put(5, "org.junit.jupiter.api.Test");
      junitTestAnnotations.put(4, "org.junit.Test");
   }

   private boolean isJUnit(final CompilationUnit unit, final int version) {
      final String importNameVersion = junitTestAnnotations.get(version);
      boolean isJUnitVersion = false;
      for (final ImportDeclaration currentImport : unit.getImports()) {
         final Name importName = currentImport.getName();
         if (importName.toString().equals(importNameVersion)) {
            isJUnitVersion = true;
         }
      }
      return isJUnitVersion;
   }

   public void addJUnit3Test(final String clazzName, final Map<String, List<File>> extensions, final Map<File, Integer> junitVersions) {
      final List<File> extending = extensions.get(clazzName);
      if (extending != null) {
         for (final File foundTest : extending) {
            if (junitVersions.get(foundTest) != null && junitVersions.get(foundTest) == 4) {
               // 34 means mixed-junit-3-4
               // -> A test may include @Test-tests, but still extend some JUnit 3 test, and therefore the extension hierarchy is still relevant for him
               junitVersions.put(foundTest, 34);
            } else {
               junitVersions.put(foundTest, 3);
            }
            addJUnit3Test(foundTest.getName().replaceAll(".java", ""), extensions, junitVersions);
         }
      }
   }

   public List<String> getTests(final File module, final ChangedEntity clazzname) {
      final List<String> methods = new LinkedList<>();
      final File clazzFile = ClazzFinder.getClazzFile(module, clazzname);
      final CompilationUnit unit = loadedFiles.get(clazzFile);
      if (unit != null) {
         final Integer junit = junitVersions.get(clazzFile);
         if (junit != null) {
            final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);
            if (junit == 3) {
               for (final MethodDeclaration method : clazz.getMethods()) {
                  if (method.getNameAsString().toLowerCase().contains("test")) {
                     methods.add(method.getNameAsString());
                  }
               }
            } else if (junit == 4) {
               getAnnotatedMethods(methods, clazz, 4);
            } else if (junit == 5) {
               getAnnotatedMethods(methods, clazz, 5);
            }
         } else {
            LOG.error("Clazz {} has no JUnit version", clazzFile);
         }
      } else {
         LOG.error("Did not find {} for {}", clazzFile, clazzname);
      }

      return methods;
   }

   void getAnnotatedMethods(final List<String> methods, final ClassOrInterfaceDeclaration clazz, final int version) {
      final String importNameVersion = junitTestAnnotations.get(version);
      for (final MethodDeclaration method : clazz.getMethods()) {
         boolean found = false;
         for (final AnnotationExpr annotation : method.getAnnotations()) {
            final String currentName = annotation.getNameAsString();
            if (currentName.equals(importNameVersion) || currentName.equals("Test")) {
               found = true;
            }
         }
         if (found) {
            methods.add(method.getNameAsString());
         }
      }
   }

   /**
    * Edits Java so that the class extends KoPeMeTestcase instead of TestCase and that the methods for specifying the performance test are added. It is assumed that every class is
    * in it's original state, i.e. no KoPeMeTestcase-changes have been made yet. Classes, that already extend KoPeMeTestcase are not changed.
    * 
    * @param javaFile File for editing
    */
   protected void editJUnit3(final File javaFile) {
      try {
         final CompilationUnit unit = loadedFiles.get(javaFile);
         unit.addImport("de.dagere.kopeme.junit3.KoPeMeTestcase");
         unit.addImport("de.dagere.kopeme.datacollection.DataCollectorList");

         final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);

         if (!clazz.getExtendedTypes(0).getNameAsString().equals("KoPeMeTestcase")) {
            if (clazz.getExtendedTypes(0).getNameAsString().equals("TestCase")) {
               clazz.setExtendedTypes(new NodeList<>());
               clazz.addExtendedType("KoPeMeTestcase");
            }

            addMethod(clazz, "getWarmupExecutions", "return " + warmupExecutions + ";", PrimitiveType.intType());
            addMethod(clazz, "getExecutionTimes", "return " + iterations + ";", PrimitiveType.intType());
            addMethod(clazz, "logFullData", "return " + logFullData + ";", PrimitiveType.booleanType());
            addMethod(clazz, "useKieker", "return " + useKieker + ";", PrimitiveType.booleanType());
            addMethod(clazz, "getMaximalTime", "return " + timeoutTime + ";", PrimitiveType.longType());
            addMethod(clazz, "getRepetitions", "return " + repetitions + ";", PrimitiveType.intType());

            if (datacollectorlist.equals(DataCollectorList.ONLYTIME)) {
               addMethod(clazz, "getDataCollectors", "return DataCollectorList.ONLYTIME;", JavaParser.parseClassOrInterfaceType("DataCollectorList"));
            }

            Files.write(javaFile.toPath(), unit.toString().getBytes(charset));
         }
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Adds the given method to the Classdeclaration
    * 
    * @param clazz Clazz where method should be added
    * @param name Name of the new method
    * @param source Source of the new method
    * @param type Returntype of the new method
    */
   protected void addMethod(final ClassOrInterfaceDeclaration clazz, final String name, final String source, final Type type) {
      final List<MethodDeclaration> oldMethods = clazz.getMethodsByName(name);
      if (oldMethods.size() == 0) {
         final MethodDeclaration addedMethod = clazz.addMethod(name, Modifier.publicModifier().getKeyword());
         addedMethod.setType(type);

         final BlockStmt statement = new BlockStmt();
         statement.addStatement(source);

         addedMethod.setBody(statement);
      }
   }

   public void generateClazz(final File module, final String name, final ChangedEntity extension, final String method) {
      final File clazzFile = ClazzFinder.getClazzFile(module, extension);

      final File generatedClass = new File(module, "src/test/java/de/peass/generated/" + name + ".java");
      generatedClass.getParentFile().mkdirs();
      final CompilationUnit cu = new CompilationUnit();
      cu.setPackageDeclaration("de.peass.generated");

      final CompilationUnit unit = loadedFiles.get(clazzFile);
      cu.getImports().addAll(unit.getImports());

      final ClassOrInterfaceDeclaration type = cu.addClass(name);
      type.getExtendedTypes().add(new ClassOrInterfaceType(extension.getJavaClazzName()));

      final NodeList<Modifier> modifiers = new NodeList<>(Modifier.publicModifier());
      final MethodDeclaration methodDeclaration = new MethodDeclaration(modifiers, new VoidType(), method);
      methodDeclaration.setModifiers(modifiers);
      methodDeclaration.getThrownExceptions().add(new ClassOrInterfaceType("java.lang.Throwable"));
      type.addMember(methodDeclaration);

      final BlockStmt block = new BlockStmt();
      block.addStatement("super." + method + "();");
      methodDeclaration.setBody(block);

      final NodeList<ReferenceType> thrownExceptions = untestifyJUnit4(clazzFile, method);
      methodDeclaration.setThrownExceptions(thrownExceptions);

      final int version = getVersion(clazzFile);
      if (version == 4) {
         final NormalAnnotationExpr performanceTestAnnotation = new NormalAnnotationExpr();
         performanceTestAnnotation.setName("org.junit.Test");
         methodDeclaration.addAnnotation(performanceTestAnnotation);
         addAnnotation(methodDeclaration);
      }

      try {
         FileUtils.writeStringToFile(generatedClass, cu.toString(), Charset.defaultCharset());
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public NodeList<ReferenceType> untestifyJUnit4(final File clazzFile, final String methodName) {
      NodeList<ReferenceType> throwDeclarations = null;
      try {
         final CompilationUnit unit = loadedFiles.get(clazzFile);

         final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);

         for (final MethodDeclaration method : clazz.getMethods()) {
            AnnotationExpr removeAnnotation = null;
            for (final AnnotationExpr annotation : method.getAnnotations()) {
               final String currentName = annotation.getNameAsString();
               if (currentName.equals("org.junit.Test") || currentName.equals("Test")) {
                  removeAnnotation = annotation;
               }
            }
            if (method.getNameAsString().equals(methodName)) {
               throwDeclarations = method.getThrownExceptions();
            }
            method.getAnnotations().remove(removeAnnotation);
         }

         Files.write(clazzFile.toPath(), unit.toString().getBytes(charset));
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return throwDeclarations;
   }

   /**
    * Edits Java so that the class is run with the KoPeMe-Testrunner and the methods are annotated additionally with @PerformanceTest.
    * 
    * @param javaFile File for editing
    */
   protected void editJUnit4(final File javaFile) {
      try {
         final CompilationUnit unit = loadedFiles.get(javaFile);

         unit.addImport("de.dagere.kopeme.annotations.Assertion");
         unit.addImport("de.dagere.kopeme.annotations.MaximalRelativeStandardDeviation");
         unit.addImport("org.junit.rules.TestRule");
         unit.addImport("org.junit.Rule");
         unit.addImport("de.dagere.kopeme.junit.rule.KoPeMeRule");

         final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);

         final boolean fieldFound = hasKoPeMeRule(clazz) || hasKoPeMeRunner(clazz);
         if (!fieldFound) {
            addRule(clazz);
         }

         for (final MethodDeclaration method : clazz.getMethods()) {
            boolean performanceTestFound = false;
            boolean testFound = false;
            for (final AnnotationExpr annotation : method.getAnnotations()) {
               final String currentName = annotation.getNameAsString();
               if (currentName.equals("de.dagere.kopeme.annotations.PerformanceTest") || currentName.equals("PerformanceTest")) {
                  performanceTestFound = true;
               }
               if (currentName.equals("org.junit.Test") || currentName.equals("org.junit.jupiter.api.Test") || currentName.equals("Test")) {
                  testFound = true;
               }
            }
            if (testFound && !performanceTestFound) {
               if (!method.isPublic()) {
                  method.setPublic(true);
               }
               addAnnotation(method);
            }
         }

         Files.write(javaFile.toPath(), unit.toString().getBytes(charset));
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public void addAnnotation(final MethodDeclaration method) {
      final NormalAnnotationExpr performanceTestAnnotation = new NormalAnnotationExpr();
      performanceTestAnnotation.setName("de.dagere.kopeme.annotations.PerformanceTest");
      performanceTestAnnotation.addPair("executionTimes", "" + iterations);
      performanceTestAnnotation.addPair("warmupExecutions", "" + warmupExecutions);
      performanceTestAnnotation.addPair("logFullData", "" + true);
      performanceTestAnnotation.addPair("useKieker", "" + useKieker);
      performanceTestAnnotation.addPair("timeout", "" + timeoutTime);
      performanceTestAnnotation.addPair("repetitions", "" + repetitions);
      if (datacollectorlist.equals(DataCollectorList.ONLYTIME)) {
         performanceTestAnnotation.addPair("dataCollectors", "\"ONLYTIME\"");
      }
      method.addAnnotation(performanceTestAnnotation);
   }

   private boolean hasKoPeMeRule(final ClassOrInterfaceDeclaration clazz) {
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

   private boolean hasKoPeMeRunner(final ClassOrInterfaceDeclaration clazz) {
      boolean kopemeTestrunner = false;
      if (clazz.getAnnotations().size() > 0) {
         for (final AnnotationExpr annotation : clazz.getAnnotations()) {
            if (annotation.getNameAsString().contains("RunWith")) {
               if (annotation instanceof SingleMemberAnnotationExpr) {
                  final SingleMemberAnnotationExpr singleMember = (SingleMemberAnnotationExpr) annotation;
                  final Expression expr = singleMember.getMemberValue();
                  if (expr.toString().equals("PerformanceTestRunnerJUnit.class")) {
                     kopemeTestrunner = true;
                  }
               }
            }
         }
      }
      return kopemeTestrunner;
   }

   private void addRule(final ClassOrInterfaceDeclaration clazz) {
      final NodeList<Expression> arguments = new NodeList<>();
      arguments.add(new ThisExpr());
      final Expression initializer = new ObjectCreationExpr(null, new ClassOrInterfaceType("KoPeMeRule"), arguments);
      final FieldDeclaration fieldDeclaration = clazz.addFieldWithInitializer(TestRule.class, "kopemeRule", initializer, Modifier.publicModifier().getKeyword());
      final NormalAnnotationExpr annotation = new NormalAnnotationExpr();
      annotation.setName("Rule");
      fieldDeclaration.getAnnotations().add(annotation);
   }

   public boolean isLogFullData() {
      return logFullData;
   }

   public void setLogFullData(final boolean logFullData) {
      this.logFullData = logFullData;
   }

   public void setDatacollectorlist(final DataCollectorList datacollectorlist) {
      this.datacollectorlist = datacollectorlist;
   }

   public void setIterations(final int iterations) {
      this.iterations = iterations;
   }

   public int getIterations() {
      return iterations;
   }

   public void setWarmupExecutions(final int warmup) {
      this.warmupExecutions = warmup;
   }

   public int getWarmupExecutions() {
      return warmupExecutions;
   }

   /**
    * Timeout in milliseconds
    * 
    * @return
    */
   public long getSumTime() {
      return timeoutTime;
   }

   /**
    * Sets timeout in milliseconds.
    * 
    * @param sumTime
    */
   public void setSumTime(final long sumTime) {
      this.timeoutTime = sumTime;
   }

   public File getProjectFolder() {
      return projectFolder;
   }

   public void setEncoding(final Charset encoding) {
      charset = encoding;
   }

   public boolean isJUnit3() {
      boolean junit3 = false;
      for (final Entry<File, Integer> clazz : junitVersions.entrySet()) {
         if (clazz.getValue() == 3) {
            junit3 = true;
         }
      }
      return junit3;
   }

}
