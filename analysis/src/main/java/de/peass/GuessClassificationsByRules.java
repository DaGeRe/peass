package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.properties.ChangeProperties;
import de.dagere.peass.analysis.properties.ChangeProperty;
import de.dagere.peass.analysis.properties.ChangeProperty.TraceChange;
import de.dagere.peass.analysis.properties.VersionChangeProperties;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.peass.analysis.all.RepoFolders;
import de.peass.analysis.groups.Classification;
import de.peass.analysis.groups.TestcaseClass;
import de.peass.analysis.groups.VersionClass;
import de.peass.analysis.guessing.ExpectedDirection;
import de.peass.analysis.guessing.Guess;
import de.peass.analysis.guessing.GuessDecider;
import de.peran.FolderSearcher;
import difflib.Delta;
import difflib.Patch;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Classifies by method diffs", name = "guessclassifications")
public class GuessClassificationsByRules implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(GuessClassificationsByRules.class);

   @Option(names = { "-methodfiles", "--methodfiles" }, description = "Path to a folder containing all method sources with changes", required = true)
   private File methodFileFolder;

   @Option(names = { "-project", "--project" }, description = "Name of the project for analysis", required = true)
   private File project;

   private int correct = 0, all = 0, wrong = 0;

   public static void main(final String[] args) {
      CommandLine commandLine = new CommandLine(new GuessClassificationsByRules());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      RepoFolders repos = new RepoFolders();

      File propertyFile = new File(methodFileFolder.getParentFile(), project + ".json");
      File classificationFile = new File(repos.getClassificationFolder(), project + ".json");
      Classification manual = FolderSearcher.MAPPER.readValue(classificationFile, Classification.class);

      final VersionChangeProperties changes = FolderSearcher.MAPPER.readValue(propertyFile, VersionChangeProperties.class);

      for (Entry<String, ChangeProperties> version : changes.getVersions().entrySet()) {
         guessVersion(manual, version);
      }

      System.out.println("Correct: " + correct + " Wrong " + wrong + " All: " + all);

      return null;
   }

   private void guessVersion(final Classification manual, final Entry<String, ChangeProperties> version) throws IOException {
      File versionFolder = new File(methodFileFolder, version.getKey());
      for (Entry<String, List<ChangeProperty>> testcase : version.getValue().getProperties().entrySet()) {

         GuessDecider guesser = new GuessDecider(versionFolder);
         for (ChangeProperty property : testcase.getValue()) {
            all++;
            if (version.getKey().equals("4ed6e923cb2033272fcb993978d69e325990a5aa")) {
               System.err.println();
            }
            Guess guess = guesser.guess(property.getAffectedMethods());
            testStructuralChanges(guesser, property, guess);
            if (guess != null) {
               VersionClass versionClass = manual.getVersions().get(version.getKey());
               if (versionClass != null) {
                  ChangedEntity entity = new ChangedEntity(testcase.getKey(), "", property.getMethod());
                  TestcaseClass testcaseClass = versionClass.getTestcases().get(entity);

                  if (testcaseClass != null) {
                     checkCorrectness(guess, testcaseClass, version.getKey(), entity);
                  } else {
                     LOG.error("In version {}, Testcase {} is missing - changes not consistent with properties!", version.getKey(), testcase);
                  }
               } else {
                  LOG.error("Version {} is missing - changes not consistent with properties!", version.getKey());
               }

            }
         }
      }
   }

   private void checkCorrectness(final Guess guess, final TestcaseClass testcaseClass, final String version, final ChangedEntity entity) {
      boolean oneCorrect = false;
      boolean oneWrong = false;
      for (Map.Entry<String, ExpectedDirection> type : guess.getDirections().entrySet()) {
         if (testcaseClass.getTypes().contains(type.getKey())) {
            ExpectedDirection direction = ExpectedDirection.getDirection(testcaseClass.getDirection());
            if (direction == type.getValue()) {
               oneCorrect = true;
            }else {
               System.out.println("Direction Wrong: " + version + " " + entity + " " + type.getKey());
               System.out.println("All: " + guess.getDirections().keySet());
               oneWrong = true;
            }
         }else {
            oneWrong = true;
         }
      }
      if (oneCorrect) {
         correct++;
      } else {
         wrong++;
         // System.out.println(version.getKey() + " " + testcase.getKey() + "#" + property.getMethod() + " " + guess + " (Original: " + testcaseClass.getTypes() + ")");
      }
   }

   private void testStructuralChanges(final GuessDecider guesser, final ChangeProperty property, final Guess guess) throws IOException {
      if (property.getTraceChangeType().equals(TraceChange.ADDED_CALLS) || property.getTraceChangeType().equals(TraceChange.NO_CALL_CHANGE)) {
         for (String method : property.getAffectedMethods()) {
            Patch<String> patch = guesser.getDiff(method);
            if (patch.getDeltas().size() > 0) {
               boolean hasAlsoRemoved = false;
               for (Delta<String> delta : patch.getDeltas()) {
                  if (delta.getOriginal().size() != 0) {
                     hasAlsoRemoved = true;
                  }
               }
               if (!hasAlsoRemoved) {
                  guess.add("ADDITIONAL_PROCESSING", ExpectedDirection.SLOWER);
               }
            }
         }
      }
      if (property.getTraceChangeType().equals(TraceChange.REMOVED_CALLS) || property.getTraceChangeType().equals(TraceChange.NO_CALL_CHANGE)) {
         for (String method : property.getAffectedMethods()) {
            Patch<String> patch = guesser.getDiff(method);
            if (patch.getDeltas().size() > 0) {
               boolean hasAlsoRemoved = false;
               for (Delta<String> delta : patch.getDeltas()) {
                  if (delta.getRevised().size() != 0) {
                     hasAlsoRemoved = true;
                  }
               }
               if (!hasAlsoRemoved) {
                  guess.add("REDUCED_PROCESSING", ExpectedDirection.SLOWER);
               }
            }

         }
      }
   }
}
