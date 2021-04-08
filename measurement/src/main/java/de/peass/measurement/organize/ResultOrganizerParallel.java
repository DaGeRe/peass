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

   private final Map<String, PeASSFolders> sourceFolders = new HashMap<>();
   
   public ResultOrganizerParallel(final PeASSFolders folders, final String currentVersion, final long currentChunkStart, final boolean isUseKieker, final boolean saveAll, final TestCase test,
         final int expectedIterations) {
      super(folders, currentVersion, currentChunkStart, isUseKieker, saveAll, test, expectedIterations);
      LOG.debug("Creating new ResultOrganizerParallel");
   }

   public void addVersionFolders(final String version, final PeASSFolders versionTempFolder) {
      LOG.debug("Adding version: {}", version);
      sourceFolders.put(version, versionTempFolder);
   }
   
   @Override
   public File getTempResultsFolder(final String version) {
      PeASSFolders currentFolders = sourceFolders.get(version);
      LOG.info("Searching method: {} Version: {} Existing versions: {}", testcase, version, sourceFolders.keySet());
      final String expectedFolderName = "*" + testcase.getClazz();
      final File containingFolder = currentFolders.getTempMeasurementFolder();
      final Collection<File> folderCandidates = findFolder(containingFolder, new WildcardFileFilter(expectedFolderName));
      if (folderCandidates.size() != 1) {
         LOG.error("Folder with name {} is existing {} times.", expectedFolderName, folderCandidates.size());
         LOG.error("Searched in: {}", containingFolder);
         return null;
      } else {
         final File folder = folderCandidates.iterator().next();
         return folder;
      }
   }
}
