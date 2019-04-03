package de.peran.analysis.helper.read;

import java.util.LinkedHashMap;
import java.util.Map;

import de.peran.measurement.analysis.TestcaseStatistic;

public class FolderValues {
   private Map<String, TestcaseStatistic> values = new LinkedHashMap<>();
   private Map<String, Boolean> isTChange = new LinkedHashMap<>();
   private Map<String, Boolean> isConfidenceChange = new LinkedHashMap<>();

   public Map<String, Boolean> getIsTChange() {
      return isTChange;
   }

   public Map<String, TestcaseStatistic> getValues() {
      return values;
   }

   public void setValues(Map<String, TestcaseStatistic> values) {
      this.values = values;
   }

   public void setIsTChange(Map<String, Boolean> isTChange) {
      this.isTChange = isTChange;
   }

   public Map<String, Boolean> getIsConfidenceChange() {
      return isConfidenceChange;
   }

   public void setIsConfidenceChange(Map<String, Boolean> isConfidenceChange) {
      this.isConfidenceChange = isConfidenceChange;
   }
}