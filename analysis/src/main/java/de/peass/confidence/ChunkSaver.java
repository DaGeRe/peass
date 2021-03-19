package de.peass.confidence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Kopemedata.Testcases;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.measurement.analysis.MeasurementFileFinder;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.statistics.DescribedChunk;
import de.peass.statistics.StatisticUtil;

/**
 * Saves chunks from cleaned data. Assumes that one clean-folder contains only one chunk (else, only one chunk is saved to cleaned data).
 * 
 * @author reichelt
 *
 */
public class ChunkSaver {

   private static final Logger LOG = LogManager.getLogger(ChunkSaver.class);

   private static BufferedWriter writerConfident, writerUnconfident, writerMultipleMeasurements;

   private final static File conficentChunks = new File("confident.txt");
   private final static File unConficentChunks = new File("unconfident.txt");
   private final static File multipleMeasurements = new File("multipleMeasurements.txt");

   static {
      if (conficentChunks.exists()) {
         conficentChunks.delete();
      }
      if (unConficentChunks.exists()) {
         unConficentChunks.delete();
      }
      if (multipleMeasurements.exists()) {
         multipleMeasurements.delete();
      }

      try {
         writerConfident = new BufferedWriter(new FileWriter(conficentChunks));
         writerUnconfident = new BufferedWriter(new FileWriter(unConficentChunks));
         writerMultipleMeasurements = new BufferedWriter(new FileWriter(multipleMeasurements));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   final double type1error;
   final double type2error;
   final File measurementFile;
   final File projectOutFolder;

   final Kopemedata kopemeData;
   final Testcases testcases;
   final String clazz;
   final TestcaseType testcaseType;
   final String method;

   public ChunkSaver(double type1error, double type2error, File measurementFile, File projectOutFolder) throws JAXBException {
      super();
      this.type1error = type1error;
      this.type2error = type2error;
      this.projectOutFolder = projectOutFolder;
      this.measurementFile = measurementFile;

      kopemeData = XMLDataLoader.loadData(measurementFile);
      testcases = kopemeData.getTestcases();
      clazz = testcases.getClazz();
      testcaseType = testcases.getTestcase().get(0);
      method = testcaseType.getName();
   }

   public boolean checkChunk(Chunk currentChunk) {
      if (currentChunk.getResult().size() > 60) {
         String versionOld = currentChunk.getResult().get(0).getVersion().getGitversion();
         String versionNew = currentChunk.getResult().get(currentChunk.getResult().size() - 1).getVersion().getGitversion();
         DescribedChunk described = new DescribedChunk(currentChunk, versionOld, versionNew);
         Relation relation = StatisticUtil.agnosticTTest(described.getDescPrevious(), described.getDescCurrent(), type1error, type2error);
         LOG.info("Relation: {} Size: {}", relation, currentChunk.getResult().size());
         if (relation == Relation.UNEQUAL || relation == Relation.EQUAL) {
            try {
               writerConfident.write(measurementFile.getParentFile().getParentFile().getParentFile().getAbsolutePath() + "\n");
               writerConfident.flush();
            } catch (IOException e) {
               e.printStackTrace();
            }
            return true;
         } else {
            try {
               writerUnconfident.write(measurementFile.getParentFile().getParentFile().getParentFile().getAbsolutePath() + "\n");
               writerUnconfident.flush();
            } catch (IOException e) {
               e.printStackTrace();
            }
            return false;
         }
      } else {
         try {
            writerUnconfident.write(measurementFile.getParentFile().getParentFile().getParentFile().getAbsolutePath() + "\n");
            writerUnconfident.flush();
         } catch (IOException e) {
            e.printStackTrace();
         }
         return false;
      }
   }

   private void saveChunk(Chunk currentChunk) throws JAXBException {
      final MeasurementFileFinder finder = new MeasurementFileFinder(projectOutFolder, clazz, method);
      final File goal = finder.getMeasurementFile();
      final Kopemedata oneResultData = finder.getOneResultData();
      Datacollector datacollector = finder.getDataCollector();

      checkMultipleMeasurements(currentChunk, datacollector);

      datacollector.getChunk().add(currentChunk);
      XMLDataStorer.storeData(goal, oneResultData);
   }

   private void checkMultipleMeasurements(Chunk currentChunk, Datacollector datacollector) {
      String[] currentVersions = KoPeMeDataHelper.getVersions(currentChunk);
      for (Chunk chunk : datacollector.getChunk()) {
         String[] versions = KoPeMeDataHelper.getVersions(chunk);
         if (versions[1] != null) {
            if (versions[1].equals(currentVersions[0])) {
               LOG.debug("Version {} already measured for {}#{}", versions[1], clazz, method);
               try {
                  writerMultipleMeasurements.write(clazz + ";" + method + ";" + measurementFile);
                  writerMultipleMeasurements.flush();
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
         }
      }
   }

   public void saveChunk() throws JAXBException {
      Chunk currentChunk = testcaseType.getDatacollector().get(0).getChunk().get(0);
      if (checkChunk(currentChunk)) {
         saveChunk(currentChunk);
      }
   }
}
