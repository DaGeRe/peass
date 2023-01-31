/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.dagere.peass.vcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.CommitDiff;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.utils.StreamGobbler;

/**
 * Helps using git from java with CLI calls to git.
 * 
 * @author reichelt
 *
 */
public final class GitUtils {

   private static final Logger LOG = LogManager.getLogger(GitUtils.class);

   /**
    * Only utility-clazz, no instantiation needed.
    */
   private GitUtils() {

   }

   public static CommitComparatorInstance getCommitsForURL(final String url, final boolean linearizeHistory) throws IOException {
      CommitComparatorInstance comparator = null;
      boolean repoFound = false;
      if (System.getenv(Constants.PEASS_PROJECTS) != null) {
         System.out.println(url);
         String project = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf("."));
         File candidate = new File(System.getenv(Constants.PEASS_PROJECTS), project);
         if (candidate.exists()) {
            repoFound = true;
            final List<String> commits = GitUtils.getCommits(candidate, false, linearizeHistory);
            comparator = new CommitComparatorInstance(commits);
            VersionComparator.setVersions(commits);
         }
      }
      if (!repoFound && System.getenv(Constants.PEASS_REPOS) != null) {
         final String repofolderName = System.getenv(Constants.PEASS_REPOS);
         File repoFolder = new File(repofolderName);
         File dependencyFolder = new File(repoFolder, "dependencies-final");
         String project = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf('.'));
         File staticSelectionFile = new File(dependencyFolder, ResultsFolders.STATIC_SELECTION_PREFIX + project + ".json");
         LOG.debug("Searching: {}", staticSelectionFile);
         if (staticSelectionFile.exists()) {
            final StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(staticSelectionFile, StaticTestSelection.class);
            VersionComparator.setDependencies(dependencies);
            comparator = new CommitComparatorInstance(dependencies);
            repoFound = true;
         }
      }

      if (!repoFound) {
         final File tempDir = Files.createTempDirectory("gitTemp").toFile();
         GitUtils.downloadProject(url, tempDir);
         final List<String> commits = GitUtils.getCommits(tempDir, false, linearizeHistory);
         comparator = new CommitComparatorInstance(commits);
         VersionComparator.setVersions(commits);
         FileUtils.deleteDirectory(tempDir);
      }
      return comparator;
   }

   public static void clone(final PeassFolders folders, final File projectFolderTemp, final String gitCryptKey) throws IOException {
      // TODO Branches klonen
      final File projectFolder = folders.getProjectFolder();
      clone(projectFolderTemp, projectFolder, gitCryptKey);
   }

   private static void clone(final File projectFolderDest, final File projectFolderSource, final String gitCryptKey) throws IOException {
      if (projectFolderDest.exists()) {
         throw new RuntimeException("Can not clone to existing folder: " + projectFolderDest.getAbsolutePath());
      }
      FileUtils.copyDirectory(projectFolderSource, projectFolderDest);
   }

   /**
    * Downloads a project to the given folder
    * 
    * @param url Project-URL
    * @param folder Goal-folder for download
    */
   public static void downloadProject(final String url, final File folder) {
      try {
         ProcessBuilder pb = new ProcessBuilder("git", "clone", url, folder.getAbsolutePath());
         LOG.debug("Command: {}", pb.command());
         final Process p = pb.start();
         StreamGobbler.showFullProcess(p);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Removes all commits from a list that are before the given start commit or after the given end commit.
    * 
    * @param startcommit Commit to start
    * @param endcommit Commit to end
    * @param commits List of commits for filtering, sorted from older to newer
    */
   public static void filterList(final String startcommit, final String endcommit, final List<String> commits) {
      LOG.info("Count of Commits: {}", commits.size());
      boolean beforeStart = startcommit != null;
      boolean afterEnd = false;
      final List<String> notRelevantCommits = new LinkedList<>();
      for (final String commit : commits) {
         LOG.debug("Processing {} {} {}", commit, beforeStart, afterEnd);
         if (startcommit != null && commit.startsWith(startcommit)) {
            beforeStart = false;
         }
         if (beforeStart || afterEnd) {
            notRelevantCommits.add(commit);
         }
         if (endcommit != null && commit.startsWith(endcommit)) {
            afterEnd = true;
            if (beforeStart) {
               boolean startCommitExists = commits.stream().anyMatch(potentialStart -> potentialStart.startsWith(startcommit));
               if (startCommitExists) {
                  throw new RuntimeException("Startcommit " + startcommit + " after endcommit " + endcommit);
               } else {
                  throw new RuntimeException("Startcommit " + startcommit + " not found at all, but endcommit " + endcommit + " found");
               }
            }
         }
      }
      LOG.debug("Removing: {}", notRelevantCommits.size());
      commits.removeAll(notRelevantCommits);
   }

   public static List<String> getCommits(final File folder, final String startcommit, final String endcommit, final boolean linearizeHistory) {
      final List<String> commits = getCommits(folder, true, linearizeHistory);
      GitUtils.filterList(startcommit, endcommit, commits);
      return commits;
   }

   /**
    * Returns the commits of the git repo in ascending order.
    * 
    * @param folder
    * @return
    */
   public static List<String> getCommits(final File folder, final boolean includeAllBranches, final boolean linearizeHistory) {
      try {
         final List<String> commitNames;
         if (!linearizeHistory) {
            commitNames = getCommitNames(folder, includeAllBranches);
         } else {
            commitNames = getLinearCommitNames(folder);
         }
         return commitNames;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
   
   public static List<GitCommit> getCommitMetadata(File folder, List<String> commitNames) throws IOException{
      List<GitCommit> commits = getCommitsMetadata(folder, commitNames);
      return commits;
   }

   private static List<String> getLinearCommitNames(final File folder) {
      try {
         ProcessBuilder oldestCommitProcessbuilder = new ProcessBuilder("git", "log", "--reverse", "--pretty=tformat:%H");
         oldestCommitProcessbuilder.directory(folder);
         Process readOldestCommitProcess = oldestCommitProcessbuilder.start();
         String oldestCommit;
         try (final BufferedReader readOldestCommitInput = new BufferedReader(new InputStreamReader(readOldestCommitProcess.getInputStream()))) {
            oldestCommit = readOldestCommitInput.readLine().split(" ")[0];
         }

         List<String> ouputCommitList = new LinkedList<>();

         ProcessBuilder commitProcessbuilder = new ProcessBuilder("git", "rev-list", "--ancestry-path", "--children", oldestCommit + "..HEAD");
         commitProcessbuilder.directory(folder);
         final Process readCommitProcess = commitProcessbuilder.start();
         try (final BufferedReader readCommitInput = new BufferedReader(new InputStreamReader(readCommitProcess.getInputStream()))) {
            String lastChild = null;
            String line;

            while ((line = readCommitInput.readLine()) != null) {
               String[] ancestryHashes = line.split(" ");
               Set<String> hashes = new HashSet<>(Arrays.asList(ancestryHashes));
               if (lastChild == null || hashes.contains(lastChild)) {
                  ouputCommitList.add(0, ancestryHashes[0]);
                  lastChild = ancestryHashes[0];
               }
            }
         }
         ouputCommitList.add(0, oldestCommit);
         return ouputCommitList;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private static List<GitCommit> getCommitsMetadata(final File folder, final List<String> commitNames) throws IOException {
      final List<GitCommit> commits = new LinkedList<>();
      for (String commit : commitNames) {
         ProcessBuilder readCommitProcessBuilder = new ProcessBuilder("git", "log", "--date=iso", "-n", "1", commit);
         readCommitProcessBuilder.directory(folder);
         final Process readCommitProcess = readCommitProcessBuilder.start();
         try (final BufferedReader readCommitInput = new BufferedReader(new InputStreamReader(readCommitProcess.getInputStream()))) {
            String line = null;
            String author = null, date = null, message = "";
            while ((line = readCommitInput.readLine()) != null) {
               if (line.startsWith("Author:")) {
                  author = line.substring(8);
               }
               if (line.startsWith("Date: ")) {
                  date = line.substring(8);
               } else if (author != null && date != null) {
                  message += line + " ";
               }
            }
            final GitCommit gc = new GitCommit(commit, author, date, message);
            commits.add(gc);
         }
      }
      return commits;
   }

   private static List<String> getCommitNames(final File folder, final boolean includeAllBranches) throws IOException {
      String command = includeAllBranches ? "git log --oneline --all --no-abbrev-commit" : "git log --oneline --no-abbrev-commit";
      final Process p = Runtime.getRuntime().exec(command, new String[0], folder);
      try (final BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
         String line;
         List<String> commitNames = new LinkedList<>();
         while ((line = input.readLine()) != null) {
            String commit = line.split(" ")[0];
            commitNames.add(0, commit);
         }
         return commitNames;
      }
   }

   public static CommitDiff getChangedClasses(final File projectFolder, final List<File> modules, final String lastCommit, final ExecutionConfig config) {
      try {
         final Process process;
         if (lastCommit != null) {
            process = Runtime.getRuntime().exec("git diff --name-only " + lastCommit + " HEAD", null, projectFolder);
         } else {
            process = Runtime.getRuntime().exec("git diff --name-only HEAD^ HEAD", null, projectFolder);
         }
         return getDiffFromProcess(process, modules, projectFolder, config);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public static CommitDiff getChangedFiles(final File projectFolder, final List<File> modules, final String commit, final ExecutionConfig config) {
      try {
         final Process process = Runtime.getRuntime().exec("git diff --name-only " + commit + ".." + commit + "~1", new String[0], projectFolder);
         return getDiffFromProcess(process, modules, projectFolder, config);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   private static CommitDiff getDiffFromProcess(final Process p, final List<File> modules, final File projectFolder, final ExecutionConfig config) {
      final CommitDiff diff = new CommitDiff(modules, projectFolder);
      final String output = StreamGobbler.getFullProcess(p, false);
      for (final String line : output.split("\n")) {
         diff.addChange(line, config);
      }
      return diff;
   }

   public static int getChangedLines(final File projectFolder, final String commit, final List<ChangedEntity> entities, ExecutionConfig config) {
      try {

         final File folderTemp = new File(FilenameUtils.normalize(projectFolder.getAbsolutePath()));
         final String command = "git diff --stat=250 " + commit + ".." + commit + "~1";
         final Process process = Runtime.getRuntime().exec(command, new String[0], folderTemp);
         final String output = StreamGobbler.getFullProcess(process, false);
         int size = 0;
         final String[] lines = output.split("\n");

         // Skip last line - therefore - 1
         for (int index = 0; index < lines.length - 1; index++) {
            final String line = lines[index].trim().replaceAll("\\s{2,}", " ");
            System.out.println(line);
            final String[] parts = line.split("\\|");
            final String clazz = parts[0].replaceAll(" ", "");
            final String number = parts[1];
            final String countString = number.split(" ")[1];
            if (!countString.equals("Bin")) {
               final int count = Integer.parseInt(countString);
               String clazzName = getClazz(clazz, config);
               if (clazzName != null) {
                  final ChangedEntity changedEntity = new ChangedEntity(clazzName, "");
                  if (entities.contains(changedEntity)) {
                     size += count;
                  }
               }
            }
         }
         return size;
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return -1;
   }

   public static String getClazz(String currentFileName, ExecutionConfig config) {
      if (currentFileName.endsWith(CommitDiff.JAVA_ENDING)) {
         String fileNameWithoutExtension = currentFileName.substring(0, currentFileName.length() - CommitDiff.JAVA_ENDING.length());
         String containedPath = null;
         for (String path : config.getAllClazzFolders()) {
            if (fileNameWithoutExtension.contains(path)) {
               containedPath = path;
               break;
            }
         }

         if (containedPath != null) {
            final int indexOf = currentFileName.indexOf(containedPath);
            final String pathWithFolder = currentFileName.substring(indexOf);
            final String classPath = CommitDiff.replaceClazzFolderFromName(pathWithFolder, containedPath);
            return classPath;
         }
      }
      return null;
   }

   public static void pull(final File projectFolder) {
      synchronized (projectFolder) {
         LOG.debug("Pulling {}", projectFolder.getAbsolutePath());
         try {
            Process pullProcess = Runtime.getRuntime().exec("git pull origin HEAD", new String[0], projectFolder);
            final String out = StreamGobbler.getFullProcess(pullProcess, false);
            pullProcess.waitFor();
            LOG.debug(out);
         } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
         }
      }
   }

   /**
    * Lets the project go to the given state by resetting it to revert potential changes and by checking out the given commit.
    * 
    * @param commit
    * @param projectFolder
    */
   public static void goToCommit(final String commit, final File projectFolder) {
      try {
         synchronized (projectFolder) {
            LOG.debug("Going to commit {} folder: {}", commit, projectFolder.getAbsolutePath());
            reset(projectFolder);

            clean(projectFolder);

            int worked = checkout(commit, projectFolder);

            if (worked != 0) {
               LOG.info("Return value was !=0 - fetching");
               final Process pFetch = Runtime.getRuntime().exec("git fetch --all", new String[0], projectFolder);
               final String outFetch = StreamGobbler.getFullProcess(pFetch, false);
               pFetch.waitFor();
               System.out.println(outFetch);

               int secondCheckoutWorked = checkout(commit, projectFolder);

               if (secondCheckoutWorked != 0) {
                  LOG.error("Second checkout did not work - an old commit is probably analyzed");
               }
            }
         }
      } catch (final IOException | InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   public static void clean(final File projectFolder) throws IOException, InterruptedException {
      final Process pClean = Runtime.getRuntime().exec("git clean -df", new String[0], projectFolder);
      final String outClean = StreamGobbler.getFullProcess(pClean, false);
      pClean.waitFor();
      System.out.println(outClean);
   }

   public static void reset(final File projectFolder) {
      try {
         Process pReset = Runtime.getRuntime().exec("git reset --hard", new String[0], projectFolder);
         final String outReset = StreamGobbler.getFullProcess(pReset, false);
         pReset.waitFor();
         System.out.println(outReset);
      } catch (IOException | InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   private static int checkout(final String tag, final File projectFolder) throws IOException, InterruptedException {
      final String gitCommand = "git checkout -f " + tag;
      LOG.trace(gitCommand);
      final Process pCheckout = Runtime.getRuntime().exec(gitCommand, new String[0], projectFolder);
      final String outCheckout = StreamGobbler.getFullProcess(pCheckout, false);
      int worked = pCheckout.waitFor();
      System.out.println(outCheckout);
      if (outCheckout.toLowerCase().contains("smudge filter lfs failed")) {
         throw new RuntimeException("Checkout did not work. Smudge filter lfs failed");
      }
      return worked;
   }

   public static String getURL(final File projectFolder) {
      try {
         final Process process = Runtime.getRuntime().exec("git config --get remote.origin.url", new String[0], projectFolder);
         final String url = StreamGobbler.getFullProcess(process, false).replace("\n", "");
         return url;
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public static String getName(final String gitCommit, final File projectFolder) {
      try {
         final Process process = Runtime.getRuntime().exec("git rev-parse " + gitCommit, new String[0], projectFolder);
         final String tag = StreamGobbler.getFullProcess(process, false).replace("\n", "");// TODO Handle non-found commit
         return tag;
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public static synchronized String getPrevious(final String gitCommit, final File projectFolder) {
      return getName(gitCommit + "~1", projectFolder);
   }

   public static String getCommitText(final File projectFolder, final String commit) {
      String text = "";
      try {
         final String[] args2 = new String[] { "git", "log", "--pretty=format:%s %b", "-n", "1", commit };
         final Process process = Runtime.getRuntime().exec(args2, new String[0], projectFolder);
         text = StreamGobbler.getFullProcess(process, false).replace("\n", "");
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return text;
   }

   public static String getCommitter(final File projectFolder, final String commit) {
      String comitter = "";
      try {
         final String[] args2 = new String[] { "git", "log", "--pretty=format:%aE %aN", "-n", "1", commit };
         final Process process = Runtime.getRuntime().exec(args2, new String[0], projectFolder);
         comitter = StreamGobbler.getFullProcess(process, false).replace("\n", "");
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return comitter;
   }

   public static int getVersions(final File projectFolder) {
      try {
         final Process process = Runtime.getRuntime().exec("git rev-list --all --count", new String[0], projectFolder);
         final int count = Integer.parseInt(StreamGobbler.getFullProcess(process, false).replace("\n", ""));
         return count;
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return 0;
   }

   public static void unlockWithGitCrypt(final File projectFolder, final String gitCryptKey) {
      LOG.debug("GIT_CRYPT_KEY is set, unlocking: {}", projectFolder);
      final ProcessBuilder processBuilder = new ProcessBuilder("git-crypt", "unlock", gitCryptKey);
      try {
         if (processBuilder.directory(projectFolder).start().waitFor() != 0) {
            LOG.error("GitCryptUnlock-Process did not exit with 0!");
         }
      } catch (InterruptedException | IOException e) {
         e.printStackTrace();
      }

      if (!checkIsUnlockedWithGitCrypt(projectFolder)) {
         LOG.error("Folder is still locked, something went wrong!");
         throw new RuntimeException("Folder is still locked, something went wrong!");
      }
   }

   /*
    * This will probably not work, if you have a git-Repo inside a git-repo!
    * e.g. if you run peass-ci-plugin with mvn hpi:run, where your jenkins-workspace is "surrounded" by the peass-ci-plugin-repo
    */
   protected static boolean checkIsUnlockedWithGitCrypt(final File projectFolder) {
      final ProcessBuilder processBuilder = new ProcessBuilder("git", "config", "--local", "--get", "filter.git-crypt.smudge");
      processBuilder.directory(projectFolder);

      try {
         if (StreamGobbler.getFullProcess(processBuilder.start(), false).equals("\"git-crypt\" smudge\n")) {
            return true;
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      /*
       * Either it is locked, or git-crypt is not used in repo at all
       */
      return false;
   }
}
