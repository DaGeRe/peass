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

import de.dagere.peass.dependency.traces.requitur.content.TraceElementContent;

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
               LOG.debug("Parameter: {} Trace-Parameter: {}", method.getParameters().size(), currentTraceElement.getParameterTypes().length);
               LOG.debug(method.getParameters()); //TODO delete
               LOG.debug(Arrays.toString(currentTraceElement.getParameterTypes()));
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
                        return (CallableDeclaration) node;
                     }
                  }
               }
               LOG.trace(parent);
            }
         }

         for (final Node child : node.getChildNodes()) {
            final CallableDeclaration possibleMethod = getMethod(child, currentTraceElement);
            if (possibleMethod != null) {
               return possibleMethod;
            }

         }
      }

      return null;
   }

   public static String getSimpleType(final String traceParameterType) {
      LOG.debug("Getting simple type of {}", traceParameterType); //TODO delete
      final String result;
      if (traceParameterType.contains("<")) {
         String withoutGenerics = traceParameterType.substring(0, traceParameterType.indexOf("<"));
         result = withoutGenerics.substring(withoutGenerics.lastIndexOf('.') + 1);
      } else {
         result = traceParameterType.substring(traceParameterType.lastIndexOf('.') + 1);
      }
      LOG.debug("Simple type: {}", result); //TODO delete
      return result;
   }

   
}
