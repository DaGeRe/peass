package de.peass.dependency.execution;

public enum AllowedKiekerRecord {
   OPERATIONEXECUTION("kieker.monitoring.probe.aspectj.operationExecution.OperationExecutionAspectFull"), 
   REDUCED_OPERATIONEXECUTION("kieker.monitoring.probe.aspectj.operationExecution.ReducedOperationExecutionAspectFull");
   
   private String fullName;
   
   private AllowedKiekerRecord(String fullName) {
      this.fullName = fullName;
   }
   
   public String getFullName() {
      return fullName;
   }
}
