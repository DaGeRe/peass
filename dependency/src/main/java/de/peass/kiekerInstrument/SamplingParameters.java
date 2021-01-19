package de.peass.kiekerInstrument;

public class SamplingParameters {
   private final String counterName, sumName;

   public SamplingParameters(String signature, int counterIndex) {
      final String nameBeforeParanthesis = signature.substring(0, signature.indexOf('('));
      final String methodNameSubstring = nameBeforeParanthesis.substring(nameBeforeParanthesis.lastIndexOf('.') + 1);
      counterName = methodNameSubstring + "Counter" + counterIndex;
      sumName = methodNameSubstring + "Sum" + counterIndex;
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
}
