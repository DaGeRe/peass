package de.peass.dependency.traces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import de.peass.dependency.ClazzFinder;
import de.peass.dependency.analysis.CalledMethodLoader;
import de.peass.dependency.analysis.FileComparisonUtil;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.TraceElement;
import de.peass.dependency.traces.requitur.ReducedTraceElement;
import de.peass.dependency.traces.requitur.RunLengthEncodingSequitur;
import de.peass.dependency.traces.requitur.Sequitur;
import de.peass.dependency.traces.requitur.TraceStateTester;
import de.peass.dependency.traces.requitur.content.Content;
import de.peass.dependency.traces.requitur.content.TraceElementContent;

/**
 * Reads the traces of kieker results and combines them with source information read by javaparser.
 * 
 * @author reichelt
 *
 */
public class TraceMethodReader {

   public static final String CYCLE_PREFIX = "#Generated$";

   private static final Logger LOG = LogManager.getLogger(TraceMethodReader.class);

   private final List<TraceElement> calls;
   private final File[] clazzFolder;

   private final Map<File, CompilationUnit> loadedUnits = new HashMap<>();
   final TraceWithMethods trace;
   final Sequitur seq = new Sequitur();

   public TraceMethodReader(final List<TraceElement> calls, final File... clazzFolder) throws FileNotFoundException {
      this.calls = calls;
      this.clazzFolder = clazzFolder;
      trace = loadTrace();
   }

   public TraceMethodReader(final File traceFolder, final ModuleClassMapping mapping, final File... clazzFolder) throws FileNotFoundException {
      this.calls = new CalledMethodLoader(traceFolder, mapping).getShortTrace(null);
      this.clazzFolder = clazzFolder;
      trace = loadTrace();
   }
   
   private TraceWithMethods loadTrace() throws FileNotFoundException {
      LOG.debug("Trace Length: {}", calls.size());
      seq.addTraceElements(calls);
      final RunLengthEncodingSequitur runLengthEncodingSequitur = new RunLengthEncodingSequitur(seq);
      runLengthEncodingSequitur.reduce();
      final List<ReducedTraceElement> rleTrace = runLengthEncodingSequitur.getReadableRLETrace();
      final TraceWithMethods trace = new TraceWithMethods(rleTrace);
      for (final ReducedTraceElement traceElement : rleTrace) {
         if (traceElement.getValue() instanceof TraceElementContent) {
            final TraceElementContent te = (TraceElementContent) traceElement.getValue();
            final File clazzFile = ClazzFinder.getClazzFile(te, clazzFolder);
            if (clazzFile != null) {
               CompilationUnit cu = loadedUnits.get(clazzFile);
               if (cu == null) {
                  LOG.trace("CU {} not imported yet", clazzFile);
                  cu = FileComparisonUtil.parse(clazzFile);
                  loadedUnits.put(clazzFile, cu);
               }
               final Node method = TraceReadUtils.getMethod(te, cu);

               if (method != null) {
                  final String commentedMethod = method.toString().intern();
                  trace.setElementSource(te, commentedMethod);
                  method.setComment(null);
                  final String noCommentMethod = method.toString().intern();
                  trace.setElementSourceNoComment(te, noCommentMethod);
               } else {
                  LOG.debug("Not found: " + te);

                  trace.setElementSource(te, null);
                  trace.setElementSourceNoComment(te, null);
               }
            }
         }
      }
      return trace;
   }
   
   public List<Content> getExpandedTrace() {
      return TraceStateTester.expandContentTrace(seq.getUncompressedTrace(), seq.getRules());
   }

   public TraceWithMethods getTraceWithMethods() throws ParseException, IOException {
      return trace;
   }

   public static void main(final String[] args) throws ParseException, IOException {
      final File traceFolder = new File(args[0]);
      final File projectFolder = new File(args[1]);
      final File clazzFolder[] = new File[4];

      clazzFolder[0] = new File(projectFolder, "src/main/java");
      clazzFolder[1] = new File(projectFolder, "src/java");
      clazzFolder[2] = new File(projectFolder, "src/test/java");
      clazzFolder[3] = new File(projectFolder, "src/test");

      final TraceMethodReader reader = new TraceMethodReader(traceFolder, ModuleClassMapping.SINGLE_MODULE_MAPPING, clazzFolder);
      reader.getTraceWithMethods();
   }

}
