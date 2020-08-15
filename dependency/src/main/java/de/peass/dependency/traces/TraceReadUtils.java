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
            return getMethodAnonymousClass(currentTraceElement, cu, indexString);
         } else {
            return getMethodNamedInnerClass(currentTraceElement, cu);
         }
      }
      Node method = null;
      for (final Node node : cu.getChildNodes()) {
         if (node instanceof ClassOrInterfaceDeclaration) {
            MethodReader reader = new MethodReader((ClassOrInterfaceDeclaration) node);
            method = reader.getMethod(node, currentTraceElement);
            if (method != null) {
               break;
            }
         } else if (node instanceof EnumDeclaration) {
            MethodReader reader = new MethodReader(null);
            method = reader.getMethod(node, currentTraceElement);
            if (method != null) {
               break;
            }
         }
      }
      LOG.trace(currentTraceElement.getClazz() + " " + currentTraceElement.getMethod());
      LOG.trace(method);
      return method;
   }

   private static Node getMethodNamedInnerClass(final TraceElementContent currentTraceElement, final CompilationUnit cu) {
      final Map<String, TypeDeclaration<?>> namedClasses = getNamedClasses(cu, "");
      final String clazz = currentTraceElement.getClazz().substring(currentTraceElement.getClazz().lastIndexOf('.') + 1);
      final TypeDeclaration<?> declaration = namedClasses.get(clazz);
      MethodReader reader = new MethodReader((ClassOrInterfaceDeclaration) null);
      return reader.getMethod(declaration, currentTraceElement);
   }

   private static Node getMethodAnonymousClass(final TraceElementContent currentTraceElement, final CompilationUnit cu, final String indexString) {
      final int index = Integer.parseInt(indexString) - 1;
      final List<NodeList<BodyDeclaration<?>>> anonymousClazzes = getAnonymusClasses(cu);
      final NodeList<BodyDeclaration<?>> nodes = anonymousClazzes.get(index);
      MethodReader reader = new MethodReader(null);
      for (final Node candidate : nodes) {
         LOG.trace(candidate);
         final Node ret = reader.getMethod(candidate, currentTraceElement);
         if (ret != null) {
            return ret;
         }
      }
      return null;
   }

   

   public static boolean traceElementsEquals(final TraceElement currentTraceElement, final TraceElement samePredecessorCandidate) {
      return samePredecessorCandidate.getClazz().equals(currentTraceElement.getClazz()) &&
            samePredecessorCandidate.getMethod().equals(currentTraceElement.getMethod()) &&
            Arrays.equals(samePredecessorCandidate.getParameterTypes(), currentTraceElement.getParameterTypes());
   }

}
