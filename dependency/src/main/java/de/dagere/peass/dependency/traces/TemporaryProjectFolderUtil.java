package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.TempPeassFolders;
import de.dagere.peass.folders.VMExecutionLogFolders;
import de.dagere.peass.vcs.GitUtils;

public class TemporaryProjectFolderUtil {

   private static final Logger LOG = LogManager.getLogger(TemporaryProjectFolderUtil.class);

   public static PeassFolders cloneForcefully(final PeassFolders originalFolders, final File dest, final VMExecutionLogFolders logFolders) throws IOException, InterruptedException {
      if (dest.exists()) {
         File peassFolder = new File(dest.getParentFile(), dest.getName() + PeassFolders.PEASS_POSTFIX);
         if (peassFolder.exists()) {
            LOG.warn("Deleting existing folder {}", peassFolder);
            FileUtils.deleteDirectory(peassFolder);
         }
      } else {
         GitUtils.clone(originalFolders, dest);
      }
      final PeassFolders folders = new TempPeassFolders(dest, originalFolders.getProjectName(), logFolders);
      return folders;
   }
}
