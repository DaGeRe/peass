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

import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.measurement.analysis.Cleaner;
import de.peass.utils.OptionConstants;
import de.peass.vcs.GitUtils;
import de.peran.FolderSearcher;

public class TestCleaner {

   public static Map<String, String> defaultUrls = new HashMap<>();

   static {
      // Chunk 1
      defaultUrls.put("commons-compress", "https://github.com/apache/commons-compress.git");
      defaultUrls.put("commons-csv", "https://github.com/apache/commons-csv.git");
      defaultUrls.put("commons-dbcp", "https://github.com/apache/commons-dbcp.git");
      defaultUrls.put("commons-fileupload", "https://github.com/apache/commons-fileupload.git");
      defaultUrls.put("commons-imaging", "https://github.com/apache/commons-imaging.git");
      defaultUrls.put("commons-io", "https://github.com/apache/commons-io.git");
      defaultUrls.put("commons-text", "https://github.com/apache/commons-text.git");
      
      // Chunk 2
      defaultUrls.put("commons-pool", "https://github.com/apache/commons-pool.git");
      defaultUrls.put("commons-numbers", "https://github.com/apache/commons-numbers.git");
      defaultUrls.put("commons-jcs", "https://github.com/apache/commons-jcs.git");
      defaultUrls.put("httpcomponents-core", "https://github.com/apache/httpcomponents-core.git");
      defaultUrls.put("k-9", "https://github.com/k9mail/k-9.git");

      // Future candidates
      defaultUrls.put("commons-math", "https://github.com/apache/commons-math.git");
      defaultUrls.put("commons-lang", "https://github.com/apache/commons-lang.git");
      defaultUrls.put("jackson-core", "https://github.com/FasterXML/jackson-core.git");
      defaultUrls.put("spring-framework", "https://github.com/spring-projects/spring-framework.git");
      defaultUrls.put("maven", "https://github.com/apache/maven.git");
      defaultUrls.put("okhttp", "https://github.com/square/okhttp.git");
   }

   private static final Logger LOG = LogManager.getLogger(Cleaner.class);

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
         LOG.error("You should pass either an URL or an dependencyfile, since the cleaner needs to know the commits order. If the project is contained in the default URLs, it will also work.");
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
         final String url = defaultUrls.get(projectNameFolder.getName());
         LOG.debug("Analyzing: {} Name: {} URL: {}", dataFolder.getAbsolutePath(), projectNameFolder.getName(), url);
         if (url != null) {
            GitUtils.getCommitsForURL(url);
         }

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
            LOG.info("Start");
            transformer.processDataFolder(dataFolder);
            LOG.info("Finish");
         } else {
            LOG.error("No URL defined.");
         }
      }
   }
  
}
