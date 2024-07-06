package de.dagere.peass.analysis.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.javaparser.ast.CompilationUnit;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.diffDetection.ClazzChangeData;
import de.dagere.nodeDiffDetector.diffDetection.FileComparisonUtil;
import de.dagere.nodeDiffDetector.sourceReading.MethodReader;
import de.dagere.nodeDiffDetector.typeFinding.TypeCache;
import de.dagere.nodeDiffDetector.typeFinding.TypeFileFinder;
import de.dagere.nodeDiffDetector.utils.JavaParserProvider;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.folders.PeassFolders;

public class PropertyChangeGuesser {

   public Set<String> getGuesses(final PeassFolders folders, final Entry<MethodCall, ClazzChangeData> changedEntity) throws FileNotFoundException {
      final Set<String> guessedTypes = new HashSet<>();
      
      //TODO Here, a real config should be passed; since this is rarely used, we go with the default folders
      TypeFileFinder finder = new TypeFileFinder(new ExecutionConfig());
      final File file = finder.getSourceFile(folders.getProjectFolder(), changedEntity.getKey());
      final File fileOld = finder.getSourceFile(folders.getOldSources(), changedEntity.getKey());

      if (file != null && fileOld != null && file.exists() && fileOld.exists()) {
         TypeCache cache = new TypeCache();

         for (Map.Entry<String, Set<String>> changedClazz : changedEntity.getValue().getChangedMethods().entrySet()) {
            // If only method change..
            if (changedClazz.getValue() != null) {
               for (String method : changedClazz.getValue()) {
                  final String source = cache.getMethodSource(changedEntity.getKey(), method, file);
                  final String sourceOld = cache.getMethodSource(changedEntity.getKey(), method, fileOld);
                  final Patch<String> changedLinesMethod = DiffUtils.diff(Arrays.asList(sourceOld.split("\n")), Arrays.asList(source.split("\n")));

                  for (final AbstractDelta<String> delta : changedLinesMethod.getDeltas()) {
                     getDeltaGuess(guessedTypes, (delta.getSource().getLines()));
                     getDeltaGuess(guessedTypes, (delta.getTarget().getLines()));
                  }
               }
            }

         }
      }
      return guessedTypes;
   }

   private void getDeltaGuess(final Set<String> guessedTypes, final List<String> delta) {
      for (final String line : delta) {
         if (line.contains("synchronized")) {
            guessedTypes.add("SYNCHRONIZED");
         }
         if (line.contains("toArray")) {
            guessedTypes.add("OPTIM");
         }
      }
   }
}
