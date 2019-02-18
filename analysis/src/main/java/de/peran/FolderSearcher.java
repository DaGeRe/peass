package de.peran;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.utils.OptionConstants;
import de.peran.measurement.analysis.AnalyseFullData;
import de.peran.measurement.analysis.StatisticInfo;

/**
 * Analyzes data from all subfolders of one folder. It is assumed that the typical PeASS-folder-structure is given.
 * 
 * @author reichelt
 *
 */
public class FolderSearcher {
   private static final Logger LOG = LogManager.getLogger(FolderSearcher.class);

   public final static ObjectMapper MAPPER = new ObjectMapper();
   static {
      MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
   }
   
   public static final String DATA = "data";

   public static final Option DATAOPTION = Option.builder(DATA).required(true).hasArgs()
         .desc("Daten der zu analysierenden Ergebnisdaten bzw. Ergebnisdateien-Ordner").build();

   public static void main(final String[] args) throws ParseException, JAXBException, InterruptedException, JsonGenerationException, JsonMappingException, IOException {
      final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);
      options.addOption(DATAOPTION);

      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      DependencyReaderUtil.loadDependencies(line);

      final StatisticInfo info = new StatisticInfo();

      final AnalyseFullData afd = new AnalyseFullData(info);
      for (int i = 0; i < line.getOptionValues(DATA).length; i++) {
         final File folder = new File(line.getOptionValues(DATA)[i]);
         LOG.info("Searching in " + folder);
         afd.processDataFolder(folder);
         MAPPER.writeValue(new File("results/statistics.json"), info);
      }
      LOG.info("Versions: {} Testcases: {} Changes: {}", afd.versions.size(), afd.testcases, afd.getChanges());

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

   }

}
