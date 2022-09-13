package de.dagere.peass.debugtools;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class DetectUncoveredChanges implements Callable<Void> {

   @Option(names = { "-staticSelectionFiles", "--staticSelectionFiles" }, description = "Files from the same project that should be analyzed", required = true)
   public File[] staticSelectionFiles;

   public static void main(String[] args) {
      final CommandLine commandLine = new CommandLine(new DetectUncoveredChanges());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      List<String> commits = new LinkedList<>();
      StaticTestSelection[] selections = new StaticTestSelection[staticSelectionFiles.length];

      for (int i = 0; i < staticSelectionFiles.length; i++) {
         File staticSelectionFile = staticSelectionFiles[i];
         selections[i] = Constants.OBJECTMAPPER.readValue(staticSelectionFile, StaticTestSelection.class);
         for (String commit : selections[i].getCommits().keySet()) {
            commits.add(commit);
         }
      }

      int uncoveredCommits = 0;
      int uncoveredChanges = 0;
      int coveredCommits = 0;
      int coveredChanges = 0;
      for (String commit : commits) {
         Set<ChangedEntity> changes = selections[0].getCommits().get(commit).getChangedClazzes().keySet();

         boolean anyUncoveredCommit = false, anyCoveredCommit = false;
         for (ChangedEntity entity : changes) {
            boolean covered = isEntityCovered(selections, commit, entity);

            if (!covered) {
               System.out.println("Not covered in " + commit + ": " + entity);
               anyUncoveredCommit = true;
               uncoveredChanges++;
            }else {
               anyCoveredCommit = true;
               coveredChanges++;
            }
         }
         if (anyUncoveredCommit) {
            uncoveredCommits++;
         }
         if (anyCoveredCommit) {
            coveredCommits++;
         }
      }

      System.out.println("Uncovered commits: " + uncoveredCommits + " (" + uncoveredChanges + ")");
      System.out.println("Covered commits: " + coveredCommits + " (" + coveredChanges + ")");

      return null;
   }

   private boolean isEntityCovered(StaticTestSelection[] selections, String commit, ChangedEntity entity) {
      boolean covered = false;
      for (StaticTestSelection selection : selections) {
         CommitStaticSelection commitStaticSelection = selection.getCommits().get(commit);
         if (commitStaticSelection != null) {
            TestSet testSet = commitStaticSelection.getChangedClazzes().get(entity);
            if (testSet != null && testSet.getTestMethods().size() > 0) {
               covered = true;
            }
         }
      }
      return covered;
   }
}
