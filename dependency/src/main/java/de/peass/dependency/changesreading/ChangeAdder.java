package de.peass.dependency.changesreading;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;

import de.peass.dependency.ChangeManager;

public class ChangeAdder {
   
   private static final Logger LOG = LogManager.getLogger(ChangeAdder.class);
   
   public static void addChange(ClazzChangeData changedata, final Node node) {
      if (node instanceof Statement || node instanceof Expression) {
         handleStatement(changedata, node);
      } else if (node instanceof ClassOrInterfaceDeclaration) {
         handleClassChange(changedata, node);
      } else if (node instanceof ImportDeclaration) {
         handleImportChange(changedata, node);
      } else {
         handleClassChange(changedata, node);
      }
   }

   public static void handleClassChange(ClazzChangeData changedata, final Node node) {
      String clazz = ClazzFinder.getClazz(node);
      if (!clazz.isEmpty()) {
         changedata.addClazzChange(clazz);
         changedata.setOnlyMethodChange(false);
      }
   }
   
   private static void handleImportChange(ClazzChangeData changedata, final Node node) {
      ImportDeclaration importDeclaration = (ImportDeclaration) node;
      changedata.addImportChange(importDeclaration.getNameAsString());
   }

   private static void handleStatement(final ClazzChangeData changedata, final Node statement) {
      Node parent = statement.getParentNode().get();
      boolean finished = false;
      while (!finished && parent.getParentNode() != null && !(parent instanceof CompilationUnit)) {
         if (parent instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) parent;
            final String parameters = getParameters(constructorDeclaration);
            String clazz = ClazzFinder.getClazz(parent);
            changedata.addChange(clazz, "<init>" + parameters);
            finished = true;
         } else if (parent instanceof MethodDeclaration) {
            final MethodDeclaration methodDeclaration = (MethodDeclaration) parent;
            final String parameters = getParameters(methodDeclaration);
            String clazz = ClazzFinder.getClazz(parent);
            changedata.addChange(clazz, methodDeclaration.getNameAsString() + parameters);
            finished = true;
         } else if (parent instanceof InitializerDeclaration) {
            InitializerDeclaration initializerDeclaration = (InitializerDeclaration) parent;
            String clazz = ClazzFinder.getClazz(initializerDeclaration);
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
         changedata.addClazzChange(ClazzFinder.getClazz(statement));
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
