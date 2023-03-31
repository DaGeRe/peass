package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.sourceReading.SourceReadUtils;
import de.dagere.nodeDiffDetector.typeFinding.TypeFinder;
import de.dagere.nodeDiffDetector.utils.JavaParserProvider;

public class TypeCache {
   
   private static final Logger LOG = LogManager.getLogger(TypeCache.class);
   
   private final Map<File, CompilationUnit> loadedUnits = new HashMap<>();
   
   public CompilationUnit getCU(final File clazzFile) throws FileNotFoundException {
      CompilationUnit cu = loadedUnits.get(clazzFile);
      if (cu == null) {
         LOG.trace("CU {} not imported yet", clazzFile);
         cu = JavaParserProvider.parse(clazzFile);
         loadedUnits.put(clazzFile, cu);
      }
      return cu;
   }
   
   public List<String> getTypes(File typeFile) throws FileNotFoundException{
      CompilationUnit cu = getCU(typeFile);
      List<String> clazzes = TypeFinder.getTypes(cu);
      return clazzes;
   }
   
   public Node getMethod(File typeFile, MethodCall call) throws FileNotFoundException {
      CompilationUnit cu = getCU(typeFile);
      final Node method = SourceReadUtils.getMethod(call, cu);
      return method;
   }
   
   public String getMethodSource(final MethodCall entity, final String method, File typeFile) throws FileNotFoundException {
      CompilationUnit typeUnit = getCU(typeFile);
      final Node node = SourceReadUtils.getMethod(entity, typeUnit);
      if (node != null) {
         return node.toString();
      } else {
         return "";
      }
   }
}
