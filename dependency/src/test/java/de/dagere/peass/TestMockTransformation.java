package de.dagere.peass;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.ParseUtil;
import de.dagere.peass.transformation.TestTransformation;

public class TestMockTransformation {
   @TempDir
   public static File testFolder;

   private static File RESOURCE_FOLDER = new File(TestConstants.TEST_RESOURCES, "transformation");
   private static File SOURCE_FOLDER;

   @BeforeAll
   public static void initFolder() throws URISyntaxException, IOException {
      SOURCE_FOLDER = new File(testFolder, "src" + File.separator + "test" + File.separator + "java");
      FileUtils.copyFile(new File(RESOURCE_FOLDER, "pom.xml"), new File(testFolder, "pom.xml"));
   }

   @Disabled
   @Test
   public void testMocked() throws IOException {
      final File old2 = new File(RESOURCE_FOLDER, "TestMocked.java");
      final File testFile2 = new File(SOURCE_FOLDER, "TestMocked.java");
      FileUtils.copyFile(old2, testFile2);

      MeasurementConfig config = new MeasurementConfig(5);
      config.getExecutionConfig().setClearMockitoCaches(true);
      config.getExecutionConfig().setExecuteBeforeClassInMeasurement(true);
      
      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, config);
      tt.determineVersions(Arrays.asList(new File[] { testFolder }));
      tt.transformTests();

      final CompilationUnit cu = JavaParserProvider.parse(testFile2);

      final ClassOrInterfaceDeclaration clazz = cu.getClassByName("TestMocked").get();
      Assert.assertNotNull(clazz);

      final List<MethodDeclaration> beforeMethods = clazz.getMethodsByName("setUp");
      MatcherAssert.assertThat(beforeMethods, Matchers.hasSize(1));

      final MethodDeclaration beforeMethod = beforeMethods.get(0);

      MatcherAssert.assertThat(beforeMethod.toString(), StringContains.containsString("myMock = Mockito.mock(Object.class))"));
      
      FieldDeclaration fieldDeclaration = clazz.getFieldByName("myMock").get();
      MatcherAssert.assertThat(fieldDeclaration.toString(), Matchers.not(StringContains.containsString("myMock = Mockito.mock(Object.class))")));
   }
}
