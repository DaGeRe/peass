package de.peass.dependency.execution;

public enum AllowedKiekerRecord {
   OPERATIONEXECUTION("kieker.monitoring.probe.aspectj.operationExecution.OperationExecutionAspectFull", 
         "kieker.common.record.controlflow.OperationExecutionRecord"), 
   REDUCED_OPERATIONEXECUTION("kieker.monitoring.probe.aspectj.operationExecution.ReducedOperationExecutionAspectFull",
         "kieker.common.record.controlflow.ReducedOperationExecutionRecord");
   
   private String fullName;
   private String record;
   
   private AllowedKiekerRecord(String fullName, String record) {
      this.fullName = fullName;
      this.record = record;
   }
   
   public String getFullName() {
      return fullName;
   }
   
   public String getRecord() {
      return record;
   }
}
