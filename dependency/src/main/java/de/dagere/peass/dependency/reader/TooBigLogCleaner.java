package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.PeassFolders;

public class TooBigLogCleaner {
   public static final int MAX_SIZE_IN_MB = 10;

   public static void cleanXMLFolder(final PeassFolders folders) throws FileNotFoundException, IOException, XmlPullParserException {
      final File xmlFileFolder = KiekerResultManager.getXMLFileFolder(folders, folders.getProjectFolder());
      if (xmlFileFolder != null) {
         FileUtils.deleteDirectory(xmlFileFolder);
      }
   }
   
   public static void cleanTooBigLogs(final PeassFolders folders, final String version) {
      File logFolder = folders.getLogFolder();
      File versionFolder = new File(logFolder, version);
      if (versionFolder.exists()) {
         for (File clazzFolder : versionFolder.listFiles()) {
            if (clazzFolder.isDirectory()) {
               for (File methodLog : clazzFolder.listFiles()) {
                  long sizeInMb = (methodLog.length() / (1024 * 1024));
                  if (sizeInMb > MAX_SIZE_IN_MB) {
                     methodLog.delete();
                  }
               }
            }
         }
      }
   }
}
