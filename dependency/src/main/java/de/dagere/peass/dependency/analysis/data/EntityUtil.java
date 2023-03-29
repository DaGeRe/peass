package de.dagere.peass.dependency.analysis.data;

import de.dagere.nodeDiffDetector.data.MethodCall;

public class EntityUtil {
   public static MethodCall determineEntity(final String clazzMethodName) {
      final String module, clazz;
      if (clazzMethodName.contains(MethodCall.MODULE_SEPARATOR)) {
         module = clazzMethodName.substring(0, clazzMethodName.indexOf(MethodCall.MODULE_SEPARATOR));
         clazz = clazzMethodName.substring(clazzMethodName.indexOf(MethodCall.MODULE_SEPARATOR) + 1, clazzMethodName.indexOf(MethodCall.METHOD_SEPARATOR));
      } else {
         module = "";
         clazz = clazzMethodName.substring(0, clazzMethodName.indexOf(MethodCall.METHOD_SEPARATOR));
      }

      final int openingParenthesis = clazzMethodName.indexOf("(");
      String method;
      if (openingParenthesis != -1) {
         method = clazzMethodName.substring(clazzMethodName.indexOf(MethodCall.METHOD_SEPARATOR) + 1, openingParenthesis);
      } else {
         method = clazzMethodName.substring(clazzMethodName.indexOf(MethodCall.METHOD_SEPARATOR) + 1);
      }
      System.out.println(clazzMethodName);

      final MethodCall entity = new MethodCall(clazz, module, method);
      if (openingParenthesis != -1) {
         final String parameterString = clazzMethodName.substring(openingParenthesis + 1, clazzMethodName.length() - 1);
         entity.createParameters(parameterString);
      }
      return entity;
   }

   public static MethodCall determineEntityWithDotSeparator(final String calledMethod) {
      
      int parenthesisIndex = calledMethod.indexOf('(');
      String parameterString = calledMethod.substring(parenthesisIndex + 1);
      String withoutParameterString = calledMethod.substring(0, parenthesisIndex);
      
      String nameWithoutModifiers = withoutParameterString.substring(withoutParameterString.lastIndexOf(' ') + 1);
      
      int dotIndex = nameWithoutModifiers.lastIndexOf('.');
      String clazz = nameWithoutModifiers.substring(0, dotIndex);
      String method = nameWithoutModifiers.substring(dotIndex + 1);
      MethodCall changedEntity = new MethodCall(clazz, null, method);
      changedEntity.createParameters(parameterString);
      return changedEntity;
   }
}
