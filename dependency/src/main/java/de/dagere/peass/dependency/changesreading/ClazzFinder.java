package de.dagere.peass.dependency.changesreading;

import java.util.LinkedList;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;

public class ClazzFinder {
   public static String getContainingClazz(final Node statement) {
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

   public static TypeDeclaration<?> findClazz(final ChangedEntity entity, final List<Node> nodes) {
      TypeDeclaration<?> declaration = null;
      for (final Node node : nodes) {
         if (node instanceof TypeDeclaration<?>) {
            final TypeDeclaration<?> temp = (TypeDeclaration<?>) node;
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
   
   public static List<String> getEntities(final Node node, final String parent, final String clazzSeparator) {
      final List<String> clazzes = new LinkedList<>();
      if (node instanceof ClassOrInterfaceDeclaration) {
         addClazzesOrInterfaces(node, parent, clazzSeparator, clazzes);
      } else if (node instanceof EnumDeclaration) {
         addEnums(node, parent, clazzSeparator, clazzes);
      } else {
         for (final Node child : node.getChildNodes()) {
            clazzes.addAll(getEntities(child, parent, ChangedEntity.CLAZZ_SEPARATOR));
         }
      }
      return clazzes;
   }

   private static void addEnums(final Node node, final String parent, final String clazzSeparator, final List<String> clazzes) {
      final EnumDeclaration enumDecl = (EnumDeclaration) node;
      final String enumName = parent.length() > 0 ? parent + clazzSeparator + enumDecl.getName().getIdentifier() : enumDecl.getName().getIdentifier();
      clazzes.add(enumName);
      for (final Node child : node.getChildNodes()) {
         clazzes.addAll(getEntities(child, enumName, ChangedEntity.CLAZZ_SEPARATOR));
      }
   }

   private static void addClazzesOrInterfaces(final Node node, final String parent, final String clazzSeparator, final List<String> clazzes) {
      final ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) node;
      final String clazzname = parent.length() > 0 ? parent + clazzSeparator + clazz.getName().getIdentifier() : clazz.getName().getIdentifier();
      clazzes.add(clazzname);
      for (final Node child : node.getChildNodes()) {
         clazzes.addAll(getEntities(child, clazzname, ChangedEntity.CLAZZ_SEPARATOR));
      }
   }

   public static List<String> getClazzes(final CompilationUnit cu) {
      final List<String> clazzes = new LinkedList<>();
      for (final Node node : cu.getChildNodes()) {
         clazzes.addAll(getEntities(node, "", "$"));
      }
      return clazzes;
   }

   public static List<ChangedEntity> getClazzEntities(final CompilationUnit cu) {
      List<String> clazzes = ClazzFinder.getClazzes(cu);
      List<ChangedEntity> entities = new LinkedList<>();
      for (String clazz : clazzes) {
         entities.add(new ChangedEntity(clazz, ""));
      }
      return entities;
   }
   
   public static List<ClassOrInterfaceDeclaration> getClazzDeclarations(final Node node) {
      final List<ClassOrInterfaceDeclaration> clazzes = new LinkedList<>();
      if (node instanceof ClassOrInterfaceDeclaration) {
         final ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) node;
         clazzes.add(clazz);
         for (final Node child : node.getChildNodes()) {
            clazzes.addAll(getClazzDeclarations(child));
         }
      } else if (node instanceof EnumDeclaration) {
         for (final Node child : node.getChildNodes()) {
            clazzes.addAll(getClazzDeclarations(child));
         }
      } else {
         for (final Node child : node.getChildNodes()) {
            clazzes.addAll(getClazzDeclarations(child));
         }
      }
      return clazzes;
   }
   
   public static List<ClassOrInterfaceDeclaration> getClazzDeclarations(final CompilationUnit cu) {
      final List<ClassOrInterfaceDeclaration> clazzes = new LinkedList<>();
      for (final Node node : cu.getChildNodes()) {
         clazzes.addAll(getClazzDeclarations(node));
      }
      return clazzes;
   }

}
