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
package de.dagere.peass.testtransformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.dagere.kopeme.parsing.JUnitParseUtil;
import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.execution.utils.ProjectModules;

/**
 * Transforms JUnit-Tests to performance tests.
 * 
 * @author reichelt
 *
 */
public class JUnitTestTransformer implements TestTransformer {

   private static final Logger LOG = LogManager.getLogger(JUnitTestTransformer.class);

   protected DataCollectorList datacollectorlist;
   protected final MeasurementConfig config;
   protected File projectFolder;
   protected boolean ignoreEOIs = false;
   protected Charset charset = StandardCharsets.UTF_8;
   private Map<String, List<File>> extensions = null;

   private final JavaParser javaParser = new JavaParser();

   /**
    * Initializes TestTransformer with folder.
    * 
    * @param projectFolder Folder, where tests should be transformed
    */
   public JUnitTestTransformer(final File projectFolder, final MeasurementConfig config) {
      LOG.debug("Test transformer for {} created", projectFolder);
      this.projectFolder = projectFolder;
      this.config = config;
      datacollectorlist = config.isUseGC() ? DataCollectorList.ONLYTIME : DataCollectorList.ONLYTIME_NOGC;
   }

   /**
    * Creates a test transformer for usage in RTS - only used for unit testing, since the generic constructor with File and MeasurementConfig should be used everywhere
    * 
    * @param projectFolder
    * @param timeout
    */
   public JUnitTestTransformer(final File projectFolder, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig) {
      this.projectFolder = projectFolder;
      config = new MeasurementConfig(1, executionConfig, kiekerConfig);
      config.setIterations(1);
      config.setWarmup(0);
      // For regression test selection, verbose debug output is helpful, and since it is only executed once, the performance loss is ok.
      config.getExecutionConfig().setRedirectToNull(false);
      config.setShowStart(true);
      datacollectorlist = DataCollectorList.ONLYTIME;
   }

   private Map<File, CompilationUnit> loadedFiles;

   public Map<File, CompilationUnit> getLoadedFiles() {
      return loadedFiles;
   }

   private Map<File, Integer> junitVersions;

   @Override
   public void determineVersions(final List<File> modules) {
      String[] pathes = config.getExecutionConfig().getTestClazzFolders().toArray(new String[0]);
      determineVersionsForPaths(modules, pathes);
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
               LOG.trace("Test folder {} does not exist", testFolder.getAbsolutePath());
            }
         }
      }
   }

   public Map<File, Integer> getJunitVersions() {
      return junitVersions;
   }

   @Override
   public TestSet findModuleTests(final ModuleClassMapping mapping, final List<String> includedModules, final ProjectModules modules) {
      determineVersions(modules.getModules());
      final TestSet allTests = new TestSet();
      for (final File module : modules.getModules()) {
         final TestSet moduleTests = findModuleTests(mapping, includedModules, module);
         allTests.addTestSet(moduleTests);
      }
      LOG.info("Included tests: {}", allTests.getTests().size());
      return allTests;
   }

   private TestSet findModuleTests(final ModuleClassMapping mapping, final List<String> includedModules, final File module) {
      final TestSet moduleTests = new TestSet();
      ClazzFileFinder finder = new ClazzFileFinder(config.getExecutionConfig());
      for (final String clazz : finder.getTestClazzes(module)) {
         final String currentModule = mapping.getModuleOfClass(clazz);
         final List<TestCase> testMethodNames = getTestMethodNames(module, new TestCase(clazz, null, currentModule));
         for (TestCase test : testMethodNames) {
            if (includedModules == null || includedModules.contains(test.getModule())) {
               addTestIfIncluded(moduleTests, test);
            }
         }
      }
      return moduleTests;
   }

   private void addTestIfIncluded(final TestSet moduleTests, final TestCase test) {
      if (NonIncludedTestRemover.isTestIncluded(test, getConfig().getExecutionConfig())) {
         moduleTests.addTest(test);
      }
   }

   @Override
   public TestSet buildTestMethodSet(final TestSet testsToUpdate, final List<File> modules) {
      final TestSet tests = new TestSet();
      determineVersions(modules);
      for (final TestCase clazzname : testsToUpdate.getClasses()) {
         final Set<String> currentClazzMethods = testsToUpdate.getMethods(clazzname);
         if (currentClazzMethods == null || currentClazzMethods.isEmpty()) {
            final File moduleFolder = new File(projectFolder, clazzname.getModule());
            final List<TestCase> methods = getTestMethodNames(moduleFolder, clazzname);
            for (final TestCase test : methods) {
               addTestIfIncluded(tests, test);
            }
         } else {
            for (final String method : currentClazzMethods) {
               TestCase test = new TestCase(clazzname.getClazz(), method, clazzname.getModule());
               addTestIfIncluded(tests, test);
            }
         }
      }
      return tests;
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

      LOG.debug("JUnit Versions Determined: {}", junitVersions.size());
      for (final Map.Entry<File, Integer> fileVersionEntry : junitVersions.entrySet()) {
         LOG.debug("Editing test file: {} {}", fileVersionEntry.getKey(), fileVersionEntry.getValue()); // TODO change to trace
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
      LOG.trace("Loading: {} {}", clazzFile, junitVersions);
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
            File canonicalJavaFile = javaFile.getCanonicalFile();
            final CompilationUnit unit = JavaParserProvider.parse(canonicalJavaFile);
            loadedFiles.put(canonicalJavaFile, unit);
            final boolean isJUnit4 = isJUnit(unit, 4);
            if (isJUnit4) {
               junitVersions.put(canonicalJavaFile, 4);
               // editJUnit4(javaFile);
            }
            final boolean isJUnit5 = isJUnit(unit, 5);
            if (isJUnit5) {
               junitVersions.put(canonicalJavaFile, 5);
               // editJUnit4(javaFile);
            }
            parseExtensions(canonicalJavaFile, unit);
         } catch (final IOException e) {
            throw new RuntimeException(e);
         }
      }

      addJUnit3Test("TestCase", junitVersions);
   }

   private void parseExtensions(final File canonicalJavaFile, final CompilationUnit unit) {
      for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
         LOG.trace("Transforming: {}", clazz.getNameAsString());
         // We only need to consider classes with one extends, since classes can not have multiple extends and we search for classes that may extend TestCase (indirectly)
         if (clazz.getExtendedTypes().size() == 1) {
            final ClassOrInterfaceType extend = clazz.getExtendedTypes(0);
            final String extensionName = extend.getNameAsString().intern();
            List<File> extensionsOfBase = extensions.get(extensionName);
            if (extensionsOfBase == null) {
               extensionsOfBase = new LinkedList<>();
               extensions.put(extensionName, extensionsOfBase);
            }
            extensionsOfBase.add(canonicalJavaFile);
         }
      }
   }

   private final Map<Integer, List<String>> junitTestAnnotations = new HashMap<>();
   {
      junitTestAnnotations.put(5, Arrays.asList("org.junit.jupiter.api.Test", "org.junit.jupiter.params.ParameterizedTest"));
      junitTestAnnotations.put(4, Arrays.asList("org.junit.Test"));
   }

   private boolean isJUnit(final CompilationUnit unit, final int version) {
      final List<String> importNameVersions = junitTestAnnotations.get(version);
      boolean isJUnitVersion = false;
      for (final ImportDeclaration currentImport : unit.getImports()) {
         final Name importName = currentImport.getName();
         for (String importNameVersion : importNameVersions) {
            if (importName.toString().equals(importNameVersion)) {
               isJUnitVersion = true;
            }
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

   @Override
   public List<TestCase> getTestMethodNames(final File module, final TestCase clazzname) {
      final List<TestCase> methods = new LinkedList<>();
      ClazzFileFinder finder = new ClazzFileFinder(config.getExecutionConfig());
      final File clazzFile = finder.getClazzFile(module, clazzname);
      final CompilationUnit unit = loadedFiles.get(clazzFile);
      if (unit != null) {
         final Integer junit = junitVersions.get(clazzFile);
         if (junit != null) {
            for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
               addTestMethodNames(clazzname, methods, junit, clazz);
            }
         } else {
            LOG.warn("Clazz {} has no JUnit version", clazzFile);
         }
      } else {
         /**
          * By default, the dependency selection adds all changed clazzes as tests (since a class not containing a test may contain a new test), so this is mostly not a real error
          */
         LOG.error("Did not find {} for {} - class not loaded (since it is not a test class?)", clazzFile, clazzname);
      }

      return methods;
   }

   private void addTestMethodNames(final TestCase clazzname, final List<TestCase> methods, final Integer junit, final ClassOrInterfaceDeclaration clazz) {
      if (junit == 3) {
         for (final MethodDeclaration method : clazz.getMethods()) {
            if (method.getNameAsString().toLowerCase().contains("test")) {
               methods.add(new TestCase(clazzname.getClazz(), method.getNameAsString(), clazzname.getModule()));
            }
         }
      } else if (junit == 4) {
         for (String junit4method : getAnnotatedMethods(clazz, 4)) {
            TestCase test = new TestCase(clazzname.getClazz(), junit4method, clazzname.getModule());
            methods.add(test);
         }
      } else if (junit == 5) {
         for (String junit5method : getAnnotatedMethods(clazz, 5)) {
            TestCase test = new TestCase(clazzname.getClazz(), junit5method, clazzname.getModule());
            methods.add(test);
         }
      }
   }

   private List<String> getAnnotatedMethods(final ClassOrInterfaceDeclaration clazz, final int version) {
      final List<String> importNameVersions = junitTestAnnotations.get(version);
      final List<String> methods = new LinkedList<>();
      for (String importNameVersion : importNameVersions) {
         String annotationName = importNameVersion.substring(importNameVersion.lastIndexOf('.') + 1);
         methods.addAll(JUnitParseUtil.getAnnotatedMethods(clazz, importNameVersion, annotationName));
      }
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

      for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
         if (!clazz.getExtendedTypes(0).getNameAsString().equals("KoPeMeTestcase")) {
            if (clazz.getExtendedTypes(0).getNameAsString().equals("TestCase")) {
               clazz.setExtendedTypes(new NodeList<>());
               clazz.addExtendedType("KoPeMeTestcase");
            }

            addMethod(clazz, "getWarmup", "return " + 0 + ";", PrimitiveType.intType());
            addMethod(clazz, "getIterations", "return " + config.getAllIterations() + ";", PrimitiveType.intType());
            addMethod(clazz, "logFullData", "return " + config.isLogFullData() + ";", PrimitiveType.booleanType());
            addMethod(clazz, "useKieker", "return " + config.isUseKieker() + ";", PrimitiveType.booleanType());
            addMethod(clazz, "getMaximalTime", "return " + config.getExecutionConfig().getTimeout() + ";", PrimitiveType.longType());
            addMethod(clazz, "getRepetitions", "return " + config.getRepetitions() + ";", PrimitiveType.intType());
            addMethod(clazz, "redirectToNull", "return " + config.getExecutionConfig().isRedirectToNull() + ";", PrimitiveType.booleanType());

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

         JUnit4Helper.editJUnit4(unit, config, datacollectorlist);

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

      for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
         final SingleMemberAnnotationExpr extendAnnotation = new SingleMemberAnnotationExpr(new Name("ExtendWith"), new ClassExpr(new TypeParameter("KoPeMeExtension")));
         clazz.addAnnotation(extendAnnotation);

         List<MethodDeclaration> testMethods = TestMethodFinder.findJUnit5TestMethods(clazz);
         new TestMethodHelper(config, datacollectorlist).prepareTestMethods(testMethods);

         BeforeAfterTransformer.transformBeforeAfter(clazz, config.getExecutionConfig());
      }
   }

   public File getProjectFolder() {
      return projectFolder;
   }

   public void setEncoding(final Charset encoding) {
      charset = encoding;
   }

   @Override
   public boolean isJUnit3() {
      boolean junit3 = false;
      for (final Entry<File, Integer> clazz : junitVersions.entrySet()) {
         if (clazz.getValue() == 3) {
            junit3 = true;
         }
      }
      return junit3;
   }

   @Override
   public void setIgnoreEOIs(final boolean ignoreEOIs) {
      this.ignoreEOIs = ignoreEOIs;
   }

   @Override
   public boolean isIgnoreEOIs() {
      return ignoreEOIs;
   }

   @Override
   public MeasurementConfig getConfig() {
      return config;
   }
}
