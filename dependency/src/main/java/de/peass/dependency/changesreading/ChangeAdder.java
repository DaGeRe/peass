package de.peass.dependency.changesreading;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;

import de.peass.dependency.analysis.data.ChangedEntity;

public class ChangeAdder {
   
   private static final Logger LOG = LogManager.getLogger(ChangeAdder.class);
   
   public static void addChange(ClazzChangeData changedata, final Node node, CompilationUnit cu) {
      if (node instanceof Statement || node instanceof Expression) {
         handleStatement(changedata, node);
      } else if (node instanceof ClassOrInterfaceDeclaration) {
         handleClassChange(changedata, (ClassOrInterfaceDeclaration) node);
      } else if (node instanceof ImportDeclaration) {
         handleImportChange(changedata, (ImportDeclaration) node, cu);
      } else {
         handleUnknownChange(changedata, cu);
      }
   }

   public static void handleClassChange(ClazzChangeData changedata, final ClassOrInterfaceDeclaration node) {
      String clazz = ClazzFinder.getContainingClazz(node);
      if (!clazz.isEmpty()) {
         changedata.addClazzChange(clazz);
         changedata.setOnlyMethodChange(false);
      }
   }
   
   public static void handleUnknownChange(ClazzChangeData changedata, final CompilationUnit cu) {
      List<ChangedEntity> entities = ClazzFinder.getClazzEntities(cu);
      for (ChangedEntity entity : entities) {
         changedata.addClazzChange(entity);
      }
   }
   
   private static void handleImportChange(ClazzChangeData changedata, final ImportDeclaration node, CompilationUnit cu) {
      List<ChangedEntity> entities = ClazzFinder.getClazzEntities(cu);
      
      changedata.addImportChange(node.getNameAsString(), entities);
   }

   private static void handleStatement(final ClazzChangeData changedata, final Node statement) {
      Node parent = statement.getParentNode().get();
      boolean finished = false;
      while (!finished && parent.getParentNode() != null && !(parent instanceof CompilationUnit)) {
         if (parent instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) parent;
            final String parameters = getParameters(constructorDeclaration);
            String clazz = ClazzFinder.getContainingClazz(parent);
            changedata.addChange(clazz, "<init>" + parameters);
            finished = true;
         } else if (parent instanceof MethodDeclaration) {
            final MethodDeclaration methodDeclaration = (MethodDeclaration) parent;
            final String parameters = getParameters(methodDeclaration);
            String clazz = ClazzFinder.getContainingClazz(parent);
            changedata.addChange(clazz, methodDeclaration.getNameAsString() + parameters);
            finished = true;
         } else if (parent instanceof InitializerDeclaration) {
            InitializerDeclaration initializerDeclaration = (InitializerDeclaration) parent;
            String clazz = ClazzFinder.getContainingClazz(initializerDeclaration);
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
         changedata.addClazzChange(ClazzFinder.getContainingClazz(statement));
         changedata.setOnlyMethodChange(false);
      }
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
