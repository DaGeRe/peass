package de.dagere.peass.testtransformation;

import java.util.LinkedList;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;

/**
 * Helper class for parsing Java code with Javaparser.
 * 
 * @author reichelt
 *
 */
public final class ParseUtil {

   /**
    * Prive Constructor - Do not Initialize Helper class.
    */
   private ParseUtil() {

   }

   /**
    * Returns the first ClassOrInterfaceDeclaration of a CompilationUnit ATTENTION: If multiple classes are declared, the first is returned (may be the case if one class/interface
    * is non-public).
    * 
    * @param unit CompilationUnit, which is searched for a class declaration
    * @return Declration if found, else null
    * @deprecated use TraceReadUtils.getNamedClasses instead
    */
   @Deprecated
   public static ClassOrInterfaceDeclaration getClass(final CompilationUnit unit) {
      for (final Node node : unit.getChildNodes()) {
         if (node instanceof ClassOrInterfaceDeclaration) {
            return (ClassOrInterfaceDeclaration) node;
         }
      }
      return null;
   }

   @Deprecated
   public static EnumDeclaration getEnum(final CompilationUnit unit) {
      for (final Node node : unit.getChildNodes()) {
         if (node instanceof EnumDeclaration) {
            return (EnumDeclaration) node;
         }
      }
      return null;
   }

   public static List<ClassOrInterfaceDeclaration> getClasses(final CompilationUnit unit) {
      List<ClassOrInterfaceDeclaration> clazzes = new LinkedList<>();
      for (final Node node : unit.getChildNodes()) {
         if (node instanceof ClassOrInterfaceDeclaration) {
            clazzes.add((ClassOrInterfaceDeclaration) node);
         }
      }
      return clazzes;
   }

   public static List<EnumDeclaration> getEnums(final CompilationUnit unit) {
      List<EnumDeclaration> enums = new LinkedList<>();
      for (final Node node : unit.getChildNodes()) {
         if (node instanceof EnumDeclaration) {
            enums.add((EnumDeclaration) node);
         }
      }
      return enums;
   }
}
