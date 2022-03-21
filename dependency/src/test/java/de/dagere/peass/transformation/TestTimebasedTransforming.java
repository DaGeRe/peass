package de.dagere.peass.transformation;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.ParseUtil;
import de.dagere.peass.testtransformation.TimeBasedTestTransformer;

public class TestTimebasedTransforming {
   
   @TempDir
	public static File testFolder;

	private static final URL SOURCE = Thread.currentThread().getContextClassLoader().getResource("transformation");
	private static File RESOURCE_FOLDER;
	private static File SOURCE_FOLDER;

	@BeforeAll
	public static void initFolder() throws URISyntaxException, IOException {
		RESOURCE_FOLDER = Paths.get(SOURCE.toURI()).toFile();
		SOURCE_FOLDER = new File(testFolder, "src/test/java");
		FileUtils.copyFile(new File(RESOURCE_FOLDER, "pom.xml"), new File(testFolder, "pom.xml"));
	}

	@Test
	public void testJUnit3Transformation() throws IOException {
		final File old = new File(RESOURCE_FOLDER, "TestMe1.java");
		final File testFile = new File(SOURCE_FOLDER, "TestMe1.java");
		FileUtils.copyFile(old, testFile);

		final JUnitTestTransformer tt = new TimeBasedTestTransformer(testFolder);
		tt.determineVersions(Arrays.asList(new File[] {testFolder}));
      tt.transformTests();

		final CompilationUnit cu = JavaParserProvider.parse(testFile);

		final ClassOrInterfaceDeclaration clazz = cu.getClassByName("TestMe1").get();
		Assert.assertNotNull(clazz);

		Assert.assertEquals("TimeBasedTestcase", clazz.getExtendedTypes(0).getName().getIdentifier());

		MatcherAssert.assertThat(clazz.getMethodsByName("getWarmupExecutions"), Matchers.hasSize(0));
		MatcherAssert.assertThat(clazz.getMethodsByName("getExecutionTimes"), Matchers.hasSize(0));
		MatcherAssert.assertThat(clazz.getMethodsByName("getDuration"), Matchers.hasSize(1));
	}

	@Test
	public void testJUnit4Transformation() throws IOException {
		final File old2 = new File(RESOURCE_FOLDER, "TestMe2.java");
		final File testFile2 = new File(SOURCE_FOLDER, "TestMe2.java");
		FileUtils.copyFile(old2, testFile2);

		final JUnitTestTransformer tt = new TimeBasedTestTransformer(testFolder);
		tt.determineVersions(Arrays.asList(new File[] {testFolder}));
      tt.transformTests();

		final CompilationUnit cu = JavaParserProvider.parse(testFile2);

		final ClassOrInterfaceDeclaration clazz = cu.getClassByName("TestMe2").get();
		Assert.assertNotNull(clazz);

		final AnnotationExpr annotation = clazz.getAnnotation(0);
		Assert.assertNotNull(annotation);
		Assert.assertEquals("@RunWith(TimeBasedTestRunner.class)", annotation.toString());

		final List<MethodDeclaration> methodsByName = clazz.getMethodsByName("testMethod1");
		MatcherAssert.assertThat(methodsByName, Matchers.hasSize(1));

		final MethodDeclaration testMethod = methodsByName.get(0);

		final AnnotationExpr performanceTestAnnotation = testMethod.getAnnotationByName("PerformanceTest").get();
		Assert.assertNotNull(performanceTestAnnotation);

		MatcherAssert.assertThat(performanceTestAnnotation.getChildNodes(), TestTransformation.hasAnnotation("duration"));
		MatcherAssert.assertThat(performanceTestAnnotation.getChildNodes(), TestTransformation.hasAnnotation("repetitions"));
		MatcherAssert.assertThat(performanceTestAnnotation.getChildNodes(), Matchers.not(TestTransformation.hasAnnotation("warmupExecutions")));

		for (final Node n : performanceTestAnnotation.getChildNodes()) {
			System.out.println(n);
		}
	}

	@Test
	public void testMe() throws IOException {
		final File old2 = new File(RESOURCE_FOLDER, "TestMe2.java");

		final CompilationUnit unit = JavaParserProvider.parse(old2);

		final ClassOrInterfaceDeclaration clazz = ParseUtil.getClasses(unit).get(0);

		for (final MethodDeclaration method : clazz.getMethods()) {
			for (final Object o : method.getAnnotations()) {
				System.out.println(o.toString() + " " + o.getClass());
			}
			final NormalAnnotationExpr a = new NormalAnnotationExpr();
			a.setName("PerformanceTest");
			a.addPair("executionTimes", "" + 5);
			method.addAnnotation(a);
			for (final Object o : method.getAnnotations()) {
				System.out.println(o.toString() + " " + o.getClass());
			}
			// final String name = "@PerformanceTest";
			// System.out.println(name);
			// method.addAnnotation(name);
		}
	}

	@Test
	public void testJUnit3ExtensionTransformation() {
		// TODO
	}
}
