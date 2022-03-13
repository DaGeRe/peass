package de.dagere.peass.measurement.dataloading;

import java.util.Arrays;
import java.util.Iterator;

import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.peass.dependency.persistence.SelectedTests;

public class KoPeMeDataHelper {
   public static final String[] getVersions(Chunk chunk) {
      final String[] versions = new String[2];
      final Iterator<Result> iterator = chunk.getResult().iterator();
      versions[0] = iterator.next().getVersion().getGitversion();
      if (iterator.hasNext()) {
         while (iterator.hasNext()) {
            final Result r = iterator.next();
            if (!r.getVersion().getGitversion().equals(versions[0])) {
               versions[1] = r.getVersion().getGitversion();
               break;
            }
         }
      }
      return versions;
   }

   public static final String[] getVersions(Chunk chunk, SelectedTests selectedTests) {
      final String[] versions = new String[2];
      final Iterator<Result> iterator = chunk.getResult().iterator();
      versions[0] = iterator.next().getVersion().getGitversion();
      if (iterator.hasNext()) {
         while (iterator.hasNext()) {
            final Result r = iterator.next();
            if (!r.getVersion().getGitversion().equals(versions[0])) {
               versions[1] = r.getVersion().getGitversion();
               break;
            }
         }
      }
      int firstIndex = Arrays.binarySearch(selectedTests.getVersionNames(), versions[0]);
      int secondIndex = Arrays.binarySearch(selectedTests.getVersionNames(), versions[1]);
      if (firstIndex < secondIndex) {
         String[] versionsInCorrectOrder = new String[2];
         versionsInCorrectOrder[0] = versions[1];
         versionsInCorrectOrder[1] = versions[0];
         return versionsInCorrectOrder;
      } else {
         return versions;
      }
   }
}
