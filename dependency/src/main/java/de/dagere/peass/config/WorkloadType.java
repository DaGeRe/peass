package de.dagere.peass.config;

public enum WorkloadType {
   JUNIT("de.dagere.peass.testtransformation.JUnitTestTransformer", "default"), JMH("de.dagere.peass.dependency.jmh.JmhTestTransformer", "de.dagere.peass.dependency.jmh.JmhTestExecutor");
   
   private final String testTransformer;
   private final String testExecutor;
   
   private WorkloadType(final String testTransformer, final String testExecutor) {
      this.testTransformer = testTransformer;
      this.testExecutor = testExecutor;
   }

   public String getTestExecutor() {
      return testExecutor;
   }
   
   public String getTestTransformer() {
      return testTransformer;
   }
}
