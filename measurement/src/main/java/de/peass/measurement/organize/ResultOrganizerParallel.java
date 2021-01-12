package de.peass.measurement.organize;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;

public class ResultOrganizerParallel extends ResultOrganizer {
   
   private static final Logger LOG = LogManager.getLogger(ResultOrganizerParallel.class);

   private Map<String, PeASSFolders> sourceFolders = new HashMap<>();
   
   public ResultOrganizerParallel(PeASSFolders folders, String currentVersion, long currentChunkStart, boolean isUseKieker, boolean saveAll, TestCase test,
         int expectedIterations) {
      super(folders, currentVersion, currentChunkStart, isUseKieker, saveAll, test, expectedIterations);
   }

   public void setFolder(String version, PeASSFolders versionTempFolder) {
      sourceFolders.put(version, versionTempFolder);
   }
   
   @Override
   public File getTempResultsFolder(final String version) {
      PeASSFolders currentFolders = sourceFolders.get(version);
      LOG.info("Searching method: {}", testcase);
      final String expectedFolderName = "*" + testcase.getClazz();
      final Collection<File> folderCandidates = findFolder(currentFolders.getTempMeasurementFolder(), new WildcardFileFilter(expectedFolderName));
      if (folderCandidates.size() != 1) {
         LOG.error("Folder with name {} is existing {} times.", expectedFolderName, folderCandidates.size());
         LOG.error("Searched in: {}", currentFolders.getTempMeasurementFolder());
         return null;
      } else {
         final File folder = folderCandidates.iterator().next();
         return folder;
      }
   }
}
