package de.peass.measurement.searchcause.kieker;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.CauseSearcherConfig;
import de.peass.measurement.searchcause.TreeUtil;
import de.peass.measurement.searchcause.data.CallTreeNode;
import kieker.analysis.exception.AnalysisConfigurationException;

public class BothTreeReader {

   private static final Logger LOG = LogManager.getLogger(BothTreeReader.class);

   private CallTreeNode rootPredecessor;
   private CallTreeNode rootVersion;

   private final CauseSearcherConfig causeSearchConfig;
   private final MeasurementConfiguration config;
   private final PeASSFolders folders;

   public BothTreeReader(CauseSearcherConfig causeSearchConfig, MeasurementConfiguration config, PeASSFolders folders) {
      super();
      this.causeSearchConfig = causeSearchConfig;
      this.config = config;
      this.folders = folders;
   }

   public void readTrees() throws InterruptedException, IOException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException {
      final TreeReader resultsManager = TreeReaderFactory.createTreeReader(folders, causeSearchConfig.getPredecessor(), config.getTimeout());
      rootPredecessor = resultsManager.getTree(causeSearchConfig.getTestCase(), causeSearchConfig.getPredecessor());

      final TreeReader resultsManagerPrevious = TreeReaderFactory.createTreeReader(folders, causeSearchConfig.getVersion(), config.getTimeout());
      rootVersion = resultsManagerPrevious.getTree(causeSearchConfig.getTestCase(), causeSearchConfig.getVersion());
      LOG.info("Traces equal: {}", TreeUtil.areTracesEqual(rootPredecessor, rootVersion));
   }
   
   public CallTreeNode getRootPredecessor() {
      return rootPredecessor;
   }

   public CallTreeNode getRootVersion() {
      return rootVersion;
   }
}
