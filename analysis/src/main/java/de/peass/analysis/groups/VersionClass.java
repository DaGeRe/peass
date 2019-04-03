package de.peass.analysis.groups;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestSet.ChangedEntitityDeserializer;

public class VersionClass {
   
   @JsonDeserialize(keyUsing = ChangedEntitityDeserializer.class)
   private Map<ChangedEntity, TestcaseClass> testcases = new HashMap<>();

   public void setTestcases(final Map<ChangedEntity, TestcaseClass> testcases) {
      this.testcases = testcases;
   }

   public Map<ChangedEntity, TestcaseClass> getTestcases() {
      return testcases;
   }

   @JsonIgnore
   public TestcaseClass addTestcase(final ChangedEntity test, final Set<String> guessedTypes, final String direction) {
      final TestcaseClass data = new TestcaseClass();
      data.setGuessedTypes(guessedTypes);
      data.setDirection(direction);
      testcases.put(test, data);
      return data;
   }

}
