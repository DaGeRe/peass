package de.peass.validation.temp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.all.RepoFolders;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;
import de.peran.FolderSearcher;

public class GetValidationExecutionFile {

   public static final String[] VALIDATION_PROJECTS = new String[] { "commons-compress", "commons-fileupload", "commons-dbcp", "commons-csv", "commons-pool", "commons-io",
         "commons-imaging", "commons-text", "jackson-core" };
   private final static String[] CHANGE_INDICATORS = new String[] { "performance", "fast", "optimization", "memory", "slow", "accelerate", "garbage", "speed", "cache" };

   public static List<String> excluded = new LinkedList<>();

   public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
      final RepoFolders folders = new RepoFolders();

      File projectsFolder = new File(System.getenv(Constants.PEASS_PROJECTS));

      File noPerformanceChangeFile = new File(folders.getCleanDataFolder().getParentFile(), "validation" + File.separator + "noperformancechange.txt");
      readNoPerformanceChanges(noPerformanceChangeFile);

      for (String project : VALIDATION_PROJECTS) {
         getProjectValidationExecution(folders, projectsFolder, project);
      }
   }

   public static void readNoPerformanceChanges(File noPerformanceChangeFile) throws FileNotFoundException, IOException {
      try (BufferedReader reader = new BufferedReader(new FileReader(noPerformanceChangeFile))){
         String line;
         while ((line = reader.readLine()) != null) {
            String commit = line.substring(0, line.indexOf(" "));
            excluded.add(commit);
         }
      }
   }

   public static void getProjectValidationExecution(final RepoFolders folders, File projectsFolder, String project)
         throws JsonParseException, JsonMappingException, IOException, JsonGenerationException {
      final ExecutionData changedTests = folders.getExecutionData(project);

      final ExecutionData selected = new ExecutionData();
      selected.setAndroid(changedTests.isAndroid());
      selected.setUrl(changedTests.getUrl());

      File projectFolder = new File(projectsFolder, project);
      List<GitCommit> commits = GitUtils.getCommits(projectFolder);
      VersionComparator.setVersions(commits);
      checkVersions(changedTests, selected, commits);

      File dest = getValidationExecutionFile(project);
      FolderSearcher.MAPPER.writeValue(dest, selected);
   }

   public static File getValidationExecutionFile(String project) {
      File dest = new File("results", "reexecute-validation" + File.separator + "execute_" + project + "_validation.json");
      dest.getParentFile().mkdirs();
      return dest;
   }

   public static void checkVersions(final ExecutionData changedTests, final ExecutionData selected, List<GitCommit> commits) {
      for (GitCommit commit : commits) {
         boolean contains = Arrays.stream(CHANGE_INDICATORS).anyMatch(commit.getMessage()::contains);
         if (contains && !excluded.contains(commit.getTag())) {
            System.out.println(commit.getTag());
            TestSet versionsTests = changedTests.getVersions().get(commit.getTag());
            if (versionsTests != null) {
               selected.getVersions().put(commit.getTag(), versionsTests);
            }
         }
      }
   }
}
