package de.dagere.peass.clean;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CleaningData {
   private static final Logger LOG = LogManager.getLogger(TestCleaner.class);

   private final File[] dataValue;
   private final File out;
   
   public CleaningData(File out, File[] dataValue) {
      this.out = out;
      this.dataValue = dataValue;
   }

//   public CleaningData(String args[]) throws ParseException, JAXBException, IOException {
//      final Options options = OptionConstants.createOptions(OptionConstants.OUT, OptionConstants.URL, OptionConstants.DEPENDENCYFILE);
//      options.addOption(FolderSearcher.DATAOPTION);
//
//      final CommandLineParser parser = new DefaultParser();
//      final CommandLine line = parser.parse(options, args);
//
//      if (line.hasOption(OptionConstants.OUT.getName())) {
//         out = new File(line.getOptionValue(OptionConstants.OUT.getName()));
//      } else {
//         out = null;
//      }
//
//      if (!line.hasOption(OptionConstants.URL.getName()) && !line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
//         LOG.error(
//               "You should pass either an URL or an dependencyfile, since the cleaner needs to know the commits order. If the project is contained in the default URLs, it will also work.");
//         // System.exit(1);
//      }
//
//      if (line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
//         DependencyReaderUtil.loadDependencies(line);
//      }
//
//      if (line.hasOption(OptionConstants.URL.getName())) {
//         final String url = line.getOptionValue(OptionConstants.URL.getName());
//         GitUtils.getCommitsForURL(url);
//      }
//
//      dataValue = line.getOptionValues(FolderSearcher.DATA);
//      LOG.debug("Data: {}", dataValue.length);
//   }

   public File[] getDataValue() {
      return dataValue;
   }

   public File getOut() {
      return out;
   }
}