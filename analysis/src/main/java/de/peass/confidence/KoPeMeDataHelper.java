package de.peass.confidence;

import java.util.Iterator;

import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;

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
}
