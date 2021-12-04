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
package de.dagere.peass.reading;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ParseException;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.changesreading.FileComparisonUtil;

/**
 * Tests whether the class comparison works for the given files, i.e. returns whether the class and its method have changed correct.
 * 
 * @author reichelt
 *
 */
public class TestClassComparison {
   private static final URL SOURCE = Thread.currentThread().getContextClassLoader().getResource("comparison");
   private static File FOLDER;

   @BeforeAll
   public static void initFolder() throws URISyntaxException {
      FOLDER = Paths.get(SOURCE.toURI()).toFile();
   }

   @Test
   public void test1() throws ParseException, IOException, URISyntaxException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test1_2_Equal.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertFalse(changedMethods.isChange());
   }

   @Test
   public void test2() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test2_2_Comments.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertFalse(changedMethods.isChange());
   }

   @Test
   public void test3() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test3_2_MethodComments.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertFalse(changedMethods.isChange());
   }

   @Test
   public void test4() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test4_2_Formatting.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertFalse(changedMethods.isChange());
   }

   @Test
   public void test5() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test5_2_Imports.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertTrue(changedMethods.isChange());
   }

   @Test
   public void test6() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test6_MethodChanged.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertTrue(changedMethods.isChange());
      Assert.assertTrue(changedMethods.isOnlyMethodChange());

      System.out.println(changedMethods.getChangedMethods());

      MatcherAssert.assertThat(changedMethods.getChangedMethods().values().iterator().next(), Matchers.hasItem("doNonStaticThing"));
      MatcherAssert.assertThat(changedMethods.getChangedMethods().values().iterator().next(), Matchers.hasItem("<init>"));
   }

   @Test
   public void test7() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test7_MethodAdded.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertTrue(changedMethods.isChange());
      Assert.assertFalse(changedMethods.isOnlyMethodChange());
   }

   @Test
   public void test8() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test8_2_LineComments.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertFalse(changedMethods.isChange());
   }

   @Test
   public void test9() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test9_FieldAdded.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertTrue(changedMethods.isChange());
   }

   @Test
   public void testStaticFieldAdded() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test10_StaticFieldAdded.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertTrue(changedMethods.isChange());
   }
   
   @Test
   public void testStaticFieldChanged() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test10_StaticFieldAdded.java");
      final File file2 = new File(FOLDER, "Test10_StaticFieldChanged.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertTrue(changedMethods.isChange());
   }
   
   @Test
   public void testParameterizedChanged() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test13_Parameter.java");
      final File file2 = new File(FOLDER, "Test13_Parameter_changed.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("myPackage.Test", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertTrue(changedMethods.isChange());
      ChangedEntity change = changedMethods.getChanges().iterator().next();
      Assert.assertEquals("myPackage.Test#doStaticThing(java.lang.String,java.lang.Object)", change.toString());
   }

   @Test
   public void testChangeAndAddition1() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test11_ChangeAndAddition.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertTrue(changedMethods.isChange());
      Assert.assertFalse(changedMethods.isOnlyMethodChange());
      Map<String, Set<String>> changedMethods2 = changedMethods.getChangedMethods();
      MatcherAssert.assertThat(changedMethods2.values().iterator().next(), Matchers.contains("<init>"));

   }

   @Test
   public void testChangeAndAddition2() throws ParseException, IOException {
      final File file1 = new File(FOLDER, "Test1_1_Equal.java");
      final File file2 = new File(FOLDER, "Test12_ChangeAndAddition2.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("Test1_1_Equal", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertTrue(changedMethods.isChange());
      Assert.assertFalse(changedMethods.isOnlyMethodChange());
      System.out.println(changedMethods.getChangedMethods());
      MatcherAssert.assertThat(changedMethods.getChangedMethods().values().iterator().next(), Matchers.containsInAnyOrder("<init>", "doNonStaticThing"));
   }
   
   @Test
   public void testImport() throws ParseException, IOException, URISyntaxException {
      final File file1 = new File(FOLDER, "TestImport_New.java");
      final File file2 = new File(FOLDER, "TestImport_Old.java");

      final ClazzChangeData changedMethods = new ClazzChangeData(new ChangedEntity("de.TestImport_new", ""));
      FileComparisonUtil.getChangedMethods(file1, file2, changedMethods);

      Assert.assertTrue(changedMethods.isChange());
      MatcherAssert.assertThat(changedMethods.getChanges(), Matchers.hasItem(new ChangedEntity("de.Test", "")));
      MatcherAssert.assertThat(changedMethods.getChanges(), Matchers.hasItem(new ChangedEntity("de.Test$InnerTest", "")));
   }
}
