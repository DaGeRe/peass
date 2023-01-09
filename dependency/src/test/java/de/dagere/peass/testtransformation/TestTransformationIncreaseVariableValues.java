/**
 * This file is part of PerAn.
 *
 * PerAn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * PerAn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.dagere.peass.testtransformation;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;

/**
 * Tests the transformationmethode of classes.
 *
 * @author reichelt ExecutionConfig
 */
public class TestTransformationIncreaseVariableValues {

   @TempDir
   public static File testFolder;

   private static final URL SOURCE = Thread.currentThread().getContextClassLoader().getResource("transformation");
   private static File RESOURCE_FOLDER;
   private static File SOURCE_FOLDER;

   private File testFile;

   @BeforeAll
   public static void initFolder() throws URISyntaxException, IOException {
      RESOURCE_FOLDER = Paths.get(SOURCE.toURI()).toFile();
      SOURCE_FOLDER = new File(testFolder, "src/test/java");
      FileUtils.copyFile(new File(RESOURCE_FOLDER, "pom.xml"),
            new File(testFolder, "pom.xml"));
   }

   @ParameterizedTest
   @ValueSource(strings = { "10000", "100000" })
   public void testJUnit5Transformation(String value) throws IOException {
      final File old2 = new File(RESOURCE_FOLDER, "TestMe9.java");
      testFile = new File(SOURCE_FOLDER, "TestMe9.java");
      FileUtils.copyFile(old2, testFile);
      MeasurementConfig config = MeasurementConfig.DEFAULT;

      config.getExecutionConfig().setIncreaseVariableValues(Collections.singletonList("de.test.TestMe9.WAIT_FOR_INITIALIZATION:" + value));

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, config);
      tt.determineVersions(Arrays.asList(new File[] { testFolder }));
      tt.transformTests();

      final CompilationUnit cu = JavaParserProvider.parse(testFile);

      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("TestMe9").get();
      Assert.assertNotNull(clazz);

      final List<MethodDeclaration> methodsByName = clazz.getMethodsByName("testMethod9");
      MatcherAssert.assertThat(methodsByName, Matchers.hasSize(1));

      final MethodDeclaration testMethod = methodsByName.get(0);

      final AnnotationExpr performanceTestAnnotation = testMethod.getAnnotationByName("PerformanceTest").get();
      Assert.assertNotNull(performanceTestAnnotation);

      MatcherAssert.assertThat(performanceTestAnnotation.getChildNodes(), TestTransformation.hasAnnotation("iterations"));
      MatcherAssert.assertThat(performanceTestAnnotation.getChildNodes(), TestTransformation.hasAnnotation("warmup"));

      for (final Node n : performanceTestAnnotation.getChildNodes()) {
         System.out.println(n);
      }

      FieldDeclaration field = clazz.getFieldByName("WAIT_FOR_INITIALIZATION").get();

      String number = field.toString().substring(field.toString().lastIndexOf(" ") + 1).replaceAll(";", "");
      Assert.assertEquals(value, number);

   }

   @After
   public void cleanup() {
      testFile.delete();
   }
}
