package de.dagere.peass.config;

import java.io.Serializable;

public class FixedCommitConfig implements Serializable {
   
   private static final long serialVersionUID = 3129231099963995908L;
   
   private String commit = "HEAD";
   private String commitOld = "HEAD~1";

   public FixedCommitConfig() {
   }
   
   public FixedCommitConfig(FixedCommitConfig fixedCommitConfig) {
      this.commit = fixedCommitConfig.getCommit();
      this.commitOld = fixedCommitConfig.getCommitOld();
   }

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
