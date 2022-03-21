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
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.PrimitiveType;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;


/**
 * Transforms JUnit-Tests to timebased performance tests.
 * 
 * @author reichelt
 */
public class TimeBasedTestTransformer extends JUnitTestTransformer {

	private static final int DEFAULT_DURATION = 60000;

	private static final Logger LOG = LogManager.getLogger(TimeBasedTestTransformer.class);

	private int duration = DEFAULT_DURATION;

	/**
	 * Creates Transformer with path.
	 * @param path Path where to transform the tests
	 */
	public TimeBasedTestTransformer(final File path) {
		super(path, MeasurementConfig.DEFAULT);
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(final int duration) {
		this.duration = duration;
	}

	/**
	 * Edits Java so that the class extends KoPeMeTestcase instead of TestCase
	 * and that the methods for specifying the performance test are added. It is
	 * assumed that every class is in it's original state, i.e. no
	 * KoPeMeTestcase-changes have been made yet. Classes, that already extend
	 * KoPeMeTestcase are not changed.
	 * 
	 * @param javaFile File to edit
	 */
	@Override
   protected void editJUnit3(final File javaFile) {
		try {
			final CompilationUnit unit = JavaParserProvider.parse(javaFile);
			unit.addImport("de.dagere.kopeme.junit3.TimeBasedTestcase");

			for (ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
			   if (clazz.getExtendedTypes(0).getNameAsString().equals("TestCase")) {
	            clazz.setExtendedTypes(new NodeList<>());
	            clazz.addExtendedType("TimeBasedTestcase");
	         }

	         addMethod(clazz, "logFullData", "return " + config.isLogFullData() + ";", PrimitiveType.booleanType());
	         addMethod(clazz, "useKieker", "return " + config.isUseKieker() + ";", PrimitiveType.booleanType());
	         addMethod(clazz, "getDuration", "return " + duration + ";", PrimitiveType.longType());
	         addMethod(clazz, "getMaximalTime", "return " + (duration * 2) + ";", PrimitiveType.longType());
	         addMethod(clazz, "getRepetitions", "return " + config.getRepetitions() + ";", PrimitiveType.intType());
			}

			Files.write(javaFile.toPath(), unit.toString().getBytes(charset));
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Edits Java so that the class is run with the KoPeMe-Testrunner and the
	 * methods are annotated additionally with @PerformanceTest.
	 * 
	 * @param javaFile File to edit
	 */
	@Override
   protected void editJUnit4(final File javaFile) {
		try {
			final CompilationUnit unit = JavaParserProvider.parse(javaFile);

			unit.addImport("de.dagere.kopeme.annotations.Assertion");
			unit.addImport("de.dagere.kopeme.annotations.MaximalRelativeStandardDeviation");
			unit.addImport("de.dagere.kopeme.annotations.PerformanceTest");
			unit.addImport("de.dagere.kopeme.junit.testrunner.time.TimeBasedTestRunner");
			unit.addImport("org.junit.runner.RunWith");

			
			for (final ClassOrInterfaceDeclaration clazz : ParseUtil.getClasses(unit)) {
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
	            performanceTestAnnotation.setName("PerformanceTest");
	            performanceTestAnnotation.addPair("duration", "" + duration);
	            performanceTestAnnotation.addPair("logFullData", "" + true);
	            performanceTestAnnotation.addPair("timeout", "" + duration * 2);
	            performanceTestAnnotation.addPair("repetitions", "" + config.getRepetitions());
	            method.addAnnotation(performanceTestAnnotation);
	         }

	         final SingleMemberAnnotationExpr annotation = new SingleMemberAnnotationExpr();
	         annotation.setName("RunWith");
	         final ClassExpr clazzExpression = new ClassExpr();
	         clazzExpression.setType("TimeBasedTestRunner");
	         annotation.setMemberValue(clazzExpression);
	         clazz.addAnnotation(annotation);
			}
			
			Files.write(javaFile.toPath(), unit.toString().getBytes(charset));
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
