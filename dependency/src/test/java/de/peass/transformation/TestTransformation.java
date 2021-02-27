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
package de.peass.transformation;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.changesreading.JavaParserProvider;
import de.peass.testtransformation.JUnitTestTransformer;

/**
 * Tests the transformation of classes.
 * 
 * @author reichelt
 *
 */
public class TestTransformation {

   @ClassRule
   public static TemporaryFolder testFolder = new TemporaryFolder();

   private static final URL SOURCE = Thread.currentThread().getContextClassLoader().getResource("transformation");
   private static File RESOURCE_FOLDER;
   private static File SOURCE_FOLDER;

   private File testFile;

   @BeforeClass
   public static void initFolder() throws URISyntaxException, IOException {
      RESOURCE_FOLDER = Paths.get(SOURCE.toURI()).toFile();
      SOURCE_FOLDER = new File(testFolder.getRoot(), "src/test/java");
      FileUtils.copyFile(new File(RESOURCE_FOLDER, "pom.xml"), new File(testFolder.getRoot(), "pom.xml"));
   }

   @Test
   public void testJUnit3Transformation() throws IOException {

      final File old = new File(RESOURCE_FOLDER, "TestMe1.java");
      testFile = new File(SOURCE_FOLDER, "TestMe1.java");
      FileUtils.copyFile(old, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder.getRoot() }));
      tt.transformTests();

      final CompilationUnit cu = JavaParserProvider.parse(testFile);

      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("TestMe1").get();
      Assert.assertNotNull(clazz);

      Assert.assertEquals("KoPeMeTestcase", clazz.getExtendedTypes(0).getName().getIdentifier());

      Assert.assertThat(clazz.getMethodsByName("getWarmup"), Matchers.hasSize(1));
      Assert.assertThat(clazz.getMethodsByName("getIterations"), Matchers.hasSize(1));
   }

   @Test
   public void testJUnit3Twice() throws IOException {
      final File old = new File(RESOURCE_FOLDER, "TestMe6.java");
      testFile = new File(SOURCE_FOLDER, "TestMe6.java");
      FileUtils.copyFile(old, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder.getRoot() }));
      tt.transformTests();

      final CompilationUnit cu = JavaParserProvider.parse(testFile);

      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("TestMe6").get();
      Assert.assertNotNull(clazz);

      Assert.assertEquals("KoPeMeTestcase", clazz.getExtendedTypes(0).getName().getIdentifier());

      Assert.assertThat(clazz.getMethodsByName("getWarmupExecutions"), Matchers.hasSize(1));
      Assert.assertThat(clazz.getMethodsByName("getExecutionTimes"), Matchers.hasSize(1));

      Assert.assertTrue(FileUtils.contentEquals(old, testFile));
   }

   @Test
   public void testJUnit3Twice2() throws IOException {
      final File old = new File(RESOURCE_FOLDER, "TestMe7.java");
      testFile = new File(SOURCE_FOLDER, "TestMe7.java");
      FileUtils.copyFile(old, testFile);
      final File superClass = new File(RESOURCE_FOLDER, "MySuper.java");
      final File goal = new File(SOURCE_FOLDER, "MySuper.java");
      FileUtils.copyFile(superClass, goal);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder.getRoot() }));
      tt.transformTests();

      final CompilationUnit cu = JavaParserProvider.parse(testFile);

      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("TestMe7").get();
      Assert.assertNotNull(clazz);

      Assert.assertEquals("MySuper", clazz.getExtendedTypes(0).getName().getIdentifier());

      Assert.assertThat(clazz.getMethodsByName("getWarmupExecutions"), Matchers.hasSize(1));
      Assert.assertThat(clazz.getMethodsByName("getExecutionTimes"), Matchers.hasSize(1));
   }

   @Test
   public void testJUnit4Transformation() throws IOException {
      final File old2 = new File(RESOURCE_FOLDER, "TestMe2.java");
      testFile = new File(SOURCE_FOLDER, "TestMe2.java");
      FileUtils.copyFile(old2, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder.getRoot() }));
      tt.transformTests();

      final CompilationUnit cu = JavaParserProvider.parse(testFile);

      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("TestMe2").get();
      Assert.assertNotNull(clazz);

      final List<MethodDeclaration> methodsByName = clazz.getMethodsByName("testMethod1");
      Assert.assertThat(methodsByName, Matchers.hasSize(1));

      final MethodDeclaration testMethod = methodsByName.get(0);

      final AnnotationExpr performanceTestAnnotation = testMethod.getAnnotationByName("PerformanceTest").get();
      Assert.assertNotNull(performanceTestAnnotation);

      Assert.assertThat(performanceTestAnnotation.getChildNodes(), hasAnnotation("iterations"));
      Assert.assertThat(performanceTestAnnotation.getChildNodes(), hasAnnotation("warmup"));

      for (final Node n : performanceTestAnnotation.getChildNodes()) {
         System.out.println(n);
      }
   }

   @Test
   public void testJUnit4TransformationRunner() throws IOException {
      final File old = new File(RESOURCE_FOLDER, "TestMe3.java");
      testFile = new File(SOURCE_FOLDER, "TestMe3.java");
      FileUtils.copyFile(old, testFile);
      System.out.println(testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder.getRoot() }));
      tt.transformTests();

      Assert.assertFalse(FileUtils.contentEquals(old, testFile));
   }

   @Test
   public void testJUnit4Twice() throws IOException {
      final File old = new File(RESOURCE_FOLDER, "TestMe4.java");
      testFile = new File(SOURCE_FOLDER, "TestMe4.java");
      FileUtils.copyFile(old, testFile);
      System.out.println(testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder.getRoot() }));
      tt.transformTests();

      Assert.assertTrue(FileUtils.contentEquals(old, testFile));
   }

   @Test
   public void testOtherRule() throws IOException {
      final File old = new File(RESOURCE_FOLDER, "TestMe5.java");
      testFile = new File(SOURCE_FOLDER, "TestMe5.java");
      FileUtils.copyFile(old, testFile);
      System.out.println(testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder.getRoot() }));
      tt.transformTests();

      Assert.assertFalse(FileUtils.contentEquals(old, testFile));
   }

   @After
   public void cleanup() {
      testFile.delete();
   }

   public static Matcher<List<Node>> hasAnnotation(final String annotationName) {
      return new BaseMatcher<List<Node>>() {
         @Override
         public boolean matches(final Object item) {
            final List<Node> nodes = (List<Node>) item;
            boolean contained = false;
            for (final Node node : nodes) {
               if (node instanceof MemberValuePair) {
                  final MemberValuePair pair = (MemberValuePair) node;
                  if (annotationName.equals(pair.getName().getIdentifier())) {
                     contained = true;
                     break;
                  }
               }
               System.out.println(node.getClass());
            }
            return contained;
         }

         @Override
         public void describeTo(final Description description) {
            description.appendText("Expected an annotation with value ").appendValue(annotationName);
         }
      };
   }
}
