package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.measurement.analysis.Cleaner;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.utils.Constants;
import de.peass.utils.OptionConstants;
import de.peass.vcs.GitUtils;
import de.peran.FolderSearcher;

public class TestCleaner {


   private static final Logger LOG = LogManager.getLogger(TestCleaner.class);

   public static void main(final String[] args) throws ParseException, JAXBException, IOException {
      final Options options = OptionConstants.createOptions(OptionConstants.OUT, OptionConstants.URL, OptionConstants.DEPENDENCYFILE);
      options.addOption(FolderSearcher.DATAOPTION);

      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      File out;
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

      LOG.debug("Data: {}", line.getOptionValues(FolderSearcher.DATA).length);
      for (int i = 0; i < line.getOptionValues(FolderSearcher.DATA).length; i++) {
         final File dataFolder = new File(line.getOptionValues(FolderSearcher.DATA)[i]);
         final File projectNameFolder = dataFolder.getParentFile();
         getCommitOrder(dataFolder, projectNameFolder.getName());

         if (VersionComparator.hasVersions()) {
            LOG.info("Searching in " + dataFolder);
            final File cleanFolder, fulldataFolder;
            if (out == null) {
               cleanFolder = new File(projectNameFolder, "clean");
               cleanFolder.mkdirs();
               final File chunkFolder = new File(cleanFolder, dataFolder.getName());
               chunkFolder.mkdirs();
               fulldataFolder = new File(chunkFolder, "measurementsFull");
            } else {
               cleanFolder = out;
               fulldataFolder = new File(cleanFolder, projectNameFolder.getName());
            }

            final Cleaner transformer = new Cleaner(fulldataFolder);
            LOG.info("Start: " + dataFolder.getAbsolutePath());
            transformer.processDataFolder(dataFolder);
            LOG.info("Finish, read: " + transformer.getRead() + " correct: " + transformer.getCorrect());
         } else {
            LOG.error("No URL defined.");
         }
      }
   }

   static void getCommitOrder(final File dataFolder, final String projectName) throws JAXBException, IOException {
      if (System.getenv(Constants.PEASS_REPOS) != null) {
         final String repofolder = System.getenv(Constants.PEASS_REPOS);
         final File folder = new File(repofolder);
         if (folder.exists()) {
            final File dependencyFile = new File(folder, "dependencies-final" + File.separator + "deps_" + projectName + ".json");
            final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
            VersionComparator.setDependencies(dependencies);
         }
      }
      if (!VersionComparator.hasVersions()) {
         final String url = Constants.defaultUrls.get(projectName);
         LOG.debug("Analyzing: {} Name: {} URL: {}", dataFolder.getAbsolutePath(), projectName, url);
         if (url != null) {
            GitUtils.getCommitsForURL(url);
         }
      }
   }

}
