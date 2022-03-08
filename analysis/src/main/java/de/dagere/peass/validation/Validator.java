package de.dagere.peass.validation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.validation.data.ProjectValidation;
import de.dagere.peass.validation.data.Validation;
import de.dagere.peass.validation.data.ValidationChange;
import de.dagere.peass.vcs.GitUtils;

public class Validator {

   private static final Logger LOG = LogManager.getLogger(Validator.class);

   private final ExecutionData changedTests;
   private final String firstVersion;
   private final String projectName;
   private int selected = 0;
   private int measured = 0;
   private int before = 0;
   private final ProjectChanges changes;
   private final ProjectStatistics statistics;

   public Validator(final File dependencyFolder, final File changeFolder, final String project) throws JsonParseException, JsonMappingException, IOException {
      final File executionFile = new File(dependencyFolder, ResultsFolders.TRACE_SELECTION_PREFIX + project + ".json");
      changedTests = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);

      final File dependencyFile = new File(dependencyFolder, ResultsFolders.STATIC_SELECTION_PREFIX + project + ".json");
      final StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, StaticTestSelection.class);
      firstVersion = dependencies.getVersionNames()[0];

      File changeFile = getChangefile(changeFolder, project);
      if (changeFile.exists()) {
         changes = Constants.OBJECTMAPPER.readValue(changeFile, ProjectChanges.class);
      } else {
         changes = null;
      }
      File statisticFile = getStatisticsFile(changeFolder, project);
      if (statisticFile.exists()) {
         statistics = Constants.OBJECTMAPPER.readValue(statisticFile, ProjectStatistics.class);
      } else {
         statistics = null;
      }

      projectName = project;
   }

   public File getStatisticsFile(final File changeFolder, final String project) {
      File statisticFile = new File(changeFolder, project + File.separator + "statistics" + File.separator + project + ".json");
      if (!statisticFile.exists()) {
         statisticFile = new File(changeFolder, project + File.separator + "statistics" + File.separator + "raw.json");
      }
      if (!statisticFile.exists()) {
         statisticFile = new File(changeFolder, "statistics" + File.separator + project + ".json");
      }
      return statisticFile;
   }

   public File getChangefile(final File changeFolder, final String project) {
      File changeFile = new File(changeFolder, project + File.separator + project + ".json");
      if (!changeFile.exists()) {
         changeFile = new File(changeFolder, project + File.separator + "raw.json");
      }
      if (!changeFile.exists()) {
         changeFile = new File(changeFolder, project + ".json");
      }
      return changeFile;
   }

   ProjectValidation validateProject(final Validation old, final Map<String, String> commits)
         throws IOException, JsonParseException, JsonMappingException {
      final ProjectValidation projectValidation = new ProjectValidation();

      LOG.info("Project: " + projectName);

      GitUtils.getCommitsForURL(changedTests.getUrl());

      Map<String, String> sortedCommits = VersionComparator.INSTANCE.sort(commits);
      for (final Map.Entry<String, String> commit : sortedCommits.entrySet()) {
         final boolean isBefore = VersionComparator.isBefore(commit.getKey(), firstVersion);
         final ValidationChange change = new ValidationChange();
         getOldExplanation(old, commit.getKey(), change);
         if (isBefore) {
            before++;
            change.setType("BEFORE");
         } else {
            if (changedTests.getVersions().get(commit.getKey()) != null) {
               selected++;
               if (changes != null) {
                  if (changes.getVersionChanges().containsKey(commit.getKey())) {
                     measured++;
                     if (commit.getKey().contains("59ffcad15d220c2bc1f70f01d58bc31dec04b423")) {
                        System.out.println("Test");
                     }
                     boolean correct = hasMeasuredImprovemend(commit.getKey());
                     if (correct) {
                        change.setType("MEASURED_CORRECT");
                     } else {
                        change.setType("MEASURED_UNCORRECT");
                     }

                     change.setCorrect(correct);
                  } else {
                     if (statistics.getStatistics().get(commit.getKey()) != null) {
                        System.out.println("No change measured: " + commit.getKey());
                        change.setType("MEASURED_UNCHANGED");
                     } else {
                        System.out.println("No change measured: " + commit.getKey());
                        change.setType("NOT_MEASURED");
                     }
                  }
               } else {
                  change.setType("NOT_MEASURED");
               }
            } else {
               System.out.println("Unselected: " + commit.getKey());
               change.setType("NOT_SELECTED");
            }
         }
         projectValidation.getChanges().put(commit.getKey(), change);
      }
      System.out.println("Selected: " + selected + " / " + commits.size());
      System.out.println("Measured: " + measured + " / " + commits.size());
      System.out.println("Before: " + before + " / " + commits.size());

      return projectValidation;
   }

   private void getOldExplanation(final Validation old, final String commit, final ValidationChange change) {
      if (old != null && old.getProjects().get(projectName) != null) {
         if (old.getProjects().get(projectName).getChanges().get(commit) != null) {
            ValidationChange oldChange = old.getProjects().get(projectName).getChanges().get(commit);
            if (oldChange.getExplanation() != null) {
               change.setExplanation(oldChange.getExplanation());
            }
         }
      }
   }

   private boolean hasMeasuredImprovemend(final String version) {
      boolean correct = false;
      Changes testcaseValues = changes.getVersionChanges().get(version);
      // for (Changes testcaseValues : changes.getVersionChanges().values()) {
      for (List<Change> methodChanges : testcaseValues.getTestcaseChanges().values()) {
         for (Change methodChange : methodChanges) {
            if (methodChange.getChangePercent() > 0) {
               correct = true;
            }
         }
      }
      // }
      return correct;
   }
}
