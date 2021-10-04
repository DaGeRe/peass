package net.kieker.sourceinstrumentation;

public enum AllowedKiekerRecord {
   OPERATIONEXECUTION("kieker.monitoring.probe.aspectj.operationExecution.OperationExecutionAspectFull", 
         "kieker.common.record.controlflow.OperationExecutionRecord"), 
   DURATION("de.dagere.kopeme.kieker.probe.DurationAspectFull",
         "de.dagere.kopeme.kieker.record.DurationRecord");
   
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
