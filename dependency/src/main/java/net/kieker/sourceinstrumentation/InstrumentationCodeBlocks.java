package net.kieker.sourceinstrumentation;

import java.util.List;

public enum InstrumentationCodeBlocks {
   OPERATIONEXECUTION("      // collect data\n" +
         "      final boolean " + InstrumentationConstants.PREFIX + "entrypoint;\n" +
         "      final String " + InstrumentationConstants.PREFIX + "hostname = " + InstrumentationConstants.PREFIX + "VM_NAME;\n" +
         "      final String " + InstrumentationConstants.PREFIX + "sessionId = " + InstrumentationConstants.PREFIX + "SESSION_REGISTRY.recallThreadLocalSessionId();\n" +
         "      final int " + InstrumentationConstants.PREFIX + "eoi; // this is executionOrderIndex-th execution in this trace\n" +
         "      final int " + InstrumentationConstants.PREFIX + "ess; // this is the height in the dynamic call tree of this execution\n" +
         "      long " + InstrumentationConstants.PREFIX + "traceId = " + InstrumentationConstants.PREFIX + "controlFlowRegistry.recallThreadLocalTraceId(); // traceId, -1 if entry point\n" +
         "      if (" + InstrumentationConstants.PREFIX + "traceId == -1) {\n" +
         "         " + InstrumentationConstants.PREFIX + "entrypoint = true;\n" +
         "         " + InstrumentationConstants.PREFIX + "traceId = " + InstrumentationConstants.PREFIX + "controlFlowRegistry.getAndStoreUniqueThreadLocalTraceId();\n" +
         "         " + InstrumentationConstants.PREFIX + "controlFlowRegistry.storeThreadLocalEOI(0);\n" +
         "         " + InstrumentationConstants.PREFIX + "controlFlowRegistry.storeThreadLocalESS(1); // next operation is ess + 1\n" +
         "         " + InstrumentationConstants.PREFIX + "eoi = 0;\n" +
         "         " + InstrumentationConstants.PREFIX + "ess = 0;\n" +
         "      } else {\n" +
         "         " + InstrumentationConstants.PREFIX + "entrypoint = false;\n" +
         "         " + InstrumentationConstants.PREFIX + "eoi = " + InstrumentationConstants.PREFIX + "controlFlowRegistry.incrementAndRecallThreadLocalEOI(); // ess > 1\n" +
         "         " + InstrumentationConstants.PREFIX + "ess = " + InstrumentationConstants.PREFIX + "controlFlowRegistry.recallAndIncrementThreadLocalESS(); // ess >= 0\n" +
         "         if ((" + InstrumentationConstants.PREFIX + "eoi == -1) || (" + InstrumentationConstants.PREFIX + "ess == -1)) {\n" +
         "            System.err.println(\"eoi and/or ess have invalid values: eoi == {} ess == {}\"+ " + InstrumentationConstants.PREFIX + "eoi+ \"\" + "
         + InstrumentationConstants.PREFIX + "ess);\n" +
         "            " + InstrumentationConstants.PREFIX + "controller.terminateMonitoring();\n" +
         "         }\n" +
         "      }\n" +
         "      // measure before\n" +
         "      final long " + InstrumentationConstants.PREFIX + "tin = " + InstrumentationConstants.PREFIX + "TIME_SOURCE.getTime();\n",
         "// measure after\n" +
               "         final long " + InstrumentationConstants.PREFIX + "tout = " + InstrumentationConstants.PREFIX + "TIME_SOURCE.getTime();\n" +
               "         " + InstrumentationConstants.PREFIX + "controller.newMonitoringRecord(new OperationExecutionRecord("
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
               "            " + InstrumentationConstants.PREFIX + "controlFlowRegistry.unsetThreadLocalTraceId();\n" +
               "            " + InstrumentationConstants.PREFIX + "controlFlowRegistry.unsetThreadLocalEOI();\n" +
               "            " + InstrumentationConstants.PREFIX + "controlFlowRegistry.unsetThreadLocalESS();\n" +
               "         } else {\n" +
               "            " + InstrumentationConstants.PREFIX + "controlFlowRegistry.storeThreadLocalESS(" + InstrumentationConstants.PREFIX + "ess); // next operation is ess\n"
               +
               "         }",
         null), REDUCED_OPERATIONEXECUTION(
               "      final long " + InstrumentationConstants.PREFIX + "tin =" + InstrumentationConstants.PREFIX + "TIME_SOURCE.getTime();\n",
               "// measure after\n"
                     + "final long " + InstrumentationConstants.PREFIX + "tout = " + InstrumentationConstants.PREFIX + "TIME_SOURCE.getTime();\n"
                     + InstrumentationConstants.PREFIX + "controller.newMonitoringRecord(new ReducedOperationExecutionRecord("
                     + InstrumentationConstants.PREFIX + "signature, "
                     + InstrumentationConstants.PREFIX + "tin, "
                     + InstrumentationConstants.PREFIX + "tout))",
               null), SAMPLING("      final long " + InstrumentationConstants.PREFIX + "tin = " + InstrumentationConstants.PREFIX + "controller.getTimeSource().getTime();", null,
                     null);

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
