package de.dagere.peass.dependency.analysis.data;

import de.dagere.kopeme.generated.Result.Params;
import de.dagere.kopeme.generated.Result.Params.Param;

public class ChangedEntityHelper {

   public static String paramsToString(final Params params) {
      String result;
      if (params != null) {
         result = "";
         for (Param param : params.getParam()) {
            result += param.getKey() + "-" + param.getValue() + " ";
         }
         result = result.substring(0, result.length() - 1);
      } else {
         result = null;
      }
      return result;
   }

   public static String getParameterString(final String[] parameterTypes) {
      if (parameterTypes.length > 0) {
         String parameterString = "(";
         for (String parameter : parameterTypes) {
            parameterString += parameter + ",";
         }
         parameterString = parameterString.substring(0, parameterString.length() - 1) + ")";
         return parameterString;
      } else {
         return "";
      }
   }

}
