package de.dagere.peass.dependency.execution;

public class CommandConcatenator {
   
   public static final String[] mavenCheckDeactivation = new String[] {
         "-Dcheckstyle.skip=true",
         "-Dmaven.javadoc.skip=true",
         "-Danimal.sniffer.skip=true",
         "-Denforcer.skip=true",
         "-Djacoco.skip=true",
         "-Drat.skip=true",
         "-DfailIfNoTests=false"
   };
   
   public static String[] concatenateCommandArrays(final String[] first, final String[] second) {
      final String[] vars = new String[first.length + second.length];
      for (int i = 0; i < first.length; i++) {
         vars[i] = first[i];
      }
      for (int i = 0; i < second.length; i++) {
         vars[first.length + i] = second[i];
      }
      return vars;
   }
}
