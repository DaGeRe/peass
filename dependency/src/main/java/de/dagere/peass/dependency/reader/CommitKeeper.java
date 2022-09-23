package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.utils.Constants;

public class CommitKeeper {
   
   public final static CommitKeeper INSTANCE = new CommitKeeper();
  
   private CommitKeeper() {
      goal = new File("/dev/null");
   }
  
   @JsonIgnore
   private final File goal;
   
   Map<String, String> nonRunableReasons = new LinkedHashMap<>();

   public CommitKeeper(final File goal) {
      super();
      this.goal = goal;
   }
   
   public Map<String, String> getNonRunableReasons() {
      return nonRunableReasons;
   }

   public void setNonRunableReasons(final Map<String, String> nonRunableReasons) {
      this.nonRunableReasons = nonRunableReasons;
   }
   
   @JsonIgnore
   public synchronized void addCommit(final String commit, final String reason) {
      nonRunableReasons.put(commit, reason);
      try {
         Constants.OBJECTMAPPER.writeValue(goal, this);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }
}
