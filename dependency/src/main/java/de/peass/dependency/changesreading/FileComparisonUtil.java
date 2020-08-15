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
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import de.peass.dependency.ClazzFinder;
import de.peass.dependency.analysis.data.ChangedEntity;

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

   public static void clearComments(final Node node) {
      final Optional<Comment> comment = node.getComment();
      if (comment.isPresent()) {
         final Comment realComment = comment.get();
         if (realComment.getComment() != null) {
            realComment.setContent("");
         }
         node.setComment(null);
      }
      node.getOrphanComments().clear();
      final List<Node> childNodes = node.getChildNodes();
      LOG.trace("Type: {}", childNodes.getClass());
      for (final Iterator<Node> begin = childNodes.iterator(); begin.hasNext();) {
         final Node child = begin.next();
         if (child instanceof LineComment) {
            ((LineComment) child).setContent("");
         } else if (child instanceof JavadocComment) {
            ((JavadocComment) child).setContent("");
         } else {
            clearComments(child);
         }
      }
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
               }
               if (child instanceof ImportDeclaration || child2 instanceof ImportDeclaration) {
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
      return testNode instanceof ImportDeclaration || testNode instanceof LineComment || testNode instanceof JavadocComment;
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

   private final static ThreadLocal<JavaParser> javaParser = new ThreadLocal<JavaParser>() {
      protected JavaParser initialValue() {
         return new JavaParser();
      };
   };

   public synchronized static CompilationUnit parse(final File file) throws FileNotFoundException {
      final JavaParser parser = javaParser.get();
      final Optional<CompilationUnit> result = parser.parse(file).getResult();
      return result.get();
   }

   public static String getMethod(final File projectFolder, final ChangedEntity entity, final String method) throws FileNotFoundException {
      final File file = ClazzFinder.getSourceFile(projectFolder, entity);
      if (file != null) {
         LOG.debug("Found:  {} {}", file, file.exists());

         final CompilationUnit newCu = parse(file);
         return getMethod(entity, method, newCu);
      } else {
         return "";
      }
   }

   public static String getMethod(final ChangedEntity entity, final String method, final CompilationUnit newCu) {
      ClassOrInterfaceDeclaration declaration = findClazz(entity, newCu.getChildNodes());
      if (declaration == null) {
         return "";
      }
      if (method.contains(ChangedEntity.CLAZZ_SEPARATOR)) {
         final String outerClazz = method.substring(0, method.indexOf(ChangedEntity.CLAZZ_SEPARATOR));
         LOG.debug("Searching: " + outerClazz + " " + method);
         declaration = findClazz(new ChangedEntity(outerClazz, ""), declaration.getChildNodes());
         LOG.debug("Suche: " + outerClazz + " " + declaration);
      }
      if (declaration == null) {
         return "";
      }

      if (method.equals("<init>")) {
         final List<ConstructorDeclaration> methods = declaration.getConstructors();
         System.out.println("Searching: " + method + " " + entity);
         if (methods.size() > 0) {
            CallableDeclaration<?> callable = findCallable(entity, methods);
            if (callable == null) {
               LOG.debug("Full parameters of not-found entity: " + entity.getParameters());
               entity.getParameters().remove(0); // Remove instance of surrounding class
               callable = findCallable(entity, methods);
               System.out.println("Found: " + callable);
            }
            callable.setComment(null);
            return callable.toString();
         } else {
            return "";
         }
      } else {
         List<MethodDeclaration> methods;
         if (method.contains(ChangedEntity.CLAZZ_SEPARATOR)) {
            final String methodName = method.substring(method.indexOf(ChangedEntity.CLAZZ_SEPARATOR) + 1);
            System.out.println("Suche: " + methodName + " " + method);
            methods = declaration.getMethodsByName(methodName);
         } else {
            methods = declaration.getMethodsByName(method);
         }
         if (methods.size() > 0) {
            CallableDeclaration<?> foundDeclaration = findCallable(entity, methods);
            if (foundDeclaration != null) {
               foundDeclaration.setComment(null);
               return foundDeclaration.toString();
            }
         }
         return "";
      }
   }

   private static CallableDeclaration<?> findCallable(final ChangedEntity entity, List<? extends CallableDeclaration<?>> methods) {
      CallableDeclaration<?> foundDeclaration = null;
      for (CallableDeclaration<?> methodDec : methods) {
         if (methodDec.getParameters().size() == entity.getParameters().size()) {
            boolean equal = parametersEqual(entity, methodDec);
            if (equal) {
               foundDeclaration = methodDec;
               break;

            }
         }
      }
      return foundDeclaration;
   }

   private static boolean parametersEqual(final ChangedEntity entity, CallableDeclaration<?> methodDec) {
      boolean equal = true;
      for (int i = 0; i < methodDec.getParameters().size(); i++) {
         String declaredParameterName = methodDec.getParameter(i).getTypeAsString();
         final String entityParameterName = entity.getParameters().get(i);
         if (entityParameterName.contains("$")) {
            String shortName = entityParameterName.substring(entityParameterName.indexOf('$') + 1);
            if (!declaredParameterName.equals(entityParameterName) && !declaredParameterName.equals(shortName)) {
               equal = false;
            }
         } else {
            if (!declaredParameterName.equals(entityParameterName)) {
               equal = false;
            }
         }
      }
      return equal;
   }

   public static ClassOrInterfaceDeclaration findClazz(final ChangedEntity entity, final List<Node> nodes) {
      ClassOrInterfaceDeclaration declaration = null;
      for (final Node node : nodes) {
         if (node instanceof ClassOrInterfaceDeclaration) {
            final ClassOrInterfaceDeclaration temp = (ClassOrInterfaceDeclaration) node;
            final String nameAsString = temp.getNameAsString();
            if (nameAsString.equals(entity.getSimpleClazzName())) {
               declaration = (ClassOrInterfaceDeclaration) node;
               break;
            } else {
               if (entity.getSimpleClazzName().startsWith(nameAsString + ChangedEntity.CLAZZ_SEPARATOR)) {
                  ChangedEntity inner = new ChangedEntity(entity.getSimpleClazzName().substring(nameAsString.length() + 1), entity.getModule());
                  declaration = findClazz(inner, node.getChildNodes());
               }
            }
         }
      }
      return declaration;
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
      final CompilationUnit newCu = parse(newFile);
      final CompilationUnit oldCu = parse(oldFile);
      try {
         clearComments(newCu);
         clearComments(oldCu);

         final List<Node> changes = comparePairwise(newCu, oldCu);
         Set<ImportDeclaration> unequalImports = new ImportComparator(newCu.getImports(), oldCu.getImports()).getNotInBoth();
         changes.addAll(unequalImports);

         if (changes.size() == 0) {
            changedata.setChange(false);
            return;
         }

         boolean onlyLineCommentOrImportChanges = true;
         for (final Node node : changes) {
            if (!(node instanceof LineComment) && !(node instanceof ImportDeclaration)) {
               onlyLineCommentOrImportChanges = false;
            }
         }
         if (onlyLineCommentOrImportChanges) {
            return;
         }

         for (final Node node : changes) {
            if (node instanceof Statement || node instanceof Expression) {
               handleStatement(changedata, node);
            } else if (node instanceof ClassOrInterfaceDeclaration) {
               handleClassChange(changedata, node);
            } else {
               handleClassChange(changedata, node);
            }
         }
      } catch (final Exception e) {
         e.printStackTrace();
         LOG.info("Found full-class change");
         changedata.setOnlyMethodChange(false);
         throw new ParseException("Parsing was not successfull");
      }

      return;
   }

   public static void handleClassChange(ClazzChangeData changedata, final Node node) {
      String clazz = getClazz(node);
      if (!clazz.isEmpty()) {
         changedata.addClazzChange(clazz);
         changedata.setOnlyMethodChange(false);
      }
   }

   private static void handleStatement(final ClazzChangeData changedata, final Node statement) {
      Node parent = statement.getParentNode().get();
      boolean finished = false;
      while (!finished && parent.getParentNode() != null && !(parent instanceof CompilationUnit)) {
         if (parent instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) parent;
            final String parameters = getParameters(constructorDeclaration);
            String clazz = getClazz(parent);
            changedata.addChange(clazz, "<init>" + parameters);
            finished = true;
         } else if (parent instanceof MethodDeclaration) {
            final MethodDeclaration methodDeclaration = (MethodDeclaration) parent;
            final String parameters = getParameters(methodDeclaration);
            String clazz = getClazz(parent);
            changedata.addChange(clazz, methodDeclaration.getNameAsString() + parameters);
            finished = true;
         } else if (parent instanceof InitializerDeclaration) {
            InitializerDeclaration initializerDeclaration = (InitializerDeclaration) parent;
            String clazz = getClazz(initializerDeclaration);
            changedata.addChange(clazz, "<init>");
            finished = true;
         }
         final Optional<Node> newParent = parent.getParentNode();
         if (!newParent.isPresent()) {
            System.out.println("No Parent: " + newParent);
         }
         parent = newParent.get();
      }
      if (!finished) {
         LOG.debug("No containing method found!");
         changedata.addClazzChange(getClazz(statement));
         changedata.setOnlyMethodChange(false);
      }
   }

   public static String getClazz(Node statement) {
      String clazz = "";
      Node current = statement;
      while (current.getParentNode().isPresent()) {
         if (current instanceof ClassOrInterfaceDeclaration || current instanceof EnumDeclaration || current instanceof AnnotationDeclaration) {
            TypeDeclaration<?> declaration = (TypeDeclaration<?>) current;
            String name = declaration.getNameAsString();
            if (!clazz.isEmpty()) {
               clazz = name + "$" + clazz;
            } else {
               clazz = name;
            }
         }
         current = current.getParentNode().get();

      }
      return clazz;
   }

   private static String getParameters(final CallableDeclaration<?> callable) {
      NodeList<Parameter> parameterDeclaration = callable.getParameters();
      String parameters;
      if (parameterDeclaration.size() > 0) {
         parameters = "(";
         for (final Parameter parameter : parameterDeclaration) {
            String type;
            if (parameter.getTypeAsString().contains("<")) {
               type = parameter.getTypeAsString().substring(0, parameter.getTypeAsString().indexOf("<"));
            } else {
               type = parameter.getTypeAsString();
            }
            if (parameter.isVarArgs()) {
               parameters += type + "[]" + ",";
            } else {
               parameters += type + ",";
            }

         }
         parameters = parameters.substring(0, parameters.length() - 1) + ")";
      } else {
         parameters = "";
      }
      return parameters;
   }
}
