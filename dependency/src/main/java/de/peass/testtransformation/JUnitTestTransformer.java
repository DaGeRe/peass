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
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.dagere.kopeme.parsing.JUnitParseUtil;
import de.peass.dependency.ClazzFileFinder;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.JavaParserProvider;
import de.peass.dependency.execution.MeasurementConfiguration;

/**
 * Transforms JUnit-Tests to performance tests.
 * 
 * @author reichelt
 *
 */
public class JUnitTestTransformer {

   private static final Logger LOG = LogManager.getLogger(JUnitTestTransformer.class);

   protected DataCollectorList datacollectorlist;
   protected final MeasurementConfiguration config;
   protected File projectFolder;
   protected boolean aggregatedWriter = false;
   protected boolean ignoreEOIs = false;
   protected Charset charset = StandardCharsets.UTF_8;
   private Map<String, List<File>> extensions = null;

   private final JavaParser javaParser = new JavaParser();

   /**
    * Initializes TestTransformer with folder.
    * 
    * @param projectFolder Folder, where tests should be transformed
    */
   public JUnitTestTransformer(final File projectFolder, final MeasurementConfiguration config) {
      LOG.debug("Test transformer for {} created", projectFolder);
      this.projectFolder = projectFolder;
      this.config = config;
      datacollectorlist = config.isUseGC() ? DataCollectorList.ONLYTIME : DataCollectorList.ONLYTIME_NOGC;
   }

   /**
    * Creates a test transformer for usage in PRONTO
    * 
    * @param projectFolder
    * @param timeout
    */
   public JUnitTestTransformer(final File projectFolder, final long timeout) {
      this.projectFolder = projectFolder;
      config = new MeasurementConfiguration(1, timeout);
      config.setIterations(1);
      config.setWarmup(0);
      config.setUseKieker(true);
      datacollectorlist = DataCollectorList.ONLYTIME;
   }

   private Map<File, CompilationUnit> loadedFiles;

   public Map<File, CompilationUnit> getLoadedFiles() {
      return loadedFiles;
   }

   private Map<File, Integer> junitVersions;

   public void determineVersions(final List<File> modules) {
      determineVersionsForPaths(modules, "src/test/", "src/androidTest");
   }

   public void determineVersionsForPaths(final List<File> modules, final String... testPaths) {
      loadedFiles = new HashMap<>();
      junitVersions = new HashMap<>();

      for (final File module : modules) {
         for (String testPath : testPaths) {
            final File testFolder = new File(module, testPath);
            if (testFolder.exists()) {
               determineVersions(testFolder);
            } else {
               LOG.error("Test folder " + testFolder.getAbsolutePath() + " does not exist.");
            }
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
         } else if (fileVersionEntry.getValue() == 4 || fileVersionEntry.getValue() == 34) {
            editJUnit4(fileVersionEntry.getKey());
         } else if (fileVersionEntry.getValue() == 5) {
            editJUnit5(fileVersionEntry.getKey());
         }
      }
   }

   /**
    * Returns the version of the JUnit test. If the file is no JUnit test, 0 is returned.
    * 
    * @param clazzFile
    * @return
    */
   public int getVersion(final File clazzFile) {
      LOG.debug("Loading: {} {}", clazzFile, junitVersions);
      if (junitVersions.containsKey(clazzFile)) {
         return junitVersions.get(clazzFile);
      } else {
         return 0;
      }

   }

   private void determineVersions(final File testFolder) {
      extensions = new HashMap<>();
      for (final File javaFile : FileUtils.listFiles(testFolder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE)) {
         try {
            final CompilationUnit unit = JavaParserProvider.parse(javaFile);
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

      addJUnit3Test("TestCase", junitVersions);
   }

   private final Map<Integer, String> junitTestAnnotations = new HashMap<>();
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

   public void addJUnit3Test(final String clazzName, final Map<File, Integer> junitVersions) {
      final List<File> extending = extensions.get(clazzName);
      if (extending != null) {
         for (final File foundTest : extending) {
            final Integer testVersion = junitVersions.get(foundTest);
            if (testVersion != null && testVersion == 4) {
               // 34 means mixed-junit-3-4
               // -> A test may include @Test-tests, but still extend some JUnit 3 test, and therefore the extension hierarchy is still relevant for him
               junitVersions.put(foundTest, 34);
            } else if (testVersion != null && testVersion == 5) {
               junitVersions.put(foundTest, 5);
            } else {
               junitVersions.put(foundTest, 3);
            }
            addJUnit3Test(foundTest.getName().replaceAll(".java", ""), junitVersions);
         }
      }
   }

   public Map<String, List<File>> getExtensions() {
      return extensions;
   }

   public List<String> getTests(final File module, final ChangedEntity clazzname) {
      final List<String> methods = new LinkedList<>();
      final File clazzFile = ClazzFileFinder.getClazzFile(module, clazzname);
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
               methods.addAll(getAnnotatedMethods(clazz, 4));
            } else if (junit == 5) {
               methods.addAll(getAnnotatedMethods(clazz, 5));
            }
         } else {
            LOG.error("Clazz {} has no JUnit version", clazzFile);
         }
      } else {
         LOG.error("Did not find {} for {}", clazzFile, clazzname);
      }

      return methods;
   }

   private List<String> getAnnotatedMethods(final ClassOrInterfaceDeclaration clazz, final int version) {
      final String importNameVersion = junitTestAnnotations.get(version);
      final List<String> methods = JUnitParseUtil.getAnnotatedMethods(clazz, importNameVersion, "Test");
      return methods;
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
         editJUnit3(unit);
         Files.write(javaFile.toPath(), unit.toString().getBytes(charset));
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   void editJUnit3(final CompilationUnit unit) {
      unit.addImport("de.dagere.kopeme.junit3.KoPeMeTestcase");
      unit.addImport("de.dagere.kopeme.datacollection.DataCollectorList");

      final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);

      if (!clazz.getExtendedTypes(0).getNameAsString().equals("KoPeMeTestcase")) {
         if (clazz.getExtendedTypes(0).getNameAsString().equals("TestCase")) {
            clazz.setExtendedTypes(new NodeList<>());
            clazz.addExtendedType("KoPeMeTestcase");
         }

         addMethod(clazz, "getWarmup", "return " + config.getWarmup() + ";", PrimitiveType.intType());
         addMethod(clazz, "getIterations", "return " + config.getIterations() + ";", PrimitiveType.intType());
         addMethod(clazz, "logFullData", "return " + config.isLogFullData() + ";", PrimitiveType.booleanType());
         addMethod(clazz, "useKieker", "return " + config.isUseKieker() + ";", PrimitiveType.booleanType());
         addMethod(clazz, "getMaximalTime", "return " + config.getTimeout() + ";", PrimitiveType.longType());
         addMethod(clazz, "getRepetitions", "return " + config.getRepetitions() + ";", PrimitiveType.intType());
         addMethod(clazz, "redirectToNull", "return " + config.isRedirectToNull() + ";", PrimitiveType.booleanType());

         synchronized (javaParser) {
            final ClassOrInterfaceType type = javaParser.parseClassOrInterfaceType("DataCollectorList").getResult().get();
            if (datacollectorlist.equals(DataCollectorList.ONLYTIME)) {
               addMethod(clazz, "getDataCollectors", "return DataCollectorList.ONLYTIME;", type);
            } else if (datacollectorlist.equals(DataCollectorList.ONLYTIME_NOGC)) {
               addMethod(clazz, "getDataCollectors", "return DataCollectorList.ONLYTIME_NOGC;", type);
            }
         }
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

   /**
    * Edits Java so that the class is run with the KoPeMe-Testrunner and the methods are annotated additionally with @PerformanceTest.
    * 
    * @param javaFile File for editing
    */
   protected void editJUnit4(final File javaFile) {
      try {
         final CompilationUnit unit = loadedFiles.get(javaFile);

         editJUnit4(unit);

         Files.write(javaFile.toPath(), unit.toString().getBytes(charset));
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   protected void editJUnit5(final File javaFile) {
      try {
         final CompilationUnit unit = loadedFiles.get(javaFile);

         editJUnit5(unit);

         Files.write(javaFile.toPath(), unit.toString().getBytes(charset));
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   void editJUnit5(final CompilationUnit unit) {
      unit.addImport("org.junit.jupiter.api.extension.ExtendWith");
      unit.addImport("de.dagere.kopeme.junit5.rule.KoPeMeExtension");

      final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);

      final SingleMemberAnnotationExpr extendAnnotation = new SingleMemberAnnotationExpr(new Name("ExtendWith"), new ClassExpr(new TypeParameter("KoPeMeExtension")));
      clazz.addAnnotation(extendAnnotation);

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
            setPublic(method);
            addAnnotation(method);
         }
      }
   }

   void editJUnit4(final CompilationUnit unit) {
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
            setPublic(method);
            addAnnotation(method);
         }
      }
   }

   private void setPublic(final MethodDeclaration method) {
      if (!method.isPublic()) {
         method.setPublic(true);
         method.setPrivate(false);
         method.setProtected(false);
         method.setDefault(false);
      }
   }

   public void addAnnotation(final MethodDeclaration method) {
      for (final AnnotationExpr annotation : method.getAnnotations()) {
         if (annotation.getNameAsString().contains("PerformanceTest")) {
            LOG.info("Found annotation " + annotation.getNameAsString() + " - do not add annotation");
            return;
         }
      }

      final NormalAnnotationExpr performanceTestAnnotation = new NormalAnnotationExpr();
      performanceTestAnnotation.setName("de.dagere.kopeme.annotations.PerformanceTest");
      performanceTestAnnotation.addPair("iterations", "" + config.getIterations());
      performanceTestAnnotation.addPair("warmup", "" + config.getWarmup());
      performanceTestAnnotation.addPair("logFullData", "" + true);
      performanceTestAnnotation.addPair("useKieker", "" + config.isUseKieker());
      performanceTestAnnotation.addPair("timeout", "" + config.getTimeout());
      performanceTestAnnotation.addPair("repetitions", "" + config.getRepetitions());
      performanceTestAnnotation.addPair("redirectToNull", "" + config.isRedirectToNull());
      if (datacollectorlist.equals(DataCollectorList.ONLYTIME)) {
         performanceTestAnnotation.addPair("dataCollectors", "\"ONLYTIME\"");
      } else if (datacollectorlist.equals(DataCollectorList.ONLYTIME_NOGC)) {
         performanceTestAnnotation.addPair("dataCollectors", "\"ONLYTIME_NOGC\"");
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

   private void addRule(final ClassOrInterfaceDeclaration clazz) {
      final NodeList<Expression> arguments = new NodeList<>();
      arguments.add(new ThisExpr());
      final Expression initializer = new ObjectCreationExpr(null, new ClassOrInterfaceType("KoPeMeRule"), arguments);
      final FieldDeclaration fieldDeclaration = clazz.addFieldWithInitializer(TestRule.class, "kopemeRule", initializer, Modifier.publicModifier().getKeyword());
      final NormalAnnotationExpr annotation = new NormalAnnotationExpr();
      annotation.setName("Rule");
      fieldDeclaration.getAnnotations().add(annotation);
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

   public File generateClazz(final File module, final ChangedEntity generatedClazz, final ChangedEntity callee, final String method) {
      return new JUnitTestGenerator(module, generatedClazz, callee, method, this).generateClazz();
   }

   public boolean isAggregatedWriter() {
      return aggregatedWriter;
   }

   public void setAggregatedWriter(final boolean aggregatedWriter) {
      this.aggregatedWriter = aggregatedWriter;
   }

   public void setIgnoreEOIs(final boolean ignoreEOIs) {
      this.ignoreEOIs = ignoreEOIs;
   }

   public boolean isIgnoreEOIs() {
      return ignoreEOIs;
   }

   public MeasurementConfiguration getConfig() {
      return config;
   }
}
