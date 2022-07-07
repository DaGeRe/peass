package de.dagere.peass.config.parameters;

import picocli.CommandLine.Option;

public class FixedCommitMixin {
   @Option(names = { "-commit", "--commit" }, description = "Newer commit for regression test selection / measurement. Do not use together with startcommit / endcommit.")
   protected String commit;

   @Option(names = { "-commitOld", "--commitOld" }, description = "Older commit for regression test selection / measurement" +
         "If used, please always specify commit; only the difference of both will be analyzed, intermediary commits will be ignored. Do not use together with startcommit / endcommit.")
   protected String commitOld;
   
   public String getCommit() {
      return commit;
   }

   public void setCommit(String commit) {
      this.commit = commit;
   }

   public String getCommitOld() {
      return commitOld;
   }

   public void setCommitOld(String commitOld) {
      this.commitOld = commitOld;
   }
}
