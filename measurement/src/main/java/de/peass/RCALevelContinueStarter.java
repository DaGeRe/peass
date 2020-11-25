package de.peass;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.CauseSearchFolders;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.LevelManager;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.searcher.CauseSearcher;
import de.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;
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
      if (resultFile != null) {
         final CauseSearchData data = Constants.OBJECTMAPPER.readValue(resultFile, CauseSearchData.class);

         final File nowFolder = new File(folders.getTempProjectFolder(), "continue");
         GitUtils.clone(folders, nowFolder);
         final CauseSearchFolders alternateFolders = new CauseSearchFolders(nowFolder);

         final JUnitTestTransformer testtransformer = new JUnitTestTransformer(alternateFolders.getProjectFolder(), data.getMeasurementConfig());

         final BothTreeReader reader = new BothTreeReader(data.getCauseConfig(), data.getMeasurementConfig(), folders);
         reader.readCachedTrees();

         final CauseTester measurer = new CauseTester(alternateFolders, testtransformer, data.getCauseConfig());
         final LevelCauseSearcher tester = new LevelCauseSearcher(measurer, data, alternateFolders);

         final List<CallTreeNode> currentVersionNodeList = new LinkedList<>();
         final List<CallTreeNode> currentPredecessorNodeList = new LinkedList<>();

         new LevelManager(currentVersionNodeList, currentPredecessorNodeList, reader).goToLastMeasuredLevel(data.getNodes());

         tester.isLevelDifferent(currentPredecessorNodeList, currentVersionNodeList);
      }

      return null;
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
}
