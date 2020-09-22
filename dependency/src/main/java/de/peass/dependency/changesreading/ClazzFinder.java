package de.peass.dependency.changesreading;

import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import de.peass.dependency.analysis.data.ChangedEntity;

public class ClazzFinder {
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
}
