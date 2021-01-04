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
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.JavaParserProvider;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.testtransformation.JUnitTestTransformer;

/**
 * Tests the transformation of classes.
 * 
 * @author reichelt
 *
 */
public class TestGeneration {

   @ClassRule
   public static TemporaryFolder testFolder = new TemporaryFolder(new File("target"));

   private static final URL SOURCE = Thread.currentThread().getContextClassLoader().getResource("generation");
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
   public void testJUnit3Generation() throws IOException {

      final File old = new File(RESOURCE_FOLDER, "TestMe1.java");
      testFile = new File(SOURCE_FOLDER, "TestMe1.java");
      FileUtils.copyFile(old, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] {testFolder.getRoot()}));
      final File generated = tt.generateClazz(testFolder.getRoot(), new ChangedEntity("de.GeneratedClass", ""), new ChangedEntity("TestMe1", ""), "testMe1");
      final CompilationUnit cu = JavaParserProvider.parse(generated);
      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("GeneratedClass").get();
      
      Assert.assertNotNull(clazz);

      Assert.assertEquals("KoPeMeTestcase", clazz.getExtendedTypes(0).getName().getIdentifier());

      Assert.assertThat(clazz.getMethodsByName("getWarmup"), Matchers.hasSize(1));
      Assert.assertThat(clazz.getMethodsByName("getIterations"), Matchers.hasSize(1));
      
      Assert.assertThat(clazz.getMethodsByName("setUp"), Matchers.hasSize(1));
      Assert.assertThat(clazz.getMethodsByName("tearDown"), Matchers.hasSize(1));
   }
   
   @Test
   public void testJUnit3Extension() throws IOException {

      final File old = new File(RESOURCE_FOLDER, "ExtendingTest.java");
      testFile = new File(SOURCE_FOLDER, "ExtendingTest.java");
      FileUtils.copyFile(old, testFile);
      final File old2 = new File(RESOURCE_FOLDER, "TestMe1.java");
      final File parentTestFile = new File(SOURCE_FOLDER, "TestMe1.java");
      FileUtils.copyFile(old2, parentTestFile);
      
      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] {testFolder.getRoot()}));
      final File generated = tt.generateClazz(testFolder.getRoot(), new ChangedEntity("de.GeneratedClass", ""), new ChangedEntity("ExtendingTest", ""), "testMe1");
      final CompilationUnit cu = JavaParserProvider.parse(generated);
      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("GeneratedClass").get();
      
      Assert.assertNotNull(clazz);

      Assert.assertEquals("KoPeMeTestcase", clazz.getExtendedTypes(0).getName().getIdentifier());

      Assert.assertThat(clazz.getMethodsByName("getWarmup"), Matchers.hasSize(1));
      Assert.assertThat(clazz.getMethodsByName("getIterations"), Matchers.hasSize(1));
      
      Assert.assertThat(clazz.getMethodsByName("setUp"), Matchers.hasSize(1));
      Assert.assertThat(clazz.getMethodsByName("tearDown"), Matchers.hasSize(1));
   }
   
   @Test
   public void testJUnit4Generation() throws IOException {
      final File old2 = new File(RESOURCE_FOLDER, "TestMe2.java");
      testFile = new File(SOURCE_FOLDER, "TestMe2.java");
      FileUtils.copyFile(old2, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] {testFolder.getRoot()}));
      final File generated = tt.generateClazz(testFolder.getRoot(), new ChangedEntity("de.GeneratedClass", ""), new ChangedEntity("TestMe2", ""), "testMethod");
      final CompilationUnit cu = JavaParserProvider.parse(generated);
      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("GeneratedClass").get();

      Assert.assertNotNull(clazz);

      final List<MethodDeclaration> methodsByName = clazz.getMethodsByName("testMethod");
      Assert.assertThat(methodsByName, Matchers.hasSize(1));

      final MethodDeclaration testMethod = methodsByName.get(0);

      final AnnotationExpr performanceTestAnnotation = testMethod.getAnnotationByName("PerformanceTest").get();
      Assert.assertNotNull(performanceTestAnnotation);

      Assert.assertThat(performanceTestAnnotation.getChildNodes(), hasAnnotation("iterations"));
      Assert.assertThat(performanceTestAnnotation.getChildNodes(), hasAnnotation("warmup"));

      for (final Node n : performanceTestAnnotation.getChildNodes()) {
         System.out.println(n);
      }
      
      Assert.assertThat(clazz.getMethodsByName("init"), Matchers.hasSize(1));
      Assert.assertThat(clazz.getMethodsByName("init2"), Matchers.hasSize(1));
      System.out.println("Modifiers: " + clazz.getMethodsByName("init2").get(0).getModifiers());
      Assert.assertThat(clazz.getMethodsByName("init2").get(0).getModifiers(), Matchers.hasSize(2));
      
      Assert.assertThat(clazz.getMethodsByName("clear"), Matchers.hasSize(1));
      Assert.assertThat(clazz.getMethodsByName("clear2"), Matchers.hasSize(1));
   }

   @Test
   public void testJUnit4TransformationRunner() throws IOException {
      final File old = new File(RESOURCE_FOLDER, "TestMe3.java");
      testFile = new File(SOURCE_FOLDER, "TestMe3.java");
      FileUtils.copyFile(old, testFile);
      System.out.println(testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] {testFolder.getRoot()}));
      
      final File generated = tt.generateClazz(testFolder.getRoot(), new ChangedEntity("de.GeneratedClass", ""), new ChangedEntity("TestMe3", ""), "testMethod");
      final CompilationUnit cu = JavaParserProvider.parse(generated);
      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("GeneratedClass").get();

      Assert.assertFalse(FileUtils.contentEquals(old, generated));
   }
     
   @Test
   public void testOtherRule() throws IOException {
      final File old = new File(RESOURCE_FOLDER, "TestMe5.java");
      testFile = new File(SOURCE_FOLDER, "TestMe5.java");
      FileUtils.copyFile(old, testFile);
      System.out.println(testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder.getRoot(), MeasurementConfiguration.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] {testFolder.getRoot()}));
      
      final File generated = tt.generateClazz(testFolder.getRoot(), new ChangedEntity("de.GeneratedClass", ""), new ChangedEntity("TestMe5", ""), "testMethod");
      final CompilationUnit cu = JavaParserProvider.parse(generated);
      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("GeneratedClass").get();
      
      final List<MethodDeclaration> methodsByName = clazz.getMethodsByName("testMethod");
      Assert.assertThat(methodsByName, Matchers.hasSize(1));
      
      
      final MethodDeclaration methodDeclaration = methodsByName.get(0);
      for (final AnnotationExpr an : methodDeclaration.getAnnotations()) {
         System.out.println(an);
      }
      Assert.assertThat(methodDeclaration.getAnnotations(), Matchers.hasSize(2));
      Assert.assertThat(methodDeclaration.getAnnotationByName("Test").get().toString(), Matchers.containsString("expected"));
      Assert.assertThat(methodDeclaration.getThrownExceptions(), Matchers.hasSize(1));
      
      
      
      Assert.assertTrue(clazz.getFieldByName("testFolder2").isPresent());
      final FieldDeclaration field = clazz.getFieldByName("testFolder2").get();
      Assert.assertTrue(field.toString().contains(" = new TemporaryFolder();"));
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
