package de.peass.reading;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.FileComparisonUtil;

public class TestFileComparisonUtil {
   
   @Test
   public void testFindClazz() {
      String test = "class A{ }";
      JavaParser parser = new JavaParser();
      List<Node> parsed = parser.parse(test).getResult().get().getChildNodes();
      ClassOrInterfaceDeclaration clazz = FileComparisonUtil.findClazz(new ChangedEntity("A", ""), parsed);
      Assert.assertNotNull(clazz);
   }
   
   @Test
   public void testFindInnerClazz() {
      String test = "class A{ class B{ } }";
      JavaParser parser = new JavaParser();
      List<Node> parsed = parser.parse(test).getResult().get().getChildNodes();
      ClassOrInterfaceDeclaration clazz = FileComparisonUtil.findClazz(new ChangedEntity("A$B", ""), parsed);
      Assert.assertNotNull(clazz);
   }
   
   @Test
   public void testFindInnerInnerClazz() {
      String test = "class A{ class B{ class C{ } } }";
      JavaParser parser = new JavaParser();
      List<Node> parsed = parser.parse(test).getResult().get().getChildNodes();
      ClassOrInterfaceDeclaration clazz = FileComparisonUtil.findClazz(new ChangedEntity("A$B$C", ""), parsed);
      Assert.assertNotNull(clazz);
   }
}
