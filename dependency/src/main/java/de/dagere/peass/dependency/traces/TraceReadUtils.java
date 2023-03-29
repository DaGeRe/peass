package de.dagere.peass.dependency.traces;

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
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.nodeDiffGenerator.sourceReading.SourceReadUtils;
import de.dagere.peass.dependency.analysis.data.TraceElement;

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

   public static boolean traceElementsEquals(final TraceElement currentTraceElement, final TraceElement samePredecessorCandidate) {
      return samePredecessorCandidate.getClazz().equals(currentTraceElement.getClazz()) &&
            samePredecessorCandidate.getMethod().equals(currentTraceElement.getMethod()) &&
            Arrays.equals(samePredecessorCandidate.getParameterTypes(), currentTraceElement.getParameterTypes());
   }

}
