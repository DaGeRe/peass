package de.peass.ci.helper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.utils.StreamGobbler;

/**
 * Builds a git project which can be used for integration testing 
 * @author reichelt
 *
 */
public class GitProjectBuilder {
   
   private static final Logger LOG = LogManager.getLogger(GitProjectBuilder.class);
   
   private final File gitFolder;
   private final List<String> tags = new LinkedList<>();
   
   public GitProjectBuilder(final File destination, final File firstVersionFolder) throws InterruptedException, IOException {
      this.gitFolder = destination;
      if (!gitFolder.exists()) {
         gitFolder.mkdirs();
      }
      
      final Process initProcess = Runtime.getRuntime().exec("git init", new String[0], destination);
      String initOutput = StreamGobbler.getFullProcess(initProcess, false);
      LOG.debug("Init output: {}", initOutput);
      
      configLocalUser(destination);

      addVersion(firstVersionFolder, "Initial Commit");
   }

   private void configLocalUser(final File destination) throws IOException {
      final Process setEmailProcess = Runtime.getRuntime().exec("git config user.email \"test@peass.github.com\"", new String[0], destination);
      String setEmailOutput = StreamGobbler.getFullProcess(setEmailProcess, false);
      LOG.debug("setEmailOutput output: {}", setEmailOutput);
      final Process setUserProcess = Runtime.getRuntime().exec("git config user.name \"tester\"", new String[0], destination);
      String setUserOutput = StreamGobbler.getFullProcess(setUserProcess, false);
      LOG.debug("setUserOutput output: {}", setUserOutput);
   }
   
   /**
    * Adds a version to the project, replacing all contents of the repository with the contents of the given folder
    * @param version
    * @throws IOException 
    * @throws InterruptedException 
    */
   public String addVersion(final File versionFolder, final String commitMessage) throws IOException, InterruptedException {
      FileUtils.copyDirectory(versionFolder, gitFolder);
      
      final Process addProcess = Runtime.getRuntime().exec("git add -A", new String[0], gitFolder);
      String addOutput = StreamGobbler.getFullProcess(addProcess, false);
      LOG.debug("Add output: {}", addOutput);
      
      ProcessBuilder processBuilder = new ProcessBuilder("git", "commit", "-m", commitMessage);
      processBuilder.directory(gitFolder);
      final Process commitProcess = processBuilder.start();
      String commitOutput = StreamGobbler.getFullProcess(commitProcess, false);
      LOG.debug("Commit output: {}", commitOutput);
      
      final Process headProcess = Runtime.getRuntime().exec("git rev-parse HEAD", new String[0], gitFolder);
      String headOutput = StreamGobbler.getFullProcess(headProcess, false).replaceAll("\n", "");
      tags.add(headOutput);
      
      return headOutput;
   }
   
   public List<String> getTags() {
      return tags;
   }
}
