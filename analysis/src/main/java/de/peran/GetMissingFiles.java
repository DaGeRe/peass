package de.peran;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;

public class GetMissingFiles {

   public static String getServer(String name) {
      if (name.equals("1.sh")) {
         return "r146";
      } else if (name.equals("2.sh")) {
         return "r147";
      } else if (name.equals("3.sh")) {
         return "r149";
      } else if (name.equals("4.sh")) {
         return "r151";
      } else if (name.equals("5.sh")) {
         return "r153";
      }
      return null;
   }

   public static void main(final String[] args) throws IOException, JAXBException {
      final File commandFolder = new File("../measurement/scripts/versions/helpers");
      final File folder = new File("../measurement/scripts/versions/sync");
      for (File file : commandFolder.listFiles()) {
         if (file.getName().matches("[1-5].sh")) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
               String line;
               while ((line = reader.readLine()) != null) {
                  String[] parts = line.split(" ");
                  if (parts.length > 17) {
                     String measurementFile = parts[parts.length - 1];
                     System.out.println(measurementFile);
                     String version = parts[17];
                     String test = parts[5];
                     String testMethod = test.substring(test.lastIndexOf("#")+1);
                     String folderName = getServer(file.getName());
                     

                     File logFile = new File(folder, folderName + File.separator + "logs" + File.separator + version + File.separator+testMethod);
                     if (!logFile.exists()) {
                        System.out.println("Missing: " + logFile.getAbsolutePath());
                     }
                  }
               }
            }
         }
      }

      int missing = 0;
      int chunks = 0;
      int loggedtests = 0;
      for (final File computerFolder : folder.listFiles()) {
         if (computerFolder.isDirectory() && computerFolder.getName().startsWith("r")) {
            final File measurementsFullFolder = new File(computerFolder, "measurementsFull");
            for (final File measurementFile : measurementsFullFolder.listFiles()) {
               if (measurementFile.getName().endsWith(".xml")) {
                  final Kopemedata data = new XMLDataLoader(measurementFile).getFullData();
                  for (final Chunk chunk : data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getChunk()) {
                     chunks++;
                     if (chunk.getResult().size() < 2) {
                        System.out.println("Missing data: " + measurementFile.getName());
                        missing++;
                     }
                  }
               }
            }

            final File logDir = new File(computerFolder, "logs");
            for (File versionFile : logDir.listFiles()) {
               for (File testcaseDir : versionFile.listFiles()) {
                  System.out.println(testcaseDir.getAbsolutePath());
                  loggedtests++;
               }
            }

            // for (final File logfile : FileUtils.listFiles(logDir, new RegexFileFilter(".*txt"), TrueFileFilter.INSTANCE)) {
            // if (!logfile.getName().equals("log_compilation.txt")) {
            //
            // }
            // System.out.println("LOg: " + logfile.getAbsolutePath());
            // }

         }
         System.out.println("Logged: " + loggedtests);
      }
      System.out.println("Incomplete chunks: " + missing + " Chunks: " + chunks);
      System.out.println("Logged: " + loggedtests);
   }
}
