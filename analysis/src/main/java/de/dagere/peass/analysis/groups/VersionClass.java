package de.dagere.peass.analysis.groups;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.nodeDiffGenerator.data.serialization.MethodCallDeserializer;

public class VersionClass {
   
   @JsonDeserialize(keyUsing = MethodCallDeserializer.class)
   private Map<MethodCall, TestcaseClass> testcases = new HashMap<>();

   public void setTestcases(final Map<MethodCall, TestcaseClass> testcases) {
      this.testcases = testcases;
   }

   public Map<MethodCall, TestcaseClass> getTestcases() {
      return testcases;
   }

   @JsonIgnore
   public TestcaseClass addTestcase(final MethodCall test, final Set<String> guessedTypes, final String direction) {
      final TestcaseClass data = new TestcaseClass();
      data.setGuessedTypes(guessedTypes);
      data.setDirection(direction);
      testcases.put(test, data);
      return data;
   }

}
