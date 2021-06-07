package de.dagere.peass.execution.maven;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.execution.EnvironmentVariables;

public class MavenCleaner {
   
   private static final Logger LOG = LogManager.getLogger(MavenCleaner.class);
   
   private final PeassFolders folders;
   private final EnvironmentVariables env;
   
   public MavenCleaner(final PeassFolders folders, final EnvironmentVariables env) {
      this.folders = folders;
      this.env = env;
   }

   public void clean(final File logFile) throws IOException, InterruptedException {
      if (!folders.getProjectFolder().exists()) {
         throw new RuntimeException("Can not execute clean - folder " + folders.getProjectFolder().getAbsolutePath() + " does not exist");
      } else {
         LOG.debug("Folder {} exists {} and is directory - cleaning should be possible",
               folders.getProjectFolder().getAbsolutePath(),
               folders.getProjectFolder().exists(),
               folders.getProjectFolder().isDirectory());
      }
      final String[] originalsClean = new String[] { env.fetchMavenCall(), "clean" };
      final ProcessBuilder pbClean = new ProcessBuilder(originalsClean);
      pbClean.directory(folders.getProjectFolder());
      if (logFile != null) {
         pbClean.redirectOutput(Redirect.appendTo(logFile));
         pbClean.redirectError(Redirect.appendTo(logFile));
      }

      cleanSafely(pbClean);
   }

   private void cleanSafely(final ProcessBuilder pbClean) throws IOException, InterruptedException {
      boolean finished = false;
      int count = 0;
      while (!finished && count < 10) {
         final Process processClean = pbClean.start();
         finished = processClean.waitFor(60, TimeUnit.MINUTES);
         if (!finished) {
            LOG.info("Clean process " + processClean + " was not finished successfully; trying again to clean");
            processClean.destroyForcibly();
         }
         count++;
      }
   }
}
