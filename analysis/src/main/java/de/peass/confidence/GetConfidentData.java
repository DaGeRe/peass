package de.peass.confidence;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.clean.CleaningData;
import de.peass.clean.TestCleaner;
import de.peass.dependencyprocessors.VersionComparator;

public class GetConfidentData {
   
   private static final Logger LOG = LogManager.getLogger(GetConfidentData.class);
   
   public static void main(String[] args) throws ParseException, JAXBException, IOException {
      CleaningData data = new CleaningData(args);
      
      LOG.debug("Data: {}", data.getDataValue().length);
      for (int i = 0; i < data.getDataValue().length; i++) {
         final File dataFolder = new File(data.getDataValue()[i]);
         final File projectNameFolder = dataFolder.getParentFile();
         TestCleaner.getCommitOrder(dataFolder, projectNameFolder.getName());

         if (VersionComparator.hasVersions()) {
            TestCleaner.cleanFolderConfidence(data.getOut(), dataFolder, projectNameFolder, 0.01, 0.01);
         } else {
            LOG.error("No URL defined.");
         }
      }
   }
}
