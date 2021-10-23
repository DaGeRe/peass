package de.dagere.peass.measurement.organize;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;

public class ResultOrganizerParallel extends ResultOrganizer {
   
   private static final Logger LOG = LogManager.getLogger(ResultOrganizerParallel.class);

   private final Map<String, PeassFolders> sourceFolders = new HashMap<>();
   
   public ResultOrganizerParallel(final PeassFolders folders, final String currentVersion, final long currentChunkStart, final boolean isUseKieker, final boolean saveAll, final TestCase test,
         final int expectedIterations) {
      super(folders, currentVersion, currentChunkStart, isUseKieker, saveAll, test, expectedIterations);
      LOG.debug("Creating new ResultOrganizerParallel");
      LOG.info("Instance: " + System.identityHashCode(this));
   }

   public void addVersionFolders(final String version, final PeassFolders versionTempFolder) {
      LOG.debug("Adding version: {}", version);
      sourceFolders.put(version, versionTempFolder);
      LOG.info("Instance: " + System.identityHashCode(this) + " Keys: " + sourceFolders.keySet());
   }
   
   @Override
   public File getTempResultsFolder(final String version) {
      PeassFolders currentFolders = sourceFolders.get(version);
      LOG.info("Searching method: {} Version: {} Existing versions: {}", testcase, version, sourceFolders.keySet());
      LOG.info("Instance: " + System.identityHashCode(this));
      final Collection<File> folderCandidates = currentFolders.findTempClazzFolder(testcase);
      if (folderCandidates.size() != 1) {
         LOG.error("Folder with name {} is existing {} times.", testcase.getClazz(), folderCandidates.size());
         return null;
      } else {
         final File folder = folderCandidates.iterator().next();
         return folder;
      }
   }
}
