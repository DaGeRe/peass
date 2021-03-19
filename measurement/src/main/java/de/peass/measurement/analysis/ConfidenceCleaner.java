package de.peass.measurement.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.measurement.analysis.statistics.DescribedChunk;
import de.peass.measurement.analysis.statistics.EvaluationPair;
import de.peass.measurement.analysis.statistics.TestData;
import de.peass.statistics.StatisticUtil;

/**
 * Creates clean data based on measurement data containing all values, but only takes into account measurements where we can say by a given alpha and beta error, that the
 * measurements are equal or are not equal - so data where an agnostic decision, that the result is unknown, are omitted
 * 
 * @author reichelt
 *
 */
public class ConfidenceCleaner extends Cleaner {

   private final static File conficentChunks = new File("confident.txt");
   private final static File unConficentChunks = new File("unconfident.txt");
   private static BufferedWriter writer, writerUnconfident;

   static {
      if (conficentChunks.exists()) {
         conficentChunks.delete();
      }
      if (unConficentChunks.exists()) {
         unConficentChunks.delete();
      }
      
      try {
         writer = new BufferedWriter(new FileWriter(conficentChunks));
         writerUnconfident = new BufferedWriter(new FileWriter(unConficentChunks));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public double type1error = 0.01, type2error = 0.01;
   private File currentOrigin;

   public ConfidenceCleaner(File measurementsFull, double type1error, double type2error) {
      super(measurementsFull);
      this.type1error = type1error;
      this.type2error = type2error;
   }
   
   @Override
   public void processTestdata(final TestData measurementEntry) {
      currentOrigin = measurementEntry.getOrigin();
      for (final Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
         read++;
         cleanTestVersionPair(entry);
      }
   }

   @Override
   public boolean checkChunk(Chunk currentChunk) {
      if (currentChunk.getResult().size() > 2) {
         String versionOld = currentChunk.getResult().get(0).getVersion().getGitversion();
         String versionNew = currentChunk.getResult().get(currentChunk.getResult().size() - 1).getVersion().getGitversion();
         DescribedChunk described = new DescribedChunk(currentChunk, versionOld, versionNew);
         Relation relation = StatisticUtil.agnosticTTest(described.getDescPrevious(), described.getDescCurrent(), type1error, type2error);
         if (relation == Relation.UNEQUAL || relation == Relation.EQUAL) {
            try {
               writer.write(currentOrigin.getAbsolutePath() + "\n");
               writer.flush();
            } catch (IOException e) {
               e.printStackTrace();
            }
            return true;
         } else {
            try {
               writerUnconfident.write(currentOrigin.getAbsolutePath() + "\n");
               writerUnconfident.flush();
            } catch (IOException e) {
               e.printStackTrace();
            }
            return false;
         }
      } else {
         return false;
      }
   }

}
