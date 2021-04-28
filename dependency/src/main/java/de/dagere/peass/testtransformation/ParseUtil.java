package de.dagere.peass.testtransformation;

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
    */
   public static ClassOrInterfaceDeclaration getClass(final CompilationUnit unit) {
      for (final Node node : unit.getChildNodes()) {
         if (node instanceof ClassOrInterfaceDeclaration) {
            return (ClassOrInterfaceDeclaration) node;
         }
      }
      return null;
   }
   
   public static EnumDeclaration getEnum(final CompilationUnit unit) {
      for (final Node node : unit.getChildNodes()) {
         if (node instanceof EnumDeclaration) {
            return (EnumDeclaration) node;
         }
      }
      return null;
   }
}
