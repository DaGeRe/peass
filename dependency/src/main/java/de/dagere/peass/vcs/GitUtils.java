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

import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.VersionDiff;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependencyprocessors.VersionComparator;
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

   public static void getCommitsForURL(final String url) throws IOException {
      boolean repoFound = false;
      if (System.getenv(Constants.PEASS_PROJECTS) != null) {
         System.out.println(url);
         String project = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf("."));
         File candidate = new File(System.getenv(Constants.PEASS_PROJECTS), project);
         if (candidate.exists()) {
            repoFound = true;
            final List<GitCommit> commits = GitUtils.getCommits(candidate, false);
            VersionComparator.setVersions(commits);
         }
      }
      if (repoFound == false && System.getenv(Constants.PEASS_REPOS) != null) {
         final String repofolderName = System.getenv(Constants.PEASS_REPOS);
         File repoFolder = new File(repofolderName);
         File dependencyFolder = new File(repoFolder, "dependencies-final");
         String project = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf('.'));
         File dependencyfile = new File(dependencyFolder, "deps_" + project + ".json");
         LOG.debug("Searching: {}", dependencyfile);
         if (dependencyfile.exists()) {
            final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyfile, Dependencies.class);
            VersionComparator.setDependencies(dependencies);
            repoFound = true;
         }
      }

      if (!repoFound) {
         final File tempDir = Files.createTempDirectory("gitTemp").toFile();
         GitUtils.downloadProject(url, tempDir);
         final List<GitCommit> commits = GitUtils.getCommits(tempDir, false);
         VersionComparator.setVersions(commits);
         FileUtils.deleteDirectory(tempDir);
      }
   }

   public static String getDiff(final File file1, final File file2) throws IOException {
      final Process checkDiff = Runtime.getRuntime().exec("diff --ignore-all-space " + file1.getAbsolutePath() + " " + file2.getAbsolutePath());
      final String isDifferent = StreamGobbler.getFullProcess(checkDiff, false);
      return isDifferent;
   }

   public static void clone(final PeASSFolders folders, final File projectFolderTemp) throws InterruptedException, IOException {
      // TODO Branches klonen
      final File projectFolder = folders.getProjectFolder();
      clone(projectFolderTemp, projectFolder);
   }

   private static void clone(final File projectFolderDest, final File projectFolderSource) throws InterruptedException, IOException {
      final String clonedProject = projectFolderSource.getAbsolutePath();
      final String goalFolder = projectFolderDest.getName();
      if (projectFolderDest.exists()) {
         throw new RuntimeException("Can not clone to existing folder: " + projectFolderDest.getAbsolutePath());
      }
      final ProcessBuilder builder = new ProcessBuilder("git", "clone", clonedProject, goalFolder);
      builder.directory(projectFolderDest.getParentFile());
      StreamGobbler.showFullProcess(builder.start());
   }

   /**
    * Downloads a project to the given folder
    * 
    * @param url Project-URL
    * @param folder Goal-folder for download
    */
   public static void downloadProject(final String url, final File folder) {
      final String command = "git clone " + url + " " + folder.getAbsolutePath();
      try {
         LOG.debug("Command: " + command);
         final Process p = Runtime.getRuntime().exec(command);
         StreamGobbler.showFullProcess(p);
      } catch (final IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   /**
    * Removes all commits from a list that are before the given start version or after the given end version.
    * 
    * @param startversion Version to start
    * @param endversion Version to end
    * @param commits List of commits for filtering, sorted from older to newer
    */
   public static void filterList(final String startversion, final String endversion, final List<GitCommit> commits) {
      LOG.info("Count of Commits: {}", commits.size());
      boolean beforeStart = startversion == null ? false : true;
      boolean afterEnd = false;
      final List<GitCommit> notRelevantCommits = new LinkedList<>();
      for (final GitCommit commit : commits) {
         LOG.debug("Processing " + commit.getTag() + " " + commit.getDate() + " " + beforeStart + " " + afterEnd);
         if (startversion != null && commit.getTag().startsWith(startversion)) {
            beforeStart = false;
         }
         if (beforeStart || afterEnd) {
            notRelevantCommits.add(commit);
         }
         if (endversion != null && commit.getTag().startsWith(endversion)) {
            afterEnd = true;
            if (beforeStart == true) {
               throw new RuntimeException("Startversion " + startversion + " before endversion " + endversion);
            }
         }
      }
      System.out.println("Removing: " + notRelevantCommits.size());
      commits.removeAll(notRelevantCommits);
   }

   public static List<GitCommit> getCommits(final File folder, final String startversion, final String endversion) {
      final List<GitCommit> commits = getCommits(folder, true);
      GitUtils.filterList(startversion, endversion, commits);
      return commits;
   }

   public static List<GitCommit> getCommits(final File folder, final boolean includeAllBranches) {
      return getCommits(folder, includeAllBranches, true, false);
   }

   /**
    * Returns the commits of the git repo in ascending order.
    * 
    * @param folder
    * @return
    */
   public static List<GitCommit> getCommits(final File folder, final boolean includeAllBranches, final boolean linearizeHistory, final boolean getMetadata) {
      try {
         final List<String> commitNames;
         if (!linearizeHistory) {
            commitNames = getCommitNames(folder, includeAllBranches);
         } else {
            commitNames = getLinearCommitNames(folder);
         }
         final List<GitCommit> commits;
         if (getMetadata) {
            commits = getCommitsMetadata(folder, commitNames);
         } else {
            commits = new LinkedList<>();
            commitNames.forEach(tag -> commits.add(new GitCommit(tag, null, null, null)));
         }
         return commits;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

   }

   private static List<String> getLinearCommitNames(final File folder) {
      try {
         Process readOldestCommitProcess = Runtime.getRuntime().exec("git log --reverse  --pretty=tformat:%H", new String[0], folder);
         final BufferedReader readOldestCommitInput = new BufferedReader(new InputStreamReader(readOldestCommitProcess.getInputStream()));
         String oldestCommit = readOldestCommitInput.readLine().split(" ")[0];

         List<String> ouputCommitList = new LinkedList<>();
         String lastChild = null;

         final Process readCommitProcess = Runtime.getRuntime().exec("git rev-list --ancestry-path --children " + oldestCommit + "..HEAD", new String[0], folder);
         final BufferedReader readCommitInput = new BufferedReader(new InputStreamReader(readCommitProcess.getInputStream()));
         String line;

         while ((line = readCommitInput.readLine()) != null) {
            String[] ancestryHashes = line.split(" ");
            Set<String> hashes = new HashSet<>(Arrays.asList(ancestryHashes));
            if (lastChild == null || hashes.contains(lastChild)) {
               ouputCommitList.add(0, ancestryHashes[0]);
               lastChild = ancestryHashes[0];
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
         final Process readCommitProcess = Runtime.getRuntime().exec("git log -n 1 " + commit, new String[0], folder);
         final BufferedReader readCommitInput = new BufferedReader(new InputStreamReader(readCommitProcess.getInputStream()));
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
      return commits;
   }

   private static List<String> getCommitNames(final File folder, final boolean includeAllBranches) throws IOException {
      String command = includeAllBranches ? "git log --oneline --all" : "git log --oneline";
      final Process p = Runtime.getRuntime().exec(command, new String[0], folder);
      final BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      List<String> commitNames = new LinkedList<>();
      while ((line = input.readLine()) != null) {
         String commit = line.split(" ")[0];
         commitNames.add(0, commit);
      }
      return commitNames;
   }

   public static VersionDiff getChangedClasses(final File projectFolder, final List<File> modules, final String lastVersion) {
      try {
         final Process process;
         if (lastVersion != null) {
            process = Runtime.getRuntime().exec("git diff --name-only " + lastVersion + " HEAD", null, projectFolder);
         } else {
            process = Runtime.getRuntime().exec("git diff --name-only HEAD^ HEAD", null, projectFolder);
         }
         return getDiffFromProcess(process, modules, projectFolder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public static VersionDiff getChangedFiles(final File projectFolder, final List<File> modules, final String version) {
      try {
         final Process process = Runtime.getRuntime().exec("git diff --name-only " + version + ".." + version + "~1", new String[0], projectFolder);
         return getDiffFromProcess(process, modules, projectFolder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   private static VersionDiff getDiffFromProcess(final Process p, final List<File> modules, final File projectFolder) {
      final VersionDiff diff = new VersionDiff(modules, projectFolder);
      final String output = StreamGobbler.getFullProcess(p, false);
      for (final String line : output.split("\n")) {
         diff.addChange(line);
      }
      return diff;
   }

   public static int getChangedLines(final File projectFolder, final String version, final List<ChangedEntity> entities) {
      try {

         final File folderTemp = new File(FilenameUtils.normalize(projectFolder.getAbsolutePath()));
         final String command = "git diff --stat=250 " + version + ".." + version + "~1";
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
               final ChangedEntity changedEntity = new ChangedEntity(clazz, "");
               if (entities.contains(changedEntity)) {
                  size += count;
               }
            }
         }
         return size;
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return -1;
   }

   public static void pull(final File projectFolder) {
      synchronized (projectFolder) {
         LOG.debug("Pulling", projectFolder.getAbsolutePath());
         try {
            Process pullProcess = Runtime.getRuntime().exec("git pull origin HEAD", new String[0], projectFolder);
            final String out = StreamGobbler.getFullProcess(pullProcess, false);
            pullProcess.waitFor();
            LOG.debug(out);
         } catch (IOException | InterruptedException e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Lets the project go to the given state by resetting it to revert potential changes and by checking out the given version.
    * 
    * @param tag
    * @param projectFolder
    */
   public static void goToTag(final String tag, final File projectFolder) {
      try {
         synchronized (projectFolder) {
            LOG.debug("Going to tag {} folder: {}", tag, projectFolder.getAbsolutePath());
            reset(projectFolder);

            clean(projectFolder);

            int worked = checkout(tag, projectFolder);

            if (worked != 0) {
               LOG.info("Return value was !=0 - fetching");
               final Process pFetch = Runtime.getRuntime().exec("git fetch --all", new String[0], projectFolder);
               final String outFetch = StreamGobbler.getFullProcess(pFetch, false);
               pFetch.waitFor();
               System.out.println(outFetch);

               int secondCheckoutWorked = checkout(tag, projectFolder);

               if (secondCheckoutWorked != 0) {
                  LOG.error("Second checkout did not work - an old version is probably analyzed");
               }
            }
         }
      } catch (final IOException e) {
         e.printStackTrace();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
   }

   public static void clean(final File projectFolder) throws IOException, InterruptedException {
      final Process pClean = Runtime.getRuntime().exec("git clean -df", new String[0], projectFolder);
      final String outClean = StreamGobbler.getFullProcess(pClean, false);
      pClean.waitFor();
      System.out.println(outClean);
   }

   public static void reset(final File projectFolder) throws IOException, InterruptedException {
      final Process pReset = Runtime.getRuntime().exec("git reset --hard", new String[0], projectFolder);
      final String outReset = StreamGobbler.getFullProcess(pReset, false);
      pReset.waitFor();
      System.out.println(outReset);
   }

   private static int checkout(final String tag, final File projectFolder) throws IOException, InterruptedException {
      final String gitCommand = "git checkout -f " + tag;
      LOG.trace(gitCommand);
      final Process pCheckout = Runtime.getRuntime().exec(gitCommand, new String[0], projectFolder);
      final String outCheckout = StreamGobbler.getFullProcess(pCheckout, false);
      int worked = pCheckout.waitFor();
      System.out.println(outCheckout);
      return worked;
   }

   public static void goToTagSoft(final String tag, final File projectFolder) {
      try {
         synchronized (projectFolder) {
            LOG.debug("Going to tag {} folder: {}", tag, projectFolder.getAbsolutePath());
            final Process pReset = Runtime.getRuntime().exec("git reset --hard", new String[0], projectFolder);
            final String out = StreamGobbler.getFullProcess(pReset, false);
            pReset.waitFor();

            final String gitCommand = "git checkout " + tag;
            LOG.trace(gitCommand);
            final Process pCheckout = Runtime.getRuntime().exec(gitCommand, new String[0], projectFolder);
            final String outCheckout = StreamGobbler.getFullProcess(pCheckout, false);
            pCheckout.waitFor();

            System.out.println(out);
            System.out.println(outCheckout);
            LOG.trace("Ausführung beendet");
         }
      } catch (final IOException e) {
         e.printStackTrace();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
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

   public synchronized static String getPrevious(final String gitCommit, final File projectFolder) {
      return getName(gitCommit + "~1", projectFolder);
   }

   public static String getCommitText(final File projectFolder, final String version) {
      String text = "";
      try {
         final String[] args2 = new String[] { "git", "log", "--pretty=format:%s %b", "-n", "1", version };
         final Process process = Runtime.getRuntime().exec(args2, new String[0], projectFolder);
         text = StreamGobbler.getFullProcess(process, false).replace("\n", "");
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return text;
   }

   public static String getCommitter(final File projectFolder, final String version) {
      String comitter = "";
      try {
         final String[] args2 = new String[] { "git", "log", "--pretty=format:%aE %aN", "-n", "1", version };
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
}
