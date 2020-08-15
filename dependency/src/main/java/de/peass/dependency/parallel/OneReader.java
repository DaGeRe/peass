package de.peass.dependency.parallel;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.reader.DependencyReader;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.vcs.GitCommit;
import de.peass.vcs.VersionIterator;

public final class OneReader implements Runnable {

   private static final Logger LOG = LogManager.getLogger(OneReader.class);

   private final GitCommit minimumCommit;
   private final File currentOutFile;
   private final DependencyReader reader;
   private final VersionIterator reserveIterator;

   public OneReader(GitCommit minimumCommit, File currentOutFile, VersionIterator reserveIterator, DependencyReader reader) {
      this.minimumCommit = minimumCommit;
      this.currentOutFile = currentOutFile;
      this.reserveIterator = reserveIterator;
      this.reader = reader;
   }

   @Override
   public void run() {
      try {
         LOG.debug("Reader initalized: " + currentOutFile + " This: " + this);
         final boolean readingSuccess = reader.readDependencies();
         if (readingSuccess) {
            readRemaining(reader);
         }
      } catch (final Throwable e) {
         e.printStackTrace();
      }
   }

   private void readRemaining(final DependencyReader reader) {
      String newest = reader.getDependencies().getNewestVersion();
      reader.setIterator(reserveIterator);
      while (reserveIterator.hasNextCommit() && VersionComparator.isBefore(newest, minimumCommit.getTag())) {
         reserveIterator.goToNextCommit();
         LOG.debug("Remaining: {} This: {}", reserveIterator.getTag(), this);
         try {
            reader.readVersion();
         } catch (final IOException e) {
            e.printStackTrace();
         }
         newest = reader.getDependencies().getNewestVersion();
      }
   }
}