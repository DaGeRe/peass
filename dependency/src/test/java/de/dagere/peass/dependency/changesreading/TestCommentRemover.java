package de.dagere.peass.dependency.changesreading;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;

/**
 * This class tests whether comments can be removed; therefore, it contains some comments itself
 */
public class TestCommentRemover {

   /**
    * This is a test method (and this comment should be removed)
    * 
    * @throws FileNotFoundException
    */
   @Test
   public void removeComments() throws FileNotFoundException {
      File testFile = new File("src/test/java/de/dagere/peass/dependency/changesreading/TestCommentRemover.java");
      CompilationUnit unit = JavaParserProvider.parse(testFile);

      // This comment should be removed
      TypeDeclaration<?> clazz = ClazzFinder.findClazz(new ChangedEntity("de.dagere.peass.dependency.changesreading.TestCommentRemover"), unit.getChildNodes());
      new CommentRemover(clazz);

      Assert.assertFalse(clazz.getComment().isPresent());
      MethodDeclaration method = clazz.getMethods().get(0);
      Assert.assertFalse(method.getComment().isPresent());
      for (Node child : method.getChildNodes()) {
         Assert.assertFalse(child instanceof Comment);
      }
   }
}
