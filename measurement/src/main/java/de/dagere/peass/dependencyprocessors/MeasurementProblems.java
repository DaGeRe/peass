package de.dagere.peass.dependencyprocessors;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class MeasurementProblems {
   private long date;
   private Map<String, List<MeasurementProblem>> problems = new LinkedHashMap<>();

   public Map<String, List<MeasurementProblem>> getProblems() {
      return problems;
   }

   public void setProblems(Map<String, List<MeasurementProblem>> problems) {
      this.problems = problems;
   }

   public long getDate() {
      return date;
   }

   public void setDate(long date) {
      this.date = date;
   }

   public void addProblem(String version, String reason) {
      List<MeasurementProblem> problemList = problems.get(version);
      if (problemList == null) {
         problemList = new LinkedList<>();
         problems.put(version, problemList);
      }
      problemList.add(new MeasurementProblem(reason));
   }
}