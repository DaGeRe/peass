package de.peass.confidence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Kopemedata.Testcases;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.clean.CleaningData;
import de.peass.clean.TestCleaner;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.measurement.analysis.MeasurementFileFinder;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.StatisticUtil;
import de.peass.measurement.analysis.statistics.DescribedChunk;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class GetConfidentData extends TestCleaner {

   @Option(names = { "-type1error", "--type1error" }, description = "Type 1 error of agnostic-t-test, i.e. probability of considering measurements equal when they are unequal")
   public double type1error = 0.01;
   
   @Option(names = { "-type2error", "--type2error" }, description = "Type 2 error of agnostic-t-test, i.e. probability of considering measurements unequal when they are equal")
   private double type2error = 0.01;


   private static final Logger LOG = LogManager.getLogger(GetConfidentData.class);

   public static void main(String[] args) throws ParseException, JAXBException, IOException {
      GetConfidentData command = new GetConfidentData();
      CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      CleaningData cleaner = new CleaningData(out, data);
      
      LOG.debug("Data: {}", cleaner.getDataValue().length);
      for (int i = 0; i < cleaner.getDataValue().length; i++) {
         final File dataFolder = cleaner.getDataValue()[i];
         final File projectNameFolder = dataFolder.getParentFile();

         File projectOutFolder = new File(cleaner.getOut(), projectNameFolder.getName());
         projectOutFolder.mkdirs();
         TestCleaner.getCommitOrder(dataFolder, projectNameFolder.getName());

         if (VersionComparator.hasVersions()) {
            for (File job : dataFolder.listFiles()) {
               File cleanFolder = new File(job, "peass" + File.separator + "clean");
               if (cleanFolder.exists()) {
                  for (File measurementFile : cleanFolder.listFiles()) {
                     if (measurementFile.getName().endsWith("xml")) {
                        ChunkSaver chunkSaver = new ChunkSaver(type1error, type2error, measurementFile, projectOutFolder );
                        chunkSaver.saveChunk();
                     }
                  }
               }
            }
         } else {
            LOG.error("No URL defined.");
         }
      }
      return null;
   }

   
}
