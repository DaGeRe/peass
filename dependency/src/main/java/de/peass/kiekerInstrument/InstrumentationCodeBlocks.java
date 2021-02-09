package de.peass.kiekerInstrument;

import java.util.List;

public enum InstrumentationCodeBlocks {
   OPERATIONEXECUTION("      // collect data\n" +
         "      final boolean entrypoint;\n" +
         "      final String hostname = MonitoringController.getInstance().getHostname();\n" +
         "      final String sessionId = SessionRegistry.INSTANCE.recallThreadLocalSessionId();\n" +
         "      final int eoi; // this is executionOrderIndex-th execution in this trace\n" +
         "      final int ess; // this is the height in the dynamic call tree of this execution\n" +
         "      long traceId = ControlFlowRegistry.INSTANCE.recallThreadLocalTraceId(); // traceId, -1 if entry point\n" +
         "      if (traceId == -1) {\n" +
         "         entrypoint = true;\n" +
         "         traceId = ControlFlowRegistry.INSTANCE.getAndStoreUniqueThreadLocalTraceId();\n" +
         "         ControlFlowRegistry.INSTANCE.storeThreadLocalEOI(0);\n" +
         "         ControlFlowRegistry.INSTANCE.storeThreadLocalESS(1); // next operation is ess + 1\n" +
         "         eoi = 0;\n" +
         "         ess = 0;\n" +
         "      } else {\n" +
         "         entrypoint = false;\n" +
         "         eoi = ControlFlowRegistry.INSTANCE.incrementAndRecallThreadLocalEOI(); // ess > 1\n" +
         "         ess = ControlFlowRegistry.INSTANCE.recallAndIncrementThreadLocalESS(); // ess >= 0\n" +
         "         if ((eoi == -1) || (ess == -1)) {\n" +
         "            System.err.println(\"eoi and/or ess have invalid values: eoi == {} ess == {}\"+ eoi+ \"\" + ess);\n" +
         "            MonitoringController.getInstance().terminateMonitoring();\n" +
         "         }\n" +
         "      }\n" +
         "      // measure before\n" +
         "      final long tin = MonitoringController.getInstance().getTimeSource().getTime();\n",
         "// measure after\n" +
               "         final long tout = MonitoringController.getInstance().getTimeSource().getTime();\n" +
               "         MonitoringController.getInstance().newMonitoringRecord(new OperationExecutionRecord(signature, sessionId, traceId, tin, tout, hostname, eoi, ess));\n" +
               "         // cleanup\n" +
               "         if (entrypoint) {\n" +
               "            ControlFlowRegistry.INSTANCE.unsetThreadLocalTraceId();\n" +
               "            ControlFlowRegistry.INSTANCE.unsetThreadLocalEOI();\n" +
               "            ControlFlowRegistry.INSTANCE.unsetThreadLocalESS();\n" +
               "         } else {\n" +
               "            ControlFlowRegistry.INSTANCE.storeThreadLocalESS(ess); // next operation is ess\n" +
               "         }",
         null), REDUCED_OPERATIONEXECUTION("      final long tin = MonitoringController.getInstance().getTimeSource().getTime();\n", "// measure after\n"
               + "final long tout = MonitoringController.getInstance().getTimeSource().getTime()\n"
               + "MonitoringController.getInstance().newMonitoringRecord(new ReducedOperationExecutionRecord(signature, tin, tout))", null), SAMPLING(null, null, null);

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
