package de.dagere.peass.dependency.parallel;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ParseException;

import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.FirstRunningVersionFinder;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.vcs.CommitIterator;

public final class OneReader implements Runnable {

   private static final Logger LOG = LogManager.getLogger(OneReader.class);

   private final String minimumCommit;
   private final CommitIterator reserveIterator;
   final FirstRunningVersionFinder firstRunningVersionFinder;
   private final DependencyReader reader;
   private final CommitComparatorInstance comparator;

   public OneReader(final String minimumCommit, final CommitIterator reserveIterator, final DependencyReader reader,
         final FirstRunningVersionFinder firstRunningVersionFinder, CommitComparatorInstance comparator) {
      this.minimumCommit = minimumCommit;
      this.reserveIterator = reserveIterator;
      this.firstRunningVersionFinder = firstRunningVersionFinder;
      this.reader = reader;
      this.comparator = comparator;
   }

   @Override
   public void run() {
      try {
         boolean init = firstRunningVersionFinder.searchFirstRunningCommit();
         if (init) {
            LOG.debug("Reader initalized: " + reader + " This: " + this);
            if (!reader.readInitialCommit()) {
               LOG.error("Analyzing first commit was not possible");
            } else {
               final boolean readingSuccess = reader.readDependencies();
               if (readingSuccess) {
                  readRemaining(reader);
               }
            }
         }
      } catch (final Throwable e) {
         e.printStackTrace();
      }
   }

   private void readRemaining(final DependencyReader reader) throws FileNotFoundException, IOException, XmlPullParserException, InterruptedException, ParseException {
      String newest = reader.getDependencies().getNewestCommit();
      reader.setIterator(reserveIterator);
      while (reserveIterator.hasNextCommit() && comparator.isBefore(newest, minimumCommit)) {
         reserveIterator.goToNextCommit();
         LOG.debug("Remaining: {} This: {}", reserveIterator.getTag(), this);
         reader.readVersion();
         newest = reader.getDependencies().getNewestCommit();
      }
   }
}