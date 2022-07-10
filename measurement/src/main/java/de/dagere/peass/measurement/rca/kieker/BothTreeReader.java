package de.dagere.peass.measurement.rca.kieker;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.treeanalysis.TreeUtil;
import de.dagere.peass.utils.Constants;

public class BothTreeReader {

   private static final Logger LOG = LogManager.getLogger(BothTreeReader.class);

   private CallTreeNode rootPredecessor;
   private CallTreeNode rootVersion;

   private final CauseSearcherConfig causeSearchConfig;
   private final MeasurementConfig config;
   private final CauseSearchFolders folders;
   private final EnvironmentVariables env;

   final File potentialCacheFileOld;
   final File potentialCacheFile;

   public BothTreeReader(final CauseSearcherConfig causeSearchConfig, final MeasurementConfig config, final CauseSearchFolders folders, final EnvironmentVariables env) {
      this.causeSearchConfig = causeSearchConfig;
      this.config = config;
      this.folders = folders;
      this.env = env;

      File treeCacheFolder = folders.getTreeCacheFolder(config.getFixedCommitConfig().getCommit(), causeSearchConfig.getTestCase());
      potentialCacheFileOld = new File(treeCacheFolder, config.getFixedCommitConfig().getCommitOld());
      potentialCacheFile = new File(treeCacheFolder, config.getFixedCommitConfig().getCommit());
   }

   public void readCachedTrees() {
      if (!potentialCacheFile.exists()) {
         throw new RuntimeException("Cache not existing! " + potentialCacheFile.getAbsolutePath());
      }
      if (!potentialCacheFileOld.exists()) {
         throw new RuntimeException("Cache not existing! " + potentialCacheFileOld.getAbsolutePath());
      }
      try {
         rootPredecessor = Constants.OBJECTMAPPER.readValue(potentialCacheFileOld, CallTreeNode.class);
         rootVersion = Constants.OBJECTMAPPER.readValue(potentialCacheFile, CallTreeNode.class);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      rootPredecessor.setConfig(config);
      setConfig(rootPredecessor);
      rootVersion.setConfig(config);
      setConfig(rootVersion);
   }

   private void setConfig(final CallTreeNode node) {
      for (CallTreeNode child : node.getChildren()) {
         child.setConfig(config);
         setConfig(child);
      }
   }

   public void readTrees() {
      if (potentialCacheFile.exists() && potentialCacheFileOld.exists()) {
         LOG.info("Using cache!");
         readCachedTrees();
      } else {
         determineTrees();
         LOG.info("Writing to cache");
         try {
            Constants.OBJECTMAPPER.writeValue(potentialCacheFileOld, rootPredecessor);
            Constants.OBJECTMAPPER.writeValue(potentialCacheFile, rootVersion);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }
   }

   private void determineTrees() {
      final TreeReader resultsManager = TreeReaderFactory.createTreeReader(folders, config.getFixedCommitConfig().getCommitOld(), config, causeSearchConfig.isIgnoreEOIs(), env);
      rootPredecessor = resultsManager.getTree(causeSearchConfig.getTestCase(), config.getFixedCommitConfig().getCommitOld());

      final TreeReader resultsManagerPrevious = TreeReaderFactory.createTreeReader(folders, config.getFixedCommitConfig().getCommit(), config, causeSearchConfig.isIgnoreEOIs(),
            env);
      rootVersion = resultsManagerPrevious.getTree(causeSearchConfig.getTestCase(), config.getFixedCommitConfig().getCommit());
      LOG.info("Traces equal: {}", TreeUtil.areTracesEqual(rootPredecessor, rootVersion));
   }

   public CallTreeNode getRootPredecessor() {
      return rootPredecessor;
   }

   public CallTreeNode getRootVersion() {
      return rootVersion;
   }

   public EnvironmentVariables getEnv() {
      return env;
   }
}
