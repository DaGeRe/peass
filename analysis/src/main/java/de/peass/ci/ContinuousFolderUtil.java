package de.peass.ci;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.utils.StreamGobbler;
import de.peass.vcs.VersionControlSystem;

public enum ContinuousFolderUtil {
   ;
   
   private static final Logger LOG = LogManager.getLogger(ContinuousExecutor.class);
   
   public static File getLocalFolder(final File projectFolder) {
      return new File(projectFolder, ".." + File.separator + projectFolder.getName() + "_fullPeass");
   }
   
   public static File getProjectLocalFolder(final File localFolder, final File projectFolder) throws IOException {
      File vcsFolder = VersionControlSystem.findVCSFolder(projectFolder);
      if (vcsFolder != null) {
         String localSuffix = projectFolder.getCanonicalPath().substring(vcsFolder.getCanonicalPath().length() + 1);
         return new File(localFolder, vcsFolder.getName() + File.separator + localSuffix);
      } else {
         return null;
      }
   }

   public static void cloneProject(final File cloneProjectFolder, final File localFolder) throws InterruptedException, IOException {
      localFolder.mkdirs();
      File originalVcsFolder = VersionControlSystem.findVCSFolder(cloneProjectFolder);
      if (originalVcsFolder != null && originalVcsFolder.exists()) {
         LOG.info("Cloning using git clone");
         final ProcessBuilder builder = new ProcessBuilder("git", "clone", originalVcsFolder.getAbsolutePath());
         builder.directory(localFolder);
         StreamGobbler.showFullProcess(builder.start());
      } else {
         throw new RuntimeException("No git folder in " + cloneProjectFolder.getAbsolutePath() + " (or parent) present - "
               + "currently, only git projects are supported");
      }
   }

}
