package de.peass.visualization;

import java.io.File;

public class KiekerPatternConverter {
   public static String getFileNameStart(final String kiekerPattern) {
      final String separator = File.separator;
      String fileNameStart = convertPattern(kiekerPattern, separator);
      return fileNameStart;
   }
   
   public static String getKey(final String kiekerPattern) {
      final String separator = ".";
      String fileNameStart = convertPattern(kiekerPattern, separator);
      return fileNameStart;
   }

   private static String convertPattern(final String kiekerPattern, final String separator) {
      final String call = kiekerPattern.substring(kiekerPattern.lastIndexOf(' ') + 1, kiekerPattern.lastIndexOf('('));
      String method = call.substring(call.lastIndexOf(".") + 1);
      String clazz = call.substring(0, call.lastIndexOf('.'));

      String parameterString = getParameterKeyString(kiekerPattern);
      
      String fileNameStart = clazz + separator + method + "_" + parameterString;
      return fileNameStart;
   }

   public static String getParameterKeyString(final String kiekerPattern) {
      String parameters = kiekerPattern.substring(kiekerPattern.lastIndexOf('(') + 1, kiekerPattern.length() - 1);
      String parameterString = "";
      for (String parameter : parameters.split(",")) {
         final int index = parameter.lastIndexOf('.');
         parameterString += (index != -1 ? parameter.substring(index + 1) : parameter) + "_";
      }
      parameterString = parameterString.substring(0, parameterString.length() - 1);
      return parameterString;
   }
}
