package de.peass.kiekerInstrument;

import java.util.List;

public enum InstrumentationCodeBlocks {
   OPERATIONEXECUTION("      // collect data\n" +
         "      final boolean " + InstrumentationConstants.PREFIX + "entrypoint;\n" +
         "      final String " + InstrumentationConstants.PREFIX + "hostname = MonitoringController.getInstance().getHostname();\n" +
         "      final String " + InstrumentationConstants.PREFIX + "sessionId = SessionRegistry.INSTANCE.recallThreadLocalSessionId();\n" +
         "      final int " + InstrumentationConstants.PREFIX + "eoi; // this is executionOrderIndex-th execution in this trace\n" +
         "      final int " + InstrumentationConstants.PREFIX + "ess; // this is the height in the dynamic call tree of this execution\n" +
         "      long " + InstrumentationConstants.PREFIX + "traceId = ControlFlowRegistry.INSTANCE.recallThreadLocalTraceId(); // traceId, -1 if entry point\n" +
         "      if (" + InstrumentationConstants.PREFIX + "traceId == -1) {\n" +
         "         " + InstrumentationConstants.PREFIX + "entrypoint = true;\n" +
         "         " + InstrumentationConstants.PREFIX + "traceId = ControlFlowRegistry.INSTANCE.getAndStoreUniqueThreadLocalTraceId();\n" +
         "         ControlFlowRegistry.INSTANCE.storeThreadLocalEOI(0);\n" +
         "         ControlFlowRegistry.INSTANCE.storeThreadLocalESS(1); // next operation is ess + 1\n" +
         "         " + InstrumentationConstants.PREFIX + "eoi = 0;\n" +
         "         " + InstrumentationConstants.PREFIX + "ess = 0;\n" +
         "      } else {\n" +
         "         " + InstrumentationConstants.PREFIX + "entrypoint = false;\n" +
         "         " + InstrumentationConstants.PREFIX + "eoi = ControlFlowRegistry.INSTANCE.incrementAndRecallThreadLocalEOI(); // ess > 1\n" +
         "         " + InstrumentationConstants.PREFIX + "ess = ControlFlowRegistry.INSTANCE.recallAndIncrementThreadLocalESS(); // ess >= 0\n" +
         "         if ((" + InstrumentationConstants.PREFIX + "eoi == -1) || (" + InstrumentationConstants.PREFIX + "ess == -1)) {\n" +
         "            System.err.println(\"eoi and/or ess have invalid values: eoi == {} ess == {}\"+ " + InstrumentationConstants.PREFIX + "eoi+ \"\" + "
         + InstrumentationConstants.PREFIX + "ess);\n" +
         "            MonitoringController.getInstance().terminateMonitoring();\n" +
         "         }\n" +
         "      }\n" +
         "      // measure before\n" +
         "      final long " + InstrumentationConstants.PREFIX + "tin = MonitoringController.getInstance().getTimeSource().getTime();\n",
         "// measure after\n" +
               "         final long " + InstrumentationConstants.PREFIX + "tout = MonitoringController.getInstance().getTimeSource().getTime();\n" +
               "         MonitoringController.getInstance().newMonitoringRecord(new OperationExecutionRecord("
               + InstrumentationConstants.PREFIX + "signature, "
               + InstrumentationConstants.PREFIX + "sessionId, "
               + InstrumentationConstants.PREFIX + "traceId, "
               + InstrumentationConstants.PREFIX + "tin, "
               + InstrumentationConstants.PREFIX + "tout, "
               + InstrumentationConstants.PREFIX + "hostname, "
               + InstrumentationConstants.PREFIX + "eoi, "
               + InstrumentationConstants.PREFIX + "ess));\n" +
               "         // cleanup\n" +
               "         if (" + InstrumentationConstants.PREFIX + "entrypoint) {\n" +
               "            ControlFlowRegistry.INSTANCE.unsetThreadLocalTraceId();\n" +
               "            ControlFlowRegistry.INSTANCE.unsetThreadLocalEOI();\n" +
               "            ControlFlowRegistry.INSTANCE.unsetThreadLocalESS();\n" +
               "         } else {\n" +
               "            ControlFlowRegistry.INSTANCE.storeThreadLocalESS(" + InstrumentationConstants.PREFIX + "ess); // next operation is ess\n" +
               "         }",
         null), REDUCED_OPERATIONEXECUTION("      final long " + InstrumentationConstants.PREFIX + "tin = MonitoringController.getInstance().getTimeSource().getTime();\n",
               "// measure after\n"
                     + "final long " + InstrumentationConstants.PREFIX + "tout = MonitoringController.getInstance().getTimeSource().getTime()\n"
                     + "MonitoringController.getInstance().newMonitoringRecord(new ReducedOperationExecutionRecord("
                     + InstrumentationConstants.PREFIX + "signature, "
                     + InstrumentationConstants.PREFIX + "tin, "
                     + InstrumentationConstants.PREFIX + "tout))",
               null), SAMPLING("      final long " + InstrumentationConstants.PREFIX + "tin = MonitoringController.getInstance().getTimeSource().getTime();", null, null);

   private final String before, after;
   private final List<String> declaredVariables;

   private InstrumentationCodeBlocks(final String before, final String after, final List<String> declaredVariables) {
      this.before = before;
      this.after = after;
      this.declaredVariables = declaredVariables;
   }

   public String getBefore() {
      return before;
   }

   public String getAfter() {
      return after;
   }

   public List<String> getDeclaredVariables() {
      return declaredVariables;
   }

}
