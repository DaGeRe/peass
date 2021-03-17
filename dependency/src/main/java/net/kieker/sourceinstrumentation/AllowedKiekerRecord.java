package net.kieker.sourceinstrumentation;

public enum AllowedKiekerRecord {
   OPERATIONEXECUTION("kieker.monitoring.probe.aspectj.operationExecution.OperationExecutionAspectFull", 
         "kieker.common.record.controlflow.OperationExecutionRecord"), 
   REDUCED_OPERATIONEXECUTION("de.dagere.kopeme.kieker.record.probe.ReducedOperationExecutionAspectFull",
         "de.dagere.kopeme.kieker.record.ReducedOperationExecutionRecord");
   
   private String fullName;
   private String record;
   
   private AllowedKiekerRecord(final String fullName, final String record) {
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
