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
package de.peran.testtransformation;

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
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

import de.dagere.kopeme.datacollection.DataCollectorList;

/**
 * Transforms JUnit-Tests to performance tests.
 * 
 * @author reichelt
 *
 */
public class JUnitTestTransformer {

	private static final int DEFAULT_EXECUTIONS = 10;
	private static final int DEFAULT_TIMEOUT = 30 * 60 * 1000;

	private static final Logger LOG = LogManager.getLogger(JUnitTestTransformer.class);

	protected DataCollectorList datacollectorlist;
	protected int warmupExecutions, executions;
	protected int sumTime;
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
		executions = DEFAULT_EXECUTIONS;
		warmupExecutions = DEFAULT_EXECUTIONS;
		sumTime = DEFAULT_TIMEOUT;
	}

	public boolean isUseKieker() {
		return useKieker;
	}

	public void setUseKieker(boolean useKieker) {
		this.useKieker = useKieker;
	}

	public int getRepetitions() {
		return repetitions;
	}

	public void setRepetitions(final int repetitions) {
		this.repetitions = repetitions;
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

		final File testFolder = new File(projectFolder, "src/test/");

		if (testFolder.exists()) {
			final Map<File, Integer> junitVersions = determineVersions(testFolder);
			for (final Map.Entry<File, Integer> version : junitVersions.entrySet()) {
				if (version.getValue() == 3) {
					editJUnit3(version.getKey());
				} else if (version.getValue() == 4 || version.getValue() == 34) {
					editJUnit4(version.getKey());
				}
			}
		} else {
			LOG.error("Test folder " + testFolder.getAbsolutePath() + " does not exist.");
		}
	}

	private Map<File, Integer> determineVersions(final File testFolder) {
		final Map<File, Integer> junitVersions = new HashMap<>();
		final Map<String, List<File>> extensions = new HashMap<>();
		for (final File javaFile : FileUtils.listFiles(testFolder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE)) {
			try {
				final CompilationUnit unit = JavaParser.parse(javaFile);
				final boolean isJUnit4 = isJUnit4(unit);
				if (isJUnit4) {
					junitVersions.put(javaFile, 4);
				}
				final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);
				if (clazz != null) { // for @interface cases
					// We only need to consider classes with one extends, since classes can not have multiple extends and we search for classes that may extend TestCase (indirectly)
					LOG.trace("Transforming: {}", clazz.getNameAsString());
					if (clazz.getExtendedTypes().size() == 1) {
						final ClassOrInterfaceType extend = clazz.getExtendedTypes(0);
						List<File> extensionsOfBase = extensions.get(extend.getNameAsString().intern());
						if (extensionsOfBase == null) {
							extensionsOfBase = new LinkedList<>();
							extensions.put(extend.getNameAsString().intern(), extensionsOfBase);
						}
						extensionsOfBase.add(javaFile);
					}
				}
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		addJUnit3Test("TestCase", extensions, junitVersions);
		return junitVersions;
	}

	private boolean isJUnit4(final CompilationUnit unit) {
		boolean isJUnit4 = false;
		for (final ImportDeclaration currentImport : unit.getImports()) {
			final Name importName = currentImport.getName();
			if (importName.toString().equals("org.junit.Test")) {
				isJUnit4 = true;
			}
		}
		return isJUnit4;
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

	/**
	 * Edits Java so that the class extends KoPeMeTestcase instead of TestCase and that the methods for specifying the performance test are added. It is assumed that every class is in it's original
	 * state, i.e. no KoPeMeTestcase-changes have been made yet. Classes, that already extend KoPeMeTestcase are not changed.
	 * 
	 * @param javaFile File for editing
	 */
	protected void editJUnit3(final File javaFile) {
		try {
			final CompilationUnit unit = JavaParser.parse(javaFile);
			unit.addImport("de.dagere.kopeme.junit3.KoPeMeTestcase");
			unit.addImport("de.dagere.kopeme.datacollection.DataCollectorList");

			final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);

			if (clazz.getExtendedTypes(0).getNameAsString().equals("TestCase")) {
				clazz.setExtendedTypes(new NodeList<>());
				clazz.addExtendedType("KoPeMeTestcase");
			}

			addMethod(clazz, "getWarmupExecutions", "return " + warmupExecutions + ";", PrimitiveType.intType());
			addMethod(clazz, "getExecutionTimes", "return " + executions + ";", PrimitiveType.intType());
			addMethod(clazz, "logFullData", "return " + logFullData + ";", PrimitiveType.booleanType());
			addMethod(clazz, "useKieker", "return " + useKieker + ";", PrimitiveType.booleanType());
			addMethod(clazz, "getMaximalTime", "return " + sumTime + ";", PrimitiveType.longType());
			addMethod(clazz, "getRepetitions", "return " + repetitions + ";", PrimitiveType.intType());

			if (datacollectorlist.equals(DataCollectorList.ONLYTIME)) {
				addMethod(clazz, "getDataCollectors", "return DataCollectorList.ONLYTIME;", JavaParser.parseClassOrInterfaceType("DataCollectorList"));
			}

			Files.write(javaFile.toPath(), unit.toString().getBytes(charset));
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds the given method to the Classdeclaration
	 * @param clazz	Clazz where method should be added
	 * @param name	Name of the new method
	 * @param source Source of the new method
	 * @param type Returntype of the new method
	 */
	protected void addMethod(final ClassOrInterfaceDeclaration clazz, final String name, final String source, final Type type) {
		final MethodDeclaration addedMethod = clazz.addMethod(name, Modifier.PUBLIC);
		addedMethod.setType(type);

		final BlockStmt statement = new BlockStmt();
		statement.addStatement(source);

		addedMethod.setBody(statement);
	}

	/**
	 * Edits Java so that the class is run with the KoPeMe-Testrunner and the methods are annotated additionally with @PerformanceTest.
	 * 
	 * @param javaFile File for editing
	 */
	protected void editJUnit4(final File javaFile) {
		try {
			final CompilationUnit unit = JavaParser.parse(javaFile);

			unit.addImport("de.dagere.kopeme.annotations.Assertion");
			unit.addImport("de.dagere.kopeme.annotations.MaximalRelativeStandardDeviation");
			// unit.addImport("de.dagere.kopeme.annotations.PerformanceTest");
			unit.addImport("de.dagere.kopeme.junit.testrunner.PerformanceTestRunnerJUnit");
			unit.addImport("org.junit.runner.RunWith");

			final ClassOrInterfaceDeclaration clazz = ParseUtil.getClass(unit);
			if (clazz.getAnnotations().size() > 0) {
				boolean otherTestRunner = false;
				for (final AnnotationExpr annotation : clazz.getAnnotations()) {
					if (annotation.getNameAsString().contains("RunWith")) {
						otherTestRunner = true;
					}
				}
				if (otherTestRunner) {
					return;
				}
			}

			for (final MethodDeclaration method : clazz.getMethods()) {
				final NormalAnnotationExpr performanceTestAnnotation = new NormalAnnotationExpr();
				performanceTestAnnotation.setName("de.dagere.kopeme.annotations.PerformanceTest");
				performanceTestAnnotation.addPair("executionTimes", "" + executions);
				performanceTestAnnotation.addPair("warmupExecutions", "" + warmupExecutions);
				performanceTestAnnotation.addPair("logFullData", "" + true);
				performanceTestAnnotation.addPair("timeout", "" + sumTime);
				performanceTestAnnotation.addPair("repetitions", "" + repetitions);
				method.addAnnotation(performanceTestAnnotation);
			}

			final SingleMemberAnnotationExpr annotation = new SingleMemberAnnotationExpr();
			annotation.setName("RunWith");
			final ClassExpr clazzExpression = new ClassExpr();
			clazzExpression.setType("PerformanceTestRunnerJUnit");
			annotation.setMemberValue(clazzExpression);
			clazz.addAnnotation(annotation);
			Files.write(javaFile.toPath(), unit.toString().getBytes(charset));
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
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
		this.executions = iterations;
	}

	public int getExecutions() {
		return executions;
	}

	public void setWarmupExecutions(final int warmup) {
		this.warmupExecutions = warmup;
	}

	public int getWarmupExecutions() {
		return warmupExecutions;
	}

	public int getSumTime() {
		return sumTime;
	}

	public void setSumTime(final int sumTime) {
		this.sumTime = sumTime;
	}

	public File getProjectFolder() {
		return projectFolder;
	}

	public void setEncoding(final Charset encoding) {
		charset = encoding;
	}

}
