package de.peass.dependency.traces;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import de.peass.dependency.analysis.data.TraceElement;
import de.peass.dependency.traces.requitur.content.TraceElementContent;

/**
 * Utility for parsing java source by javaparser
 * 
 * @author reichelt
 *
 */
public class TraceReadUtils {

   private static final Logger LOG = LogManager.getLogger(TraceReadUtils.class);

   private static final Pattern findDotPattern = Pattern.compile(".", Pattern.LITERAL);
   private static final String QUOTE_REPLACEMENT = Matcher.quoteReplacement(File.separator);

   /**
    * Only static access to the util.
    */
   private TraceReadUtils() {

   }

   /**
    * Constructs the class file name out of the class of a trace element
    * 
    * @param te
    * @return
    */
   public static String getClassFileName(final TraceElementContent te) {
      final String javaClazzName = te.getClazz();

      String clazzFilePart = findDotPattern
            .matcher(javaClazzName)
            .replaceAll(QUOTE_REPLACEMENT);

      // String clazzFilePart = javaClazzName.replace(".", File.separator);
      final int indexOf = clazzFilePart.indexOf("$");
      if (indexOf != -1) {
         clazzFilePart = clazzFilePart.substring(0, indexOf);
      }
      final String clazzFileName = clazzFilePart + ".java";
      return clazzFileName;
   }

   public static List<NodeList<BodyDeclaration<?>>> getAnonymusClasses(final Node parent) {
      final List<NodeList<BodyDeclaration<?>>> foundAnonymousClasses = new LinkedList<>();
      for (final Node child : parent.getChildNodes()) {
         System.out.println(child.getClass());
         if (child instanceof ObjectCreationExpr) {
            final ObjectCreationExpr expr = (ObjectCreationExpr) child;
            if (expr.getAnonymousClassBody().isPresent()) {
               foundAnonymousClasses.add(expr.getAnonymousClassBody().get());
            } else {
               foundAnonymousClasses.addAll(getAnonymusClasses(child));
            }
         } else {
            foundAnonymousClasses.addAll(getAnonymusClasses(child));
         }
      }
      return foundAnonymousClasses;
   }

   public static Map<String, TypeDeclaration<?>> getNamedClasses(final Node parent, final String alreadyReadPrefix) {
      final Map<String, TypeDeclaration<?>> foundDeclaredClasses = new HashMap<>();
      for (final Node child : parent.getChildNodes()) {
         LOG.trace(child.getClass());
         if (child instanceof ClassOrInterfaceDeclaration) {
            final String ownName = ((ClassOrInterfaceDeclaration) child).getNameAsString();
            // if (parent instanceof ClassOrInterfaceDeclaration) {
            // foundDeclaredClasses.put(((ClassOrInterfaceDeclaration) parent).getNameAsString() + "$" + ownName, (ClassOrInterfaceDeclaration) child);
            // } else {
            // foundDeclaredClasses.put(ownName, (ClassOrInterfaceDeclaration) child);
            // }
            if (alreadyReadPrefix.equals("")) {
               foundDeclaredClasses.put(ownName, (ClassOrInterfaceDeclaration) child);
               foundDeclaredClasses.putAll(getNamedClasses(child, ownName));
            } else {
               foundDeclaredClasses.put(alreadyReadPrefix + "$" + ownName, (ClassOrInterfaceDeclaration) child);
               foundDeclaredClasses.putAll(getNamedClasses(child, alreadyReadPrefix + "$" + ownName));
            }

         } else {
            foundDeclaredClasses.putAll(getNamedClasses(child, alreadyReadPrefix));
         }

      }
      return foundDeclaredClasses;
   }

   public static Node getMethod(final TraceElementContent currentTraceElement, final CompilationUnit cu) {
      if (currentTraceElement.getClazz().contains("$")) {
         final String indexString = currentTraceElement.getClazz().split("\\$")[1];
         if (indexString.matches("[0-9]+")) {
            final int index = Integer.parseInt(indexString) - 1;
            final List<NodeList<BodyDeclaration<?>>> anonymousClazzes = getAnonymusClasses(cu);
            final NodeList<BodyDeclaration<?>> nodes = anonymousClazzes.get(index);
            for (final Node candidate : nodes) {
               LOG.trace(candidate);
               final Node ret = getMethod(candidate, currentTraceElement);
               if (ret != null) {
                  return ret;
               }
            }
         } else {
            final Map<String, TypeDeclaration<?>> namedClasses = getNamedClasses(cu, "");
            final String clazz = currentTraceElement.getClazz().substring(currentTraceElement.getClazz().lastIndexOf('.') + 1);
            final TypeDeclaration<?> declaration = namedClasses.get(clazz);
            return getMethod(declaration, currentTraceElement);
         }
      }
      Node method = null;
      for (final Node node : cu.getChildNodes()) {
         if (node instanceof ClassOrInterfaceDeclaration || node instanceof EnumDeclaration) {
            method = TraceReadUtils.getMethod(node, currentTraceElement);
            if (method != null) {
               break;
            }
         }
      }
      LOG.trace(currentTraceElement.getClazz() + " " + currentTraceElement.getMethod());
      LOG.trace(method);
      return method;
   }

   public static Node getMethod(final Node node, final TraceElementContent currentTraceElement) {
      if (node != null && node.getParentNode().isPresent()) {
         final Node parent = node.getParentNode().get();
         if (node instanceof MethodDeclaration) {
            final MethodDeclaration method = (MethodDeclaration) node;
            if (method.getNameAsString().equals(currentTraceElement.getMethod())) {
               LOG.trace("Parameter: {} Trace-Parameter: {}", method.getParameters().size(), currentTraceElement.getParameterTypes().length);
               if (parametersEqual(currentTraceElement, method)) {
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
                     if (parametersEqual(currentTraceElement, constructor)) {
                        return node;
                     }
                  }
               }
               LOG.trace(parent);
            }
         }

         for (final Node child : node.getChildNodes()) {
            final Node possibleMethod = getMethod(child, currentTraceElement);
            if (possibleMethod != null) {
               return possibleMethod;
            }

         }
      }

      return null;
   }

   private static boolean parametersEqual(final TraceElementContent te, final CallableDeclaration<?> method) {
      if (te.getParameterTypes().length == 0 && method.getParameters().size() == 0) {
         return true;
      } else if (method.getParameters().size() == 0) {
         return false;
      }
      int parameterIndex = 0;
      final List<Parameter> parameters = method.getParameters();
      String[] traceParameterTypes;
      if (te.isInnerClassCall()) {
         final String outerClazz = te.getOuterClass();
         final String firstType = te.getParameterTypes()[0];
         if (outerClazz.equals(firstType)) {
            traceParameterTypes = new String[te.getParameterTypes().length - 1];
            System.arraycopy(te.getParameterTypes(), 1, traceParameterTypes, 0, te.getParameterTypes().length - 1);
         } else {
            traceParameterTypes = te.getParameterTypes();
         }
      } else {
         traceParameterTypes = te.getParameterTypes();
      }
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
            if (!testParameter(traceParameterTypes, parameterIndex, type, false)) {
               return false;
            }
         } else {
            if (traceParameterTypes.length > parameterIndex) {
               for (int varArgIndex = parameterIndex; varArgIndex < traceParameterTypes.length; varArgIndex++) {
                  if (!testParameter(traceParameterTypes, varArgIndex, type, true)) {
                     return false;
                  }
               }
            }
         }

         parameterIndex++;
      }

      return true;
   }

   private static boolean testParameter(final String traceParameterTypes[], final int parameterIndex, final Type type, final boolean varArgAllowed) {
      final String traceParameterType = traceParameterTypes[parameterIndex];
      final String simpleTraceParameterType = traceParameterType.substring(traceParameterType.lastIndexOf('.') + 1);
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
      } else {
         return false;
      }
   }

   public static boolean traceElementsEquals(final TraceElement currentTraceElement, final TraceElement samePredecessorCandidate) {
      return samePredecessorCandidate.getClazz().equals(currentTraceElement.getClazz()) &&
            samePredecessorCandidate.getMethod().equals(currentTraceElement.getMethod()) &&
            Arrays.equals(samePredecessorCandidate.getParameterTypes(), currentTraceElement.getParameterTypes());
   }

}
