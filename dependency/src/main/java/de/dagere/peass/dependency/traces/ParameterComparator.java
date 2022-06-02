package de.dagere.peass.dependency.traces;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;

public class ParameterComparator {

   private static final Logger LOG = LogManager.getLogger(ParameterComparator.class);

   private final ClassOrInterfaceDeclaration clazz;

   public ParameterComparator(final ClassOrInterfaceDeclaration clazz) {
      this.clazz = clazz;
   }

   public boolean parametersEqual(final TraceElementContent traceElement, final CallableDeclaration<?> method) {
      if (traceElement.getParameterTypes().length == 0 && method.getParameters().size() == 0) {
         return true;
      } else if (method.getParameters().size() == 0 && !method.isConstructorDeclaration()) {
         return false;
      }

      String[] traceParameterTypes = getTraceParameterTypes(traceElement, method);
      if (traceParameterTypes.length == 0 && method.getParameters().size() == 0) {
         return true;
      } else if (method.getParameters().size() == 0) {
         return false;
      }
      
      
      final List<Parameter> parameters = method.getParameters();
      int parameterIndex = 0;
      LOG.trace("Length: {} vs {}", traceParameterTypes.length, parameters.size());
      if (traceParameterTypes.length != parameters.size() && !parameters.get(parameters.size() - 1).isVarArgs()) {
         return false;
      } else if (parameters.get(parameters.size() - 1).isVarArgs()) {
         if (traceParameterTypes.length < parameters.size() - 1) {
            return false;
         }
      }

      for (final Parameter parameter : parameters) {
         final Type type = parameter.getType();
         LOG.trace(type + " " + type.getClass());
         if (!parameter.isVarArgs()) {
            if (!checkParameter(traceParameterTypes, parameterIndex, type, false)) {
               return false;
            }
         } else {
            if (traceParameterTypes.length > parameterIndex) {
               for (int varArgIndex = parameterIndex; varArgIndex < traceParameterTypes.length; varArgIndex++) {
                  if (!checkParameter(traceParameterTypes, varArgIndex, type, true)) {
                     return false;
                  }
               }
            }
         }

         parameterIndex++;
      }

      return true;
   }

   private String[] getTraceParameterTypes(final TraceElementContent traceElement, final CallableDeclaration<?> method) {
      String[] traceParameterTypes;
      if (method.isConstructorDeclaration()) {
         Node parentNode = method.getParentNode().get();
         if (parentNode instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration parentClass = (ClassOrInterfaceDeclaration) parentNode;
            if (!parentClass.isStatic()) {
               traceParameterTypes = getCleanedTraceParameters(traceElement);
            } else {
               traceParameterTypes = traceElement.getParameterTypes();
            }
         } else {
            traceParameterTypes = getCleanedTraceParameters(traceElement);
            
         }
      } else {
         traceParameterTypes = traceElement.getParameterTypes();
      }
      return traceParameterTypes;
   }

   private String[] getCleanedTraceParameters(final TraceElementContent te) {
      String[] traceParameterTypes;
      if (te.isInnerClassCall()) {
         final String outerClazz = te.getOuterClass();
         final String firstType = te.getParameterTypes()[0];
         if (outerClazz.equals(firstType) || outerClazz.endsWith("." + firstType)) {
            // if (outerClazz.equals(firstType)) {
            traceParameterTypes = new String[te.getParameterTypes().length - 1];
            System.arraycopy(te.getParameterTypes(), 1, traceParameterTypes, 0, te.getParameterTypes().length - 1);
         } else {
            traceParameterTypes = te.getParameterTypes();
         }
      } else {
         traceParameterTypes = te.getParameterTypes();
      }
      return traceParameterTypes;
   }

   private boolean checkParameter(final String traceParameterTypes[], final int parameterIndex, final Type type, final boolean varArgAllowed) {
      final String simpleTraceParameterType = MethodReader.getSimpleType(traceParameterTypes[parameterIndex]);
      final String typeString = type instanceof ClassOrInterfaceType ? ((ClassOrInterfaceType) type).getNameAsString() : type.toString();
      // ClassOrInterfaceType
      if (typeString.equals(simpleTraceParameterType)) {
         return true;
      } else if (varArgAllowed && (typeString + "[]").equals(simpleTraceParameterType)) {
         return true;
      } else if (simpleTraceParameterType.contains("$")) {
         final String innerClassName = simpleTraceParameterType.substring(simpleTraceParameterType.indexOf("$") + 1);
         if (innerClassName.equals(typeString)) {
            return true;
         } else {
            return false;
         }
      } else if (clazz != null && clazz.getTypeParameters().size() > 0) {
         boolean isTypeParameter = isTypeParameter(typeString);
         return isTypeParameter;
      } else {
         return false;
      }
   }

   private boolean isTypeParameter(final String typeString) {
      boolean isTypeParameter = false;
      // It is too cumbersome to check whether a class really fits to the class hierarchy of a generic class;
      // therefore, we only check whether the parameter is one of the type parameters
      for (TypeParameter parameter : clazz.getTypeParameters()) {
         if (parameter.getName().toString().equals(typeString)) {
            isTypeParameter = true;
         }
      }
      return isTypeParameter;
   }
}
