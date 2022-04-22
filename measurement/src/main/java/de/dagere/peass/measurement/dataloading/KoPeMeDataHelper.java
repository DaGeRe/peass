package de.dagere.peass.measurement.dataloading;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.dependency.persistence.SelectedTests;

public class KoPeMeDataHelper {
   public static final String[] getVersions(VMResultChunk chunk) {
      final String[] versions = new String[2];
      final Iterator<VMResult> iterator = chunk.getResults().iterator();
      versions[0] = iterator.next().getCommit();
      if (iterator.hasNext()) {
         while (iterator.hasNext()) {
            final VMResult r = iterator.next();
            if (!r.getCommit().equals(versions[0])) {
               versions[1] = r.getCommit();
               break;
            }
         }
      }
      return versions;
   }

   public static final String[] getVersions(VMResultChunk chunk, SelectedTests selectedTests) {
      final String[] versions = getVersions(chunk);
      return filterVersions(selectedTests, versions);
   }

   private static String[] filterVersions(SelectedTests selectedTests, final String[] versions) {
      String[] versionNames = selectedTests.getVersionNames();
      List<String> versionOrderList = Arrays.asList(versionNames);
      int firstIndex = versionOrderList.indexOf(versions[0]);
      int secondIndex = versionOrderList.indexOf(versions[1]);
      if (firstIndex > secondIndex) {
         String[] versionsInCorrectOrder = new String[2];
         versionsInCorrectOrder[0] = versions[1];
         versionsInCorrectOrder[1] = versions[0];
         return versionsInCorrectOrder;
      } else {
         return versions;
      }
   }
   
   // TODO: Use unified version of this method
   public static List<String> getVersionList(final VMResultChunk chunk) {
      List<String> versions = new LinkedList<>();
      for (VMResult result : chunk.getResults()) {
         if (!versions.contains(result.getCommit())) {
            versions.add(result.getCommit());
         }
      }
      return versions;
   }
}
