package de.dagere.peass.measurement.dataloading;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.dependency.persistence.SelectedTests;

public class KoPeMeDataHelper {
   public static final String[] getCommits(VMResultChunk chunk) {
      final String[] commits = new String[2];
      final Iterator<VMResult> iterator = chunk.getResults().iterator();
      commits[0] = iterator.next().getCommit();
      if (iterator.hasNext()) {
         while (iterator.hasNext()) {
            final VMResult r = iterator.next();
            if (!r.getCommit().equals(commits[0])) {
               commits[1] = r.getCommit();
               break;
            }
         }
      }
      return commits;
   }

   public static final String[] getCommits(VMResultChunk chunk, SelectedTests selectedTests) {
      final String[] commits = getCommits(chunk);
      return filterCommits(selectedTests, commits);
   }

   private static String[] filterCommits(SelectedTests selectedTests, final String[] commits) {
      String[] versionNames = selectedTests.getCommitNames();
      List<String> versionOrderList = Arrays.asList(versionNames);
      int firstIndex = versionOrderList.indexOf(commits[0]);
      int secondIndex = versionOrderList.indexOf(commits[1]);
      if (firstIndex > secondIndex) {
         String[] versionsInCorrectOrder = new String[2];
         versionsInCorrectOrder[0] = commits[1];
         versionsInCorrectOrder[1] = commits[0];
         return versionsInCorrectOrder;
      } else {
         return commits;
      }
   }
   
   // TODO: Use unified version of this method
   public static List<String> getCommitList(final VMResultChunk chunk) {
      List<String> commits = new LinkedList<>();
      for (VMResult result : chunk.getResults()) {
         if (!commits.contains(result.getCommit())) {
            commits.add(result.getCommit());
         }
      }
      return commits;
   }
}
