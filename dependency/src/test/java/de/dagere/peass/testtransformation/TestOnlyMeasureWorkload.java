package de.dagere.peass.testtransformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;

public class TestOnlyMeasureWorkload {

   @TempDir
   public static File testFolder;

   private static final File RESOURCE_FOLDER = new File(TestConstants.TEST_RESOURCES, "transformation");
   private static File SOURCE_FOLDER;

   @BeforeAll
   public static void initFolder() throws URISyntaxException, IOException {
      SOURCE_FOLDER = new File(testFolder, "src" + File.separator + "test" + File.separator + "java");
      FileUtils.copyFile(new File(RESOURCE_FOLDER, "pom.xml"), new File(testFolder, "pom.xml"));
   }

   @Test
   public void testOnlyMeasureWorkloadJUnit4() throws IOException {
      final File untransformed = new File(RESOURCE_FOLDER, "TestMeBeforeAfter.java");
      final File testFile = new File(SOURCE_FOLDER, "TestMeBeforeAfter.java");
      FileUtils.copyFile(untransformed, testFile);

      final CompilationUnit cu = executeTransformation(testFile);

      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("TestMeBeforeAfter").get();
      Assert.assertNotNull(clazz);

      checkTestMethod(clazz);

      checkBefore(clazz);

      checkAfter(clazz);
   }

   @Test
   public void testOnlyMeasureWorkloadJUnit5() throws IOException {
      final File untransformed = new File(RESOURCE_FOLDER, "TestMeBeforeAfter5.java");
      final File testFile = new File(SOURCE_FOLDER, "TestMeBeforeAfter5.java");
      FileUtils.copyFile(untransformed, testFile);

      final CompilationUnit cu = executeTransformation(testFile);

      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("TestMeBeforeAfter5").get();
      Assert.assertNotNull(clazz);

      checkTestMethod(clazz);

      checkBefore(clazz);

      checkAfter(clazz);
   }

   private CompilationUnit executeTransformation(final File testFile) throws FileNotFoundException {
      MeasurementConfig config = new MeasurementConfig(2);
      config.getExecutionConfig().setOnlyMeasureWorkload(true);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, config);
      tt.determineVersions(Arrays.asList(new File[] { testFolder }));
      tt.transformTests();

      final CompilationUnit cu = JavaParserProvider.parse(testFile);
      return cu;
   }

   private void checkTestMethod(final ClassOrInterfaceDeclaration clazz) {
      final List<MethodDeclaration> methodsByName = clazz.getMethodsByName("testMethod1");
      MatcherAssert.assertThat(methodsByName, Matchers.hasSize(1));
      final MethodDeclaration testMethod = methodsByName.get(0);
      final AnnotationExpr performanceTestAnnotation = testMethod.getAnnotationByName("PerformanceTest").get();
      Assert.assertNotNull(performanceTestAnnotation);
   }

   private void checkAfter(final ClassOrInterfaceDeclaration clazz) {
      final List<MethodDeclaration> afterMethods = clazz.getMethodsByName("simpleAfter");
      MethodDeclaration afterMethod = afterMethods.get(0);
      Optional<AnnotationExpr> afterAnnotation = afterMethod.getAnnotationByName("AfterNoMeasurement");
      Assert.assertTrue(afterAnnotation.isPresent());
   }

   private void checkBefore(final ClassOrInterfaceDeclaration clazz) {
      final List<MethodDeclaration> beforeMethods = clazz.getMethodsByName("simpleBefore");
      MethodDeclaration beforeMethod = beforeMethods.get(0);
      Optional<AnnotationExpr> beforeAnnotation = beforeMethod.getAnnotationByName("BeforeNoMeasurement");
      Assert.assertTrue(beforeAnnotation.isPresent());
   }
}
