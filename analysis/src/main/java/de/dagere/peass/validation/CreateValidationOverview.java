package de.dagere.peass.validation;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.validation.data.ProjectValidation;
import de.dagere.peass.validation.data.Validation;
import de.dagere.peass.validation.data.ValidationChange;
import de.peran.FolderSearcher;

public class CreateValidationOverview {
   private static final boolean FOR_PHD_THESIS = false;

   static class ValidationCounts {
      int before = 0;
      int correct = 0;
      int incorrect = 0;
      int unchanged = 0;
      int unselected = 0;
      int sum;

      int workload = 0;
   }

   static class Analyzer {
      ValidationCounts overall = new ValidationCounts();
      ValidationCounts current = new ValidationCounts();

      public void analyze(ProjectValidation project) {
         for (final ValidationChange change : project.getChanges().values()) {
            if (change.getType().equals("BEFORE")) {
               overall.before++;
               current.before++;
            } else if (change.getType().equals("MEASURED_CORRECT")) {
               overall.correct++;
               current.correct++;
            } else if (change.getType().equals("MEASURED_UNCORRECT")) {
               handleWrong(change);
               // If it got slower, but the committer did a mistake, the measurement of PeASS is correct

            } else if (change.getType().equals("NOT_SELECTED")) {
               overall.unselected++;
               current.unselected++;
            } else if (change.getType().equals("MEASURED_UNCHANGED")) {
               handleWrong(change);
               // overall.incorrect++;
               // current.incorrect++;
            }
         }
         // current.sum = current.before + current.correct + current.incorrect + current.unselected + current.unchanged;
         // Leave before out - only distracting for understanding
         current.sum = current.correct + current.workload + current.unselected + current.unchanged;
         overall.sum += current.sum;
      }

      public void handleWrong(final ValidationChange change) {
         if (change.getExplanation() != null) {
            if (change.getExplanation().equals("WRONG")) {
               overall.correct++;
               current.correct++;
            }
            if (change.getExplanation().equals("UNCHANGED") || change.getExplanation().equals("OLD_JDK_CHANGE")) {
               current.unchanged++;
               overall.unchanged++;
            }
            if (change.getExplanation().equals("WORKLOAD")
                  || change.getExplanation().equals("SMALL_TESTCASE")
                  || change.getExplanation().equals("BUFFERED_STREAM")) {
               current.workload++;
               overall.workload++;
            }
         } else {
            throw new RuntimeException("No Definition");
         }
      }

      public void reset() {
         current = new ValidationCounts();
      }
   }

   static class ValidationLines {
      StringBuffer project = new StringBuffer("Project & ");
      StringBuffer selected = new StringBuffer("Selected & ");
      StringBuffer correct = new StringBuffer("Correct & ");
      StringBuffer workload = new StringBuffer("Workload & ");
      StringBuffer unchanged = new StringBuffer("Unchanged & ");
      StringBuffer unselected = new StringBuffer("Unselected & ");
      StringBuffer before = new StringBuffer("Before & ");
      StringBuffer all = new StringBuffer("All & ");
   }

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {

      final File validationFile = new File(args[0]);
      final Validation validation = FolderSearcher.MAPPER.readValue(validationFile, Validation.class);

      Analyzer analyzer = new Analyzer();

      if (FOR_PHD_THESIS) {
         System.out.println("Projekt & Performanzverbesserungen & Gefunden & Nicht Untersucht");

         for (final Map.Entry<String, ProjectValidation> project : validation.getProjects().entrySet()) {
            analyzer.analyze(project.getValue());
            final double correctPercent = (analyzer.current.correct / ((double) analyzer.current.correct + analyzer.current.incorrect + analyzer.current.unchanged));
            System.out.println(project.getKey() + " & " +
                  (analyzer.current.correct + analyzer.current.incorrect + analyzer.current.unchanged) + " & " +
                  analyzer.current.correct + "(" + correctPercent + ")" + " & " +
                  analyzer.current.before + "\\\\");

            analyzer.reset();
         }
         System.out
               .println("Summe  & " + (analyzer.overall.sum) + " & " +
                     analyzer.overall.correct + "(" + ((double) analyzer.overall.correct / analyzer.overall.sum) + ")" + " & " +
                     analyzer.overall.before);
      } else {
         ValidationLines lines = new ValidationLines();

         for (final Map.Entry<String, ProjectValidation> project : validation.getProjects().entrySet()) {
            analyzer.analyze(project.getValue());
            lines.project.append(project.getKey() + " & ");
            lines.selected.append((analyzer.current.correct + analyzer.current.workload + analyzer.current.unchanged) + " & ");
            lines.correct.append(analyzer.current.correct + " & ");
            lines.workload.append(analyzer.current.workload + " & ");
            lines.unchanged.append(analyzer.current.unchanged + " & ");
            lines.unselected.append(analyzer.current.unselected + " & ");
            // lines.before.append(analyzer.current.before + " & ");
            lines.all.append(analyzer.current.sum + " & ");

            // final double correctPercent = (analyzer.correct / ((double) analyzer.correct + analyzer.incorrect + analyzer.unchanged));
            // System.out.println(project.getKey() + " & " +
            // (analyzer.correct + analyzer.incorrect + analyzer.unchanged) + " & " +
            // analyzer.correct + "(" + correctPercent + ")" + " & " +
            // analyzer.workload + " & " +
            // analyzer.no_change + " & " +
            // analyzer.unselected + " & " +
            // analyzer.before + "\\\\");
            analyzer.reset();
         }
         System.out.println(lines.project + "\\\\");
         System.out.println(lines.selected.toString() + (analyzer.overall.correct + analyzer.overall.workload + analyzer.overall.unchanged) + "\\\\");
         System.out.println(lines.correct.toString() + analyzer.overall.correct + "\\\\");
         System.out.println(lines.workload.toString() + analyzer.overall.workload + "\\\\");
         System.out.println(lines.unchanged.toString() + analyzer.overall.unchanged + "\\\\ \\hline");
         System.out.println(lines.unselected.toString() + analyzer.overall.unselected + "\\\\ \\hline");
         // System.out.println(lines.before.toString() + analyzer.overall.before + "\\\\ \\hline");
         System.out.println(lines.all.toString() + analyzer.overall.sum + "\\\\");
      }

      int selected = analyzer.overall.correct + analyzer.overall.workload;
      double percentage = (double) analyzer.overall.correct / selected;
      System.out.println();
      System.out.println("Percentage: " + percentage + " (" + analyzer.overall.correct + " / " + selected + ")");
      System.out.println("Before: " + analyzer.overall.before);

   }
}
