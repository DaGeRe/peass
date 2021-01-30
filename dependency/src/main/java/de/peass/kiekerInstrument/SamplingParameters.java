package de.peass.kiekerInstrument;

public class SamplingParameters {
   private final String counterName, sumName;
   private final String signature;

   public SamplingParameters(String signature, int counterIndex) {
      final String nameBeforeParanthesis = signature.substring(0, signature.indexOf('('));
      final String methodNameSubstring = nameBeforeParanthesis.substring(nameBeforeParanthesis.lastIndexOf('.') + 1);
      if (methodNameSubstring.equals("<init>")) {
         counterName = "initCounter" + counterIndex;
         sumName = "initSum" + counterIndex;
      } else {
         counterName = methodNameSubstring + "Counter" + counterIndex;
         sumName = methodNameSubstring + "Sum" + counterIndex;
      }

      this.signature = signature;
   }

   public String getCounterName() {
      return counterName;
   }

   public String getSumName() {
      return sumName;
   }

   public String getFinalBlock(String signature, int count) {
      return "// measure after\n" +
            "         final long tout = MonitoringController.getInstance().getTimeSource().getTime();\n" +
            "        " + sumName + "+=tout-tin;\n" +
            "if (" + counterName + "++%" + count + "==0){\n" +
            "final String signature = \"" + signature + "\";\n" +
            "final long calculatedTout=tin+" + sumName + ";\n" +
            "MonitoringController.getInstance().newMonitoringRecord(new ReducedOperationExecutionRecord(signature, tin, calculatedTout));\n"
            + sumName + "=0;}\n";
   }

   public String getSignature() {
      return signature;
   }
}
