package de.dagere.peass.dependency.changesreading;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;

/**
 * Provides ThreadLocal JavaParser-instances, so parsing can be done in parallel
 * @author reichelt
 *
 */
public class JavaParserProvider {
   
   private final static ThreadLocal<JavaParser> javaParser = new ThreadLocal<JavaParser>() {
      protected JavaParser initialValue() {
         return new JavaParser();
      };
   };

   public synchronized static CompilationUnit parse(final File file) throws FileNotFoundException {
      final JavaParser parser = javaParser.get();
      final Optional<CompilationUnit> result = parser.parse(file).getResult();
      if (!result.isPresent()) {
         List<Problem> problems = new LinkedList<>();
         problems.add(new Problem("Could not parse class", null, null));
         throw new ParseProblemException(problems);
      }
      return result.get();
   }
   
   public static ThreadLocal<JavaParser> getJavaparser() {
      return javaParser;
   }

}
