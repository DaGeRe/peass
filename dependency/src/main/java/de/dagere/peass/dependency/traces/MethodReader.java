package de.dagere.peass.dependency.traces;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

public class MethodReader {

   private static final Logger LOG = LogManager.getLogger(MethodReader.class);

   private final ClassOrInterfaceDeclaration clazz;

   public MethodReader(final ClassOrInterfaceDeclaration clazz) {
      this.clazz = clazz;
   }

   public CallableDeclaration<?> getMethod(final Node node, final TraceElementContent currentTraceElement) {
      if (node != null && node.getParentNode().isPresent()) {
         final Node parent = node.getParentNode().get();
         if (node instanceof MethodDeclaration) {
            final MethodDeclaration method = (MethodDeclaration) node;
            if (method.getNameAsString().equals(currentTraceElement.getMethod())) {
               //TODO LOG.trace
               LOG.trace("Parameter: {} Trace-Parameter: {}", method.getParameters().size(), currentTraceElement.getParameterTypes().length);
               LOG.trace(method.getParameters()); //TODO delete
               LOG.trace(Arrays.toString(currentTraceElement.getParameterTypes()));
               if (new ParameterComparator(this.clazz).parametersEqual(currentTraceElement, method)) {
                  if (parent instanceof TypeDeclaration<?>) {
                     final TypeDeclaration<?> clazz = (TypeDeclaration<?>) parent;
                     final String clazzName = clazz.getNameAsString();
                     if (clazzName.equals(currentTraceElement.getSimpleClazz())) {
                        return method;
                     }
                  } else {
                     return method;
                  }
               }
            }
         } else if (node instanceof ConstructorDeclaration) {
            if ("<init>".equals(currentTraceElement.getMethod())) {
               if (parent instanceof TypeDeclaration<?>) {
                  final ConstructorDeclaration constructor = (ConstructorDeclaration) node;
                  final TypeDeclaration<?> clazz = (TypeDeclaration<?>) parent;
                  LOG.trace(clazz.getNameAsString() + " " + currentTraceElement.getClazz());
                  if (clazz.getNameAsString().equals(currentTraceElement.getSimpleClazz())) {
                     if (new ParameterComparator(this.clazz).parametersEqual(currentTraceElement, constructor)) {
                        return (CallableDeclaration<?>) node;
                     }
                  }
               }
               LOG.trace(parent);
            }
         }

         for (final Node child : node.getChildNodes()) {
            final CallableDeclaration<?> possibleMethod = getMethod(child, currentTraceElement);
            if (possibleMethod != null) {
               return possibleMethod;
            }

         }
      }

      return null;
   }

   /**
    * Takes a parameter type (e.g. my.packageDeclaration.MyClass<GenericStuff>) and returns the simple type (e.g. MyClass). Generics can not be considered
    * since they are erased at runtime and therefore not present in traces. 
    * 
    * In general, it would be nice to use FQNs instead of simple types. This would require:
    *    1. Parsing the CompilationUnit for a type declaration (which would mean that the FQN would be package + name by hierarchy in CompilationUnit).
    *    2. Parsing the Imports (can be obtained from the CompilationUnit)
    *    3. Parsing the Declarations in the Package (would require to parse all Files in the package-folder)
    *    4. If none of this applies: package can assumed to be java.lang
    *    
    * Currently, this is not implemented. This results in equal simple class names (e.g. my.package1.MyClass and my.package2.MyClass) to be considered equal.
    * @param traceParameterType
    * @return
    */
   public static String getSimpleType(final String traceParameterType) {
      LOG.trace("Getting simple type of {}", traceParameterType); 
      final String result;
      if (traceParameterType.contains("<")) {
         String withoutGenerics = traceParameterType.substring(0, traceParameterType.indexOf("<"));
         result = withoutGenerics.substring(withoutGenerics.lastIndexOf('.') + 1);
      } else {
         result = traceParameterType.substring(traceParameterType.lastIndexOf('.') + 1);
      }
      LOG.trace("Simple type: {}", result); 
      return result;
   }

   
}
