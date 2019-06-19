package de.peass.clean;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.utils.OptionConstants;
import de.peass.vcs.GitUtils;
import de.peran.FolderSearcher;

public class CleaningData {
   private static final Logger LOG = LogManager.getLogger(TestCleaner.class);

   private final String[] dataValue;
   private final File out;

   public CleaningData(String args[]) throws ParseException, JAXBException, IOException {
      final Options options = OptionConstants.createOptions(OptionConstants.OUT, OptionConstants.URL, OptionConstants.DEPENDENCYFILE);
      options.addOption(FolderSearcher.DATAOPTION);

      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      if (line.hasOption(OptionConstants.OUT.getName())) {
         out = new File(line.getOptionValue(OptionConstants.OUT.getName()));
      } else {
         out = null;
      }

      if (!line.hasOption(OptionConstants.URL.getName()) && !line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
         LOG.error(
               "You should pass either an URL or an dependencyfile, since the cleaner needs to know the commits order. If the project is contained in the default URLs, it will also work.");
         // System.exit(1);
      }

      if (line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
         DependencyReaderUtil.loadDependencies(line);
      }

      if (line.hasOption(OptionConstants.URL.getName())) {
         final String url = line.getOptionValue(OptionConstants.URL.getName());
         GitUtils.getCommitsForURL(url);
      }

      dataValue = line.getOptionValues(FolderSearcher.DATA);
      LOG.debug("Data: {}", dataValue.length);
   }

   public String[] getDataValue() {
      return dataValue;
   }

   public File getOut() {
      return out;
   }
}