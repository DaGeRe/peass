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
package de.peass.dependency.changesreading;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.stmt.BlockStmt;

import de.peass.dependency.ClazzFileFinder;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.traces.TraceReadUtils;
import de.peass.dependency.traces.requitur.content.TraceElementContent;

/**
 * Helps to compare whether two versions of a file may have changed performance (and whether this change is for the use of the whole file or only some methods).
 * 
 * @author reichelt
 *
 */
public final class FileComparisonUtil {

   private static final Logger LOG = LogManager.getLogger(FileComparisonUtil.class);

   /**
    * Util class should not be initialized
    */
   private FileComparisonUtil() {

   }

   /**
    * Compares two nodes and returns eventually happened changes
    * 
    * @param node1 First node for comparison
    * @param node2 Second node for comparison
    * @return Eventually empty List of changed Nodes
    */
   public static List<Node> comparePairwise(final Node node1, final Node node2) {
      final List<Node> changes = new LinkedList<>();

      final List<Node> childs1 = cleanUnneccessary(node1.getChildNodes());
      final List<Node> childs2 = cleanUnneccessary(node2.getChildNodes());

      // Test node1.getChildNodes().size() != node2.getChildNodes().size() rausgeschmissen - zählen ja nicht die Kommentare?
      if (childs1.size() != childs2.size()) {
         if (node1 instanceof ClassOrInterfaceDeclaration && node2 instanceof ClassOrInterfaceDeclaration) {
            final List<MethodDeclaration> firstDeclarations = node1.findAll(MethodDeclaration.class);
            final List<MethodDeclaration> secondDeclarations = node2.findAll(MethodDeclaration.class);
            if (firstDeclarations.size() == secondDeclarations.size()) {
               compareNodeList(changes, firstDeclarations, secondDeclarations);
            } else if (firstDeclarations.size() == 0) {
               changes.addAll(firstDeclarations);
            } else if (secondDeclarations.size() == 0) {
               changes.addAll(secondDeclarations);
            } else {
               NodeListComparator nodeListComparator = new NodeListComparator(changes, firstDeclarations, secondDeclarations);
               nodeListComparator.compareUnequalNodeList();
            }
            final List<ConstructorDeclaration> firstDeclarationsConstructor = node1.findAll(ConstructorDeclaration.class);
            final List<ConstructorDeclaration> secondDeclarationsConstructor = node2.findAll(ConstructorDeclaration.class);
            if (firstDeclarationsConstructor.size() == secondDeclarationsConstructor.size()) {
               compareNodeList(changes, firstDeclarationsConstructor, secondDeclarationsConstructor);
            }
         }

         LOG.info("Size of change: " + node1.hashCode() + "(" + node1.getChildNodes().size() + ") " + node2.hashCode() + "(" + node2.getChildNodes().size() + ") ");
         changes.add(node1);
         changes.add(node2);
      } else {
         compareNodeList(changes, childs1, childs2);
      }

      return changes;
   }

   private static void compareNodeList(final List<Node> changes, final List<? extends Node> childs1, final List<? extends Node> childs2) {
      if (childs1.size() != childs2.size()) {
         throw new RuntimeException("Need to pass equal child count!");
      }
      final Iterator<? extends Node> node2iterator = childs2.iterator();
      for (final Iterator<? extends Node> node1iterator = childs1.iterator(); node1iterator.hasNext();) {
         final Node child = node1iterator.next();
         if (!node2iterator.hasNext()) {
            LOG.error("Unexpected!");
         }
         final Node child2 = node2iterator.next();
         if (child.getClass() != child2.getClass()) {
            changes.add(child);
            changes.add(child2);
         } else {

            if (!child.equals(child2)) {
               if (child instanceof BlockStmt || child2 instanceof BlockStmt) {
                  changes.add(child);
                  changes.add(child2);
               } else if (child instanceof ImportDeclaration || child2 instanceof ImportDeclaration) {
                  changes.add(child);
                  changes.add(child2);
               } else {
                  /*
                   * Currently, nothing is done in this case, as a declaration change should cause an call change (which will be an BlockStmt-Change), or the method is not called,
                   * than the declaration change will not be relevant. TODO Is it detected, if the e.g. a public static ClassA asd = new ClassA(1) instead of new ClassA(2)? (cause
                   * this is a declaration as well)
                   */

                  changes.addAll(comparePairwise(child, child2));
               }
            } else {
               LOG.trace("Equal: {} {}", child, child2);
            }
         }
      }
   }

   private static boolean isNodeNotRelevant(final Node testNode) {
      return testNode instanceof LineComment || testNode instanceof JavadocComment;
   }

   private static List<Node> cleanUnneccessary(final List<Node> childs1) {
      final List<Node> result = new LinkedList<>();
      result.addAll(childs1);
      for (final Iterator<Node> iterator = result.iterator(); iterator.hasNext();) {
         final Node testNode = iterator.next();
         if (isNodeNotRelevant(testNode)) {
            iterator.remove();
         }
      }
      return result;
   }

   public static String getMethod(final File projectFolder, final ChangedEntity entity, final String method) throws FileNotFoundException {
      final File file = ClazzFileFinder.getSourceFile(projectFolder, entity);
      if (file != null) {
         LOG.debug("Found:  {} {}", file, file.exists());
         final CompilationUnit cu = JavaParserProvider.parse(file);

         return getMethod(entity, method, cu);
      } else {
         return "";
      }
   }

   public static String getMethod(ChangedEntity entity, String method, CompilationUnit clazzUnit) {
      TraceElementContent traceElement = new TraceElementContent(entity.getJavaClazzName(), 
            method, entity.getModule(), entity.getParameters().toArray(new String[0]), 0);

      final Node node = TraceReadUtils.getMethod(traceElement, clazzUnit);
      if (node != null) {
         return node.toString();
      } else {
         return "";
      }
   }

   /**
    * Returns the information whether the source has changed
    * 
    * @param newFile Old File to check
    * @param oldFile New File to check
    * @return Changedata, i.e. if a change happened and if it was class- or method-wide
    * @throws ParseException If Class can't be parsed
    * @throws IOException If class can't be read
    */
   public static void getChangedMethods(final File newFile, final File oldFile, ClazzChangeData changedata) throws ParseException, IOException {
      try {
         final CompilationUnit newCu = JavaParserProvider.parse(newFile);
         final CompilationUnit oldCu = JavaParserProvider.parse(oldFile);
         
         new CommentRemover(newCu);
         new CommentRemover(oldCu);

         final List<Node> changes = comparePairwise(newCu, oldCu);
         Set<ImportDeclaration> unequalImports = new ImportComparator(newCu.getImports(), oldCu.getImports()).getNotInBoth();
         changes.addAll(unequalImports);

         if (changes.size() == 0) {
            changedata.setChange(false);
            return;
         }

         boolean onlyLineCommentChanges = checkOnlyLineCommentChange(changes);
         if (onlyLineCommentChanges) {
            return;
         }

         for (final Node node : changes) {
            // Use old CU for change adding - if new CU contains more clazzes, these will be regarded as changes anyway
            ChangeAdder.addChange(changedata, node, oldCu);
         }
      } catch (final Exception e) {
         e.printStackTrace();
         LOG.info("Found full-class change");
         changedata.setOnlyMethodChange(false);
         throw new ParseException("Parsing was not successfull");
      }

      return;
   }
   
   private static boolean checkOnlyLineCommentChange(final List<Node> changes) {
      boolean onlyLineCommentOrImportChanges = true;
      for (final Node node : changes) {
         if (!(node instanceof LineComment)) {
            onlyLineCommentOrImportChanges = false;
         }
      }
      return onlyLineCommentOrImportChanges;
   }
}
