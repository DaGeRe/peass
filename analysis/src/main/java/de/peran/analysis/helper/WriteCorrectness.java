package de.peran.analysis.helper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peran.FolderSearcher;
import de.peran.measurement.analysis.StatisticInfo;

public class WriteCorrectness {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final File changesSaveFile = new File(args[0]);
      final File statisticsFile = new File(args[1]);
      final File changesUnsaveFile = new File(args[2]);

      final ProjectChanges correctChanges = FolderSearcher.MAPPER.readValue(changesSaveFile, ProjectChanges.class);
      final ProjectChanges unsaveChanges = FolderSearcher.MAPPER.readValue(changesUnsaveFile, ProjectChanges.class);
      final StatisticInfo info = FolderSearcher.MAPPER.readValue(statisticsFile, StatisticInfo.class);

      int correct = 0;
      final int incorrect = 0;
      for (final Entry<String, Changes> version : correctChanges.getVersionChanges().entrySet()) {
         for (final Entry<String, List<Change>> testcase : version.getValue().getTestcaseChanges().entrySet()) {
            for (final Change change : testcase.getValue()) {
               final Changes changes = unsaveChanges.getVersionChanges().get(version.getKey());
               if (changes != null) {
                  final List<Change> otherChanges = changes.getTestcaseChanges().get(testcase.getKey());
                  Change other = null;
                  if (otherChanges != null) {
                     for (final Change otherCandidate : otherChanges) {
                        if (otherCandidate.getMethod().equals(change.getMethod())) {
                           other = otherCandidate;
                           break;
                        }
                     }
                     if (other != null) {
                        if (Math.abs(change.getTvalue()) > 3) {
//                           other.setCorrectness("YES");
                           correct++;
                        } else {
                           // Statistic statistic = info.getStatistics().get(version.getKey()).get(new TestCase(testcase.getKey()));
                            System.out.println(version.getKey() + " " + testcase.getKey() + " " + change.getTvalue());
                           // if (Math.abs(change.getTvalue()) < 2) {
                           // System.out.println("Problem: " + testcase.getKey());
                           // }
                           // if (Math.abs(change.getTvalue()) < 1 ||
                           // (Math.abs(change.getTvalue()) < 2 && statistic.getExecutions() > 50)) {
                           // other.setCorrectness("NO");
                           // incorrect++;
                           // }
                        }
                     } else {
//                        Map<TestCase, Statistic> versionData = info.getStatistics().get(version.getKey());
//                        TestCase testCase2 = new TestCase(testcase.getKey(), change.getMethod());
                        // System.out.println(versionData.keySet());
                        // System.out.println(versionData.keySet().iterator().next());
                        // System.out.println(testCase2);
                        // TODO Correct Jackson-Deserialization
//                        Statistic statistic = versionData.get(new TestCase(testCase2.toString(), null));
//                        System.out.println(version.getKey() + " " + testcase.getKey() + " " + change.getTvalue());
//                        System.out.println(statistic.getTvalue());
                     }
                  }
               }
            }
         }
      }
      FolderSearcher.MAPPER.writeValue(changesUnsaveFile, unsaveChanges);
      System.out.println("Correct: " + correct + " Incorrect: " + incorrect);
   }
}
