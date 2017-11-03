
package de.peran;

/*-
 * #%L
 * peran-dependency
 * %%
 * Copyright (C) 2017 Hanns-Seidel-Stiftung
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.javaparser.ParseException;

import de.peran.dependency.analysis.FileComparisonUtil;
import de.peran.dependency.analysis.data.ClazzChangeData;

/**
 * Tests whether the class comparison works for the given files, i.e. returns whether the class and its method have changed correct.
 * @author reichelt
 *
 */
public class TestClassComparison {
	private static final URL SOURCE = Thread.currentThread().getContextClassLoader().getResource("comparison");
	private static File FOLDER;
	
	@BeforeClass
	public static void initFolder() throws URISyntaxException{
		FOLDER =  Paths.get(SOURCE.toURI()).toFile();
	}
	
	
	@Test
	public void test1() throws ParseException, IOException, URISyntaxException{
		final File file1 = new File(FOLDER, "Test1_1_Equal.java");
		final File file2 = new File(FOLDER, "Test1_2_Equal.java");
		
		final ClazzChangeData changedMethods = FileComparisonUtil.getChangedMethods(file1, file2);
		
		Assert.assertFalse(changedMethods.isChange());
	}
	
	@Test
	public void test2() throws ParseException, IOException{
		final File file1 = new File(FOLDER, "Test1_1_Equal.java");
		final File file2 = new File(FOLDER, "Test2_2_Comments.java");
		
		final ClazzChangeData changedMethods = FileComparisonUtil.getChangedMethods(file1, file2);
		
		Assert.assertFalse(changedMethods.isChange());
	}
	
	@Test
	public void test3() throws ParseException, IOException{
		final File file1 = new File(FOLDER, "Test1_1_Equal.java");
		final File file2 = new File(FOLDER, "Test3_2_MethodComments.java");
		
		final ClazzChangeData changedMethods = FileComparisonUtil.getChangedMethods(file1, file2);
		
		Assert.assertFalse(changedMethods.isChange());
	}
	
	@Test
	public void test4() throws ParseException, IOException{
		final File file1 = new File(FOLDER, "Test1_1_Equal.java");
		final File file2 = new File(FOLDER, "Test4_2_Formatting.java");
		
		final ClazzChangeData changedMethods = FileComparisonUtil.getChangedMethods(file1, file2);
		
		Assert.assertFalse(changedMethods.isChange());
	}
	
	@Test
	public void test5() throws ParseException, IOException{
		final File file1 = new File(FOLDER, "Test1_1_Equal.java");
		final File file2 = new File(FOLDER, "Test5_2_Imports.java");
		
		final ClazzChangeData changedMethods = FileComparisonUtil.getChangedMethods(file1, file2);
		
		Assert.assertFalse(changedMethods.isChange());
	}
	
	@Test
	public void test6() throws ParseException, IOException{
		final File file1 = new File(FOLDER, "Test1_1_Equal.java");
		final File file2 = new File(FOLDER, "Test6_MethodChanged.java");
		
		final ClazzChangeData changedMethods = FileComparisonUtil.getChangedMethods(file1, file2);
		
		Assert.assertTrue(changedMethods.isChange());
		Assert.assertTrue(changedMethods.isOnlyMethodChange());
		
		System.out.println(changedMethods.getChangedMethods());
		
		Assert.assertThat(changedMethods.getChangedMethods(), Matchers.hasItem("doNonStaticThing"));
		Assert.assertThat(changedMethods.getChangedMethods(), Matchers.hasItem("<init>"));
	}
	
	@Test
	public void test7() throws ParseException, IOException{
		final File file1 = new File(FOLDER, "Test1_1_Equal.java");
		final File file2 = new File(FOLDER, "Test7_MethodAdded.java");
		
		final ClazzChangeData changedMethods = FileComparisonUtil.getChangedMethods(file1, file2);
		
		Assert.assertTrue(changedMethods.isChange());
		Assert.assertFalse(changedMethods.isOnlyMethodChange());
	}
	
	@Test
	public void test8() throws ParseException, IOException{
		final File file1 = new File(FOLDER, "Test1_1_Equal.java");
		final File file2 = new File(FOLDER, "Test8_2_LineComments.java");
		
		final ClazzChangeData changedMethods = FileComparisonUtil.getChangedMethods(file1, file2);
		
		Assert.assertFalse(changedMethods.isChange());
	}
	
	@Test
	public void test9() throws ParseException, IOException{
		final File file1 = new File(FOLDER, "Test1_1_Equal.java");
		final File file2 = new File(FOLDER, "Test9_FieldAdded.java");
		
		final ClazzChangeData changedMethods = FileComparisonUtil.getChangedMethods(file1, file2);
		
		Assert.assertTrue(changedMethods.isChange());
	}
	
	@Test
	public void test10() throws ParseException, IOException{
		final File file1 = new File(FOLDER, "Test1_1_Equal.java");
		final File file2 = new File(FOLDER, "Test10_StaticFieldChanged.java");
		
		final ClazzChangeData changedMethods = FileComparisonUtil.getChangedMethods(file1, file2);
		
		Assert.assertTrue(changedMethods.isChange());
	}
}
