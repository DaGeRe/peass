package de.peass.analysis.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;

import de.peass.dependency.ClazzFinder;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.ClazzChangeData;
import de.peass.dependency.changesreading.FileComparisonUtil;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class PropertyChangeGuesser {

   public Set<String> getGuesses(final PeASSFolders folders, final Entry<ChangedEntity, ClazzChangeData> changedEntity) throws FileNotFoundException {
      final Set<String> guessedTypes = new HashSet<>();
      final File file = ClazzFinder.getSourceFile(folders.getProjectFolder(), changedEntity.getKey());
      final File fileOld = ClazzFinder.getSourceFile(folders.getOldSources(), changedEntity.getKey());

      if (file != null && fileOld != null && file.exists() && fileOld.exists()) {
         final CompilationUnit clazzUnit = FileComparisonUtil.parse(file);
         final CompilationUnit clazzUnitOld = FileComparisonUtil.parse(fileOld);

         for (Map.Entry<String, Set<String>> changedClazz : changedEntity.getValue().getChangedMethods().entrySet()) {
            // If only method change..
            if (changedClazz.getValue() != null) {
               for (String method : changedClazz.getValue()) {
                  final String source = FileComparisonUtil.getMethod(changedEntity.getKey(), method, clazzUnit);
                  final String sourceOld = FileComparisonUtil.getMethod(changedEntity.getKey(), method, clazzUnitOld);
                  final Patch<String> changedLinesMethod = DiffUtils.diff(Arrays.asList(sourceOld.split("\n")), Arrays.asList(source.split("\n")));

                  for (final Delta<String> delta : changedLinesMethod.getDeltas()) {
                     getDeltaGuess(guessedTypes, (delta.getOriginal().getLines()));
                     getDeltaGuess(guessedTypes, (delta.getRevised().getLines()));
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
