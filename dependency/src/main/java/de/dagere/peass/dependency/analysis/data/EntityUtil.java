package de.dagere.peass.dependency.analysis.data;

public class EntityUtil {
   public static ChangedEntity determineEntity(final String clazzMethodName) {
      final String module, clazz;
      if (clazzMethodName.contains(ChangedEntity.MODULE_SEPARATOR)) {
         module = clazzMethodName.substring(0, clazzMethodName.indexOf(ChangedEntity.MODULE_SEPARATOR));
         clazz = clazzMethodName.substring(clazzMethodName.indexOf(ChangedEntity.MODULE_SEPARATOR) + 1, clazzMethodName.indexOf(ChangedEntity.METHOD_SEPARATOR));
      } else {
         module = "";
         clazz = clazzMethodName.substring(0, clazzMethodName.indexOf(ChangedEntity.METHOD_SEPARATOR));
      }

      final int openingParenthesis = clazzMethodName.indexOf("(");
      String method;
      if (openingParenthesis != -1) {
         method = clazzMethodName.substring(clazzMethodName.indexOf(ChangedEntity.METHOD_SEPARATOR) + 1, openingParenthesis);
      } else {
         method = clazzMethodName.substring(clazzMethodName.indexOf(ChangedEntity.METHOD_SEPARATOR) + 1);
      }
      System.out.println(clazzMethodName);

      final ChangedEntity entity = new ChangedEntity(clazz, module, method);
      if (openingParenthesis != -1) {
         final String parameterString = clazzMethodName.substring(openingParenthesis + 1, clazzMethodName.length() - 1);
         entity.createParameters(parameterString);
      }
      return entity;
   }
}
