package de.peass;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.traces.ViewGenerator;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.OptionConstants;
import de.peass.vcs.GitCommit;

/**
 * First reads all dependencies and afterwards determines the views and creates the execution file. Both is parallelized. This is the class that should be used if a project as a
 * whole should be analyzed.
 * 
 * @author reichelt
 *
 */
public class DependencyExecutionReader {

   private static final Logger LOG = LogManager.getLogger(DependencyExecutionReader.class);

   public static void main(final String[] args) {
      try {
         final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.OUT,
               OptionConstants.TIMEOUT, OptionConstants.THREADS);

         final CommandLineParser parser = new DefaultParser();
         final CommandLine line = parser.parse(options, args);

         final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
         if (!projectFolder.exists()) {
            throw new RuntimeException("Folder " + projectFolder.getAbsolutePath() + " does not exist.");
         }
         final String project = projectFolder.getName();

         final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(line, projectFolder);
         VersionComparator.setVersions(commits);

         final int timeout = Integer.parseInt(line.getOptionValue(OptionConstants.TIMEOUT.getName(), "5"));
         final int threads = Integer.parseInt(line.getOptionValue(OptionConstants.THREADS.getName(), "4"));

         final File resultBaseFolder = new File(line.getOptionValue(OptionConstants.OUT.getName(), "results"));
         final File[] outFiles = DependencyParallelReader.getDependencies(projectFolder, resultBaseFolder, project, commits, timeout, threads);

         LOG.debug("Files: {}", outFiles);

         final File out = new File(resultBaseFolder, "deps_" + project + ".json");
         final Dependencies all = DependencyParallelReader.mergeVersions(out, outFiles);

         final PeASSFolders folders = new PeASSFolders(projectFolder);
         final File dependencyTempFiles = new File(folders.getTempProjectFolder().getParentFile(), "dependencyTempFiles");
         folders.getTempProjectFolder().renameTo(dependencyTempFiles);

         final File executeOut = new File(resultBaseFolder, "execute_" + project + ".json");
         final File viewFolder = new File(resultBaseFolder, "views_" + project);

         final ViewGenerator viewGenerator = new ViewGenerator(projectFolder, all, executeOut, viewFolder, threads, timeout);
         viewGenerator.processCommandline();
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }
}
