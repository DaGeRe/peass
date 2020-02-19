package de.peass.measurement.rca.kieker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kieker.tools.traceAnalysis.systemModel.Operation;

public class KiekerPatternConverter {

   private static final Logger LOG = LogManager.getLogger(KiekerPatternConverter.class);

   /**
    * Fixes parameters, i.e. removes spaces, that might be introduced by kieker.
    * @param kiekerCall Kieker call that should be fixed
    * @return fixed call
    */
   public static String fixParameters(final String kiekerCall) {
      LOG.info("Fixing: {}", kiekerCall);
      final int parametersBegin = kiekerCall.indexOf('(');
      if (parametersBegin != -1) {
         final String namePart = kiekerCall.substring(0, parametersBegin);
         String parameterPart = kiekerCall.substring(parametersBegin);
         parameterPart = parameterPart.replace(" ", "");
         final String result = namePart + parameterPart;
         return result;
      } else {
         return kiekerCall;
      }

   }

   public static String getKiekerPattern(final String kiekerCall) {
      final int parametersBegin = kiekerCall.indexOf('(');
      String namePart = kiekerCall.substring(0, parametersBegin);
      namePart = addNew(namePart);

      String parameterPart = kiekerCall.substring(parametersBegin);
      parameterPart = parameterPart.replace(" ", "");
      final String result = namePart + parameterPart;
      return result;
   }

   private static String addNew(String kiekerCall) {
      if (kiekerCall.contains("<init>")) {
         final String[] splitted = kiekerCall.split(" ");
         String repaired;
         if (splitted[0].equals("public") || splitted[0].equals("protected") || splitted[0].equals("private")) {
            repaired = splitted[0] + " new ";
            for (int i = 1; i < splitted.length; i++) {
               repaired += splitted[i] + " ";
            }
            repaired = repaired.substring(0, repaired.length() - 1);
         } else {
            repaired = "new " + kiekerCall;
         }
         kiekerCall = repaired;
      }
      return kiekerCall;
   }

   public static String getKiekerPattern(final Operation operation) {
      final StringBuilder strBuild = new StringBuilder();
      for (final String t : operation.getSignature().getModifier()) {
         strBuild.append(t)
               .append(' ');
      }
      if (operation.getSignature().hasReturnType()) {
         strBuild.append(operation.getSignature().getReturnType())
               .append(' ');
      } else {
         strBuild.append("new")
               .append(' ');
      }
      strBuild.append(operation.getComponentType().getFullQualifiedName())
            .append('.');
      strBuild.append(operation.getSignature().getName()).append('(');

      boolean first = true;
      for (final String t : operation.getSignature().getParamTypeList()) {
         if (!first) {
            strBuild.append(',');
         } else {
            first = false;
         }
         strBuild.append(t);
      }
      strBuild.append(')');

      return strBuild.toString();
   }
}
