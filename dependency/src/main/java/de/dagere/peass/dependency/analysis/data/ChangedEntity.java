package de.dagere.peass.dependency.analysis.data;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChangedEntity implements Comparable<ChangedEntity> {

   private static final Logger LOG = LogManager.getLogger(ChangedEntity.class);

   public static final String MODULE_SEPARATOR = "§";
   public static final String METHOD_SEPARATOR = "#";
   public static final String CLAZZ_SEPARATOR = "$";

   private String method;
   private final String module;
   private final String javaClazzName;
   private final List<String> parameters = new LinkedList<String>();

   public ChangedEntity(@JsonProperty("clazz") final String clazz, @JsonProperty("module") final String module) {
      if (clazz.contains(File.separator)) {
         throw new RuntimeException("Class should be full qualified name, not path! " + clazz);
      }
      this.module = module != null ? module : "";
      if (!clazz.contains(METHOD_SEPARATOR)) {
         javaClazzName = clazz;
      } else {
         javaClazzName = clazz.substring(0, clazz.lastIndexOf(ChangedEntity.METHOD_SEPARATOR));
         method = clazz.substring(clazz.lastIndexOf(ChangedEntity.METHOD_SEPARATOR) + 1);
      }
      if (method != null && (method.contains("(") && method.contains(")"))) {
         String parameterString = method.substring(method.indexOf("(") + 1, method.length() - 1).replaceAll(" ", "");
         createParameters(parameterString);
         method = method.substring(0, method.indexOf("("));
      }

      if (javaClazzName.startsWith(".")) {
         throw new RuntimeException("Java class names are not allowed to start with ., but was " + javaClazzName);
      }

      LOG.trace(javaClazzName + " " + clazz);
      LOG.trace(javaClazzName);

      if (javaClazzName.startsWith(".")) {
         throw new RuntimeException("Java class names are not allowed to start with ., but was " + javaClazzName);
      }
   }

   @JsonCreator
   public ChangedEntity(@JsonProperty("clazz") final String clazz, @JsonProperty("module") final String module, @JsonProperty("method") final String testMethodName) {
      this(clazz, module);

      if (testMethodName != null && (testMethodName.contains("(") && testMethodName.contains(")"))) {
         String parameterString = testMethodName.substring(testMethodName.indexOf("(") + 1, testMethodName.length() - 1).replaceAll(" ", "");
         createParameters(parameterString);
         method = testMethodName.substring(0, testMethodName.indexOf("("));
      } else {
         method = testMethodName;
      }
   }

   public ChangedEntity(final String fullName) {
      int moduleIndex = fullName.indexOf(ChangedEntity.MODULE_SEPARATOR);
      if (moduleIndex == -1) {
         module = "";
         if (fullName.contains(File.separator)) {
            throw new RuntimeException("Class should be full qualified name, not path! " + fullName);
         }
         final int methodIndex = fullName.lastIndexOf(ChangedEntity.METHOD_SEPARATOR);
         if (methodIndex == -1) {
            javaClazzName = fullName;
            method = null;
         } else {
            javaClazzName = fullName.substring(0, methodIndex);
            method = fullName.substring(methodIndex + 1);

            if (fullName.contains("(")) {
               method = fullName.substring(methodIndex + 1, fullName.indexOf("("));
               String paramString = fullName.substring(fullName.indexOf("(") + 1, fullName.length() - 1);
               createParameters(paramString);
            } else {
               method = fullName.substring(methodIndex + 1);
            }
         }
      } else {
         module = fullName.substring(0, moduleIndex);
         String end = fullName.substring(moduleIndex + 1);
         final int methodIndex = end.lastIndexOf(ChangedEntity.METHOD_SEPARATOR);
         if (methodIndex == -1) {
            javaClazzName = end;
            method = null;
         } else {
            javaClazzName = end.substring(0, methodIndex);
            method = end.substring(methodIndex + 1);

            if (end.contains("(")) {
               method = end.substring(methodIndex + 1, end.indexOf("("));
               String paramString = end.substring(end.indexOf("(") + 1, end.length() - 1);
               createParameters(paramString);
            } else {
               method = end.substring(methodIndex + 1);
            }
         }
      }
      if (javaClazzName.startsWith(".")) {
         throw new RuntimeException("Java class names are not allowed to start with ., but was " + javaClazzName);
      }
   }

   @JsonIgnore
   public String getJavaClazzName() {
      return javaClazzName;
   }

   @JsonIgnore
   public String getSimpleClazzName() {
      return javaClazzName.substring(javaClazzName.lastIndexOf('.') + 1);
   }

   @JsonIgnore
   public String getSimpleFullName() {
      return javaClazzName.substring(javaClazzName.lastIndexOf('.') + 1) + METHOD_SEPARATOR + method;
   }

   @JsonIgnore
   public String getPackage() {
      final String result = javaClazzName.contains(".") ? javaClazzName.substring(0, javaClazzName.lastIndexOf('.')) : "";
      return result;
   }

   public String getClazz() {
      return javaClazzName;
   }

   public String getMethod() {
      return method;
   }

   public void setMethod(final String method) {
      this.method = method;
   }

   @JsonIgnore
   public String getParameterString() {
      return ChangedEntityHelper.getParameterString(parameters.toArray(new String[0]));
   }

   @JsonInclude(Include.NON_EMPTY)
   public String getModule() {
      return module;
   }

   @Override
   public String toString() {
      String result;
      if (module != null && !module.equals("")) {
         result = module + MODULE_SEPARATOR + javaClazzName;
      } else {
         result = javaClazzName;
      }
      if (method != null && !"".equals(method)) {
         result += METHOD_SEPARATOR + method;
      }
      if (parameters.size() > 0) {
         result += ChangedEntityHelper.getParameterString(parameters.toArray(new String[0]));
      }
      return result;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj instanceof ChangedEntity) {
         final ChangedEntity other = (ChangedEntity) obj;
         if (method != null) {
            if (other.method == null) {
               return false;
            }
            if (!other.method.equals(method)) {
               return false;
            }
         } else {
            if (other.method != null) {
               return false;
            }
         }
         if (module != null) {
            return other.module.equals(module) && other.javaClazzName.equals(javaClazzName);
         } else {
            return other.javaClazzName.equals(javaClazzName);
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return javaClazzName.hashCode();
   }

   @Override
   public int compareTo(final ChangedEntity o) {
      final String own = toString();
      final String other = o.toString();
      return own.compareTo(other);
   }

   public ChangedEntity copy() {
      final ChangedEntity copy = new ChangedEntity(javaClazzName, module);
      copy.setMethod(this.method);
      return copy;
   }

   @JsonIgnore
   public ChangedEntity onlyClazz() {
      return new ChangedEntity(javaClazzName, module);
   }

   @JsonIgnore
   public ChangedEntity getSourceContainingClazz() {
      if (!javaClazzName.contains(CLAZZ_SEPARATOR)) {
         return new ChangedEntity(javaClazzName, module);
      } else {
         final String clazzName = javaClazzName.substring(0, javaClazzName.indexOf(CLAZZ_SEPARATOR));
         return new ChangedEntity(clazzName, module, "");
      }
   }

   public List<String> getParameters() {
      return parameters;
   }

   @JsonIgnore
   public String getParametersPrintable() {
      String result = "";
      for (String parameter : parameters) {
         result += parameter + "_";
      }
      return result.length() > 0 ? result.substring(0, result.length() - 1) : "";
   }

   public void createParameters(final String parameterString) {
      LOG.trace("Creating parameters: {}", parameterString); // TODO trace
      String cleanParameters = parameterString.replaceAll(" ", "").replaceAll("\\(", "").replaceAll("\\)", "");
      if (parameterString.contains("<")) {
         addParameterWithGenerics(cleanParameters);
      } else {
         addGenericFreePart(cleanParameters);
      }
      LOG.trace("Parameters parsed: {}", parameters); // TODO trace
   }

   private void addParameterWithGenerics(final String parameterString) {
      final String[] genericSplitted = parameterString.split(">");
      for (String genericPart : genericSplitted) {
         if (genericPart.length() > 0) {
            if (genericPart.startsWith(",")) {
               genericPart = genericPart.substring(1);
            }
            if (genericPart.contains("<")) {
               final String beforeGeneric = genericPart.substring(0, genericPart.indexOf('<'));
               final String[] beforeGenericEnding = beforeGeneric.split(",");
               if (beforeGenericEnding.length > 1) {
                  for (int i = 0; i < beforeGenericEnding.length - 1; i++) {
                     this.parameters.add(beforeGenericEnding[i]);
                  }
               }
               String genericParameter = beforeGenericEnding[beforeGenericEnding.length - 1] + genericPart.substring(genericPart.indexOf('<')) + '>';
               this.parameters.add(genericParameter);
            } else {
               addGenericFreePart(genericPart);
            }
         } else {
            String lastParameter = this.parameters.get(this.parameters.size() - 1);
            lastParameter += ">";
            this.parameters.set(this.parameters.size() - 1, lastParameter);
         }
      }
   }

   private void addGenericFreePart(final String parameterString) {
      if (parameterString.length() == 0) {
         this.parameters.clear();
      } else {
         final String[] parameters = parameterString.split(",");
         for (final String parameter : parameters) {
            // int dotIndex = parameter.lastIndexOf('.');
            // if (dotIndex != -1) {
            // this.parameters.add(parameter.substring(dotIndex + 1));
            // } else {
            this.parameters.add(parameter);
            // }
         }
      }
   }

   @JsonIgnore
   public String[] getParameterTypes() {
      return parameters.toArray(new String[0]);
   }

}