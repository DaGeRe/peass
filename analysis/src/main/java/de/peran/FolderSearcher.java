package de.peran;

import org.apache.commons.cli.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Analyzes data from all subfolders of one folder. It is assumed that the typical PeASS-folder-structure is given.
 * 
 * @author reichelt
 *
 */
public class FolderSearcher {
   private static final Logger LOG = LogManager.getLogger(FolderSearcher.class);

   public static final String DATA = "data";

   public static final Option DATAOPTION = Option.builder(DATA).required(true).hasArgs()
         .desc("Data folders that should be analyzed").build();

//   public static void main(final String[] args) throws ParseException, JAXBException, InterruptedException, JsonGenerationException, JsonMappingException, IOException {
//      final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);
//      options.addOption(DATAOPTION);
//
//      final CommandLineParser parser = new DefaultParser();
//      final CommandLine line = parser.parse(options, args);
//
//      Cleaner.loadDependencies(line);
//
//      final ProjectStatistics info = new ProjectStatistics();
//
//      final AnalyseFullData afd = new AnalyseFullData(info);
//      for (int i = 0; i < line.getOptionValues(DATA).length; i++) {
//         final File folder = new File(line.getOptionValues(DATA)[i]);
//         LOG.info("Searching in " + folder);
//         afd.processDataFolder(folder);
//         MAPPER.writeValue(new File("results/statistics.json"), info);
//      }
//      LOG.info("Versions: {} Testcases: {} Changes: {}", afd.versions.size(), afd.testcases, afd.getChanges());

//      for (final Entry<String, Changes> entry : ProjectChanges.getOldChanges().getVersionChanges().entrySet()) {
//         final Changes newChanges = afd.knowledge.getVersion(entry.getKey());
//         if (newChanges == null) {
//         } else {
//            for (final Entry<String, List<Change>> changeTests : entry.getValue().getTestcaseChanges().entrySet()) {
//               final List<Change> clazzChanges = newChanges.getTestcaseChanges().get(changeTests.getKey());
//               if (clazzChanges == null) {
//                  LOG.debug("Test not found: {}", changeTests.getKey());
//               } else {
//                  for (final Change change : changeTests.getValue()) {
//                     boolean found = false;
//                     for (final Change newChange : clazzChanges) {
//                        if (newChange.getDiff().equals(change.getDiff())) {
//                           found = true;
//                        }
//                     }
//
//                     if (!found) {
//                        LOG.debug("Entry not found: {}", change.getDiff());
//                     } else {
//                        LOG.debug("Entry found: {}", change.getDiff());
//                     }
//                  }
//               }
//            }
//         }
//      }

//   }

}
