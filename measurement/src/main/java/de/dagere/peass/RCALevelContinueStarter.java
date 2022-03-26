package de.dagere.peass;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CausePersistenceManager;
import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.LevelManager;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Continues root-cause-analysis based on existing treefile", name = "continuerca")
public class RCALevelContinueStarter implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(RCALevelContinueStarter.class);

   @Option(names = { "-folder", "--folder" }, description = "Folder of the project that should be analyzed", required = true)
   protected File projectFolder;

   public static void main(final String[] args) {
      final RCALevelContinueStarter command = new RCALevelContinueStarter();
      final CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      final CauseSearchFolders folders = new CauseSearchFolders(projectFolder);
      final File resultFile = getResultFile(folders);
      final File resultFileFull = getFullResultFile(folders);
      if (resultFile != null && resultFileFull != null) {
         final CauseSearchData data = Constants.OBJECTMAPPER.readValue(resultFile, CauseSearchData.class);
         final CauseSearchData dataFull = Constants.OBJECTMAPPER.readValue(resultFileFull, CauseSearchData.class);
         
         final CauseSearchFolders alternateFolders = createAlternateFolders(folders);

         MeasurementConfig measurementConfig = data.getMeasurementConfig();
         final BothTreeReader reader = new BothTreeReader(data.getCauseConfig(), measurementConfig, folders, new EnvironmentVariables(measurementConfig.getExecutionConfig().getProperties()));
         reader.readCachedTrees();
         
         CausePersistenceManager persistenceManager = new CausePersistenceManager(data, dataFull, alternateFolders);

         EnvironmentVariables emptyEnv = new EnvironmentVariables();
         final CauseTester measurer = new CauseTester(alternateFolders, measurementConfig, data.getCauseConfig(), emptyEnv);
         final LevelCauseSearcher tester = new LevelCauseSearcher(measurer, persistenceManager, emptyEnv);

         final List<CallTreeNode> currentVersionNodeList = new LinkedList<>();
         final List<CallTreeNode> currentPredecessorNodeList = new LinkedList<>();

         new LevelManager(currentVersionNodeList, currentPredecessorNodeList, reader).goToLastMeasuredLevel(data.getNodes());

         tester.isLevelDifferent(currentPredecessorNodeList, currentVersionNodeList);
      }

      return null;
   }

   private CauseSearchFolders createAlternateFolders(final CauseSearchFolders folders) throws InterruptedException, IOException {
      final File nowFolder = new File(folders.getTempProjectFolder(), "continue");
      GitUtils.clone(folders, nowFolder);
      final CauseSearchFolders alternateFolders = new CauseSearchFolders(nowFolder);
      return alternateFolders;
   }

   private File getResultFile(final CauseSearchFolders folders) {
      File resultFile = null;
      for (final File versionFolder : folders.getRcaTreeFolder().listFiles()) {
         for (final File testcaseFolder : versionFolder.listFiles()) {
            for (final File treeFile : testcaseFolder.listFiles()) {
               if (treeFile.getName().endsWith(".json")) {
                  resultFile = treeFile;
               }
            }
         }
      }
      return resultFile;
   }
   
   private File getFullResultFile(final CauseSearchFolders folders) {
      File resultFile = null;
      for (final File versionFolder : folders.getRcaTreeFolder().listFiles()) {
         for (final File testcaseFolder : versionFolder.listFiles()) {
            File detailsFolder = new File(testcaseFolder, "details");
            for (final File treeFile : detailsFolder.listFiles()) {
               if (treeFile.getName().endsWith(".json")) {
                  resultFile = treeFile;
               }
            }
         }
      }
      return resultFile;
   }
}
