package net.kieker.sourceinstrumentation.instrument;

import net.kieker.sourceinstrumentation.InstrumentationConstants;

public class SamplingParameters {
   private final String counterName, sumName;
   private final String signature;

   public SamplingParameters(final String signature, final int counterIndex) {
      final String nameBeforeParanthesis = signature.substring(0, signature.indexOf('('));
      final String methodNameSubstring = nameBeforeParanthesis.substring(nameBeforeParanthesis.lastIndexOf('.') + 1);
      if (methodNameSubstring.equals("<init>")) {
         counterName = InstrumentationConstants.PREFIX + "initCounter" + counterIndex;
         sumName = InstrumentationConstants.PREFIX + "initSum" + counterIndex;
      } else {
         counterName = InstrumentationConstants.PREFIX + methodNameSubstring + "Counter" + counterIndex;
         sumName = InstrumentationConstants.PREFIX + methodNameSubstring + "Sum" + counterIndex;
      }

      this.signature = signature;
   }

   public String getCounterName() {
      return counterName;
   }

   public String getSumName() {
      return sumName;
   }

   public String getFinalBlock(final String signature, final int count) {
      return "// measure after\n" +
            "         final long " + InstrumentationConstants.PREFIX + "tout = MonitoringController.getInstance().getTimeSource().getTime();\n" +
            "        " + sumName + "+=" + InstrumentationConstants.PREFIX + "tout-" + InstrumentationConstants.PREFIX + "tin;\n" +
            "if (" + counterName + "++%" + count + "==0){\n" +
            "final String " + InstrumentationConstants.PREFIX + "signature = \"" + signature + "\";\n" +
            "final long " + InstrumentationConstants.PREFIX + "calculatedTout=" + InstrumentationConstants.PREFIX + "tin+" + sumName + ";\n" +
            "MonitoringController.getInstance().newMonitoringRecord(new ReducedOperationExecutionRecord(" + InstrumentationConstants.PREFIX + "signature, "
            + InstrumentationConstants.PREFIX + "tin, " + InstrumentationConstants.PREFIX + "calculatedTout));\n"
            + sumName + "=0;}\n";
   }

   public String getSignature() {
      return signature;
   }
}
