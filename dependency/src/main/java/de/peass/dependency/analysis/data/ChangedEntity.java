package de.peass.dependency.analysis.data;

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

//TODO What happens to changes, that do occur in classes which are only file-local? -> Should be separated by $
public class ChangedEntity implements Comparable<ChangedEntity> {

   public static final String[] potentialClassFolders = new String[] { "src/main/java/", "src/test/java/", "src/test/", "src/java/", "src/androidTest/java/" };

   public static String replaceClazzFolderFromName(final String fileName) {
      boolean isInClassFolder = false;
      for (final String clazzFolder : potentialClassFolders) {
         if (fileName.contains(clazzFolder)) {
            isInClassFolder = true;
         }
      }
      if (isInClassFolder) {
         String tempClazzName = fileName.replace(".java", "");
         for (final String clazzFolder : potentialClassFolders) {
            tempClazzName = tempClazzName.replaceAll(clazzFolder, "");
         }
         return tempClazzName.replace(File.separatorChar, '.');
      } else {
         return null;
      }
   }

   private static final Logger LOG = LogManager.getLogger(ChangedEntity.class);

   private final String filename;
   private String method;
   private final String module;
   private final String javaClazzName;
   private final List<String> parameters = new LinkedList<String>();

   @JsonCreator
   public ChangedEntity(@JsonProperty("clazz") final String clazz, @JsonProperty("module") final String module) {
      this.filename = clazz;
      this.module = module != null ? module : "";
      if (!clazz.contains(METHOD_SEPARATOR)) {
         final String editedName = replaceClazzFolderFromName(clazz);
         if (editedName != null) {
            javaClazzName = editedName;
         } else {
            if (clazz.contains(File.separator)) {
               LOG.error("Classfolder not found: " + clazz + " Module: " + module);
            }
            javaClazzName = clazz;
         }
      } else {
         javaClazzName = clazz.substring(0, clazz.lastIndexOf("#"));
         method = clazz.substring(clazz.lastIndexOf("#") + 1);
      }

      LOG.trace(javaClazzName + " " + clazz);
      LOG.trace(javaClazzName);
   }

   public ChangedEntity(final String testClassName, final String moduleOfClass, final String testMethodName) {
      this(testClassName, moduleOfClass);
      method = testMethodName;
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

   public String getFilename() {
      return filename;
   }

   public String getClazz() {
      return filename;
   }

   public String getMethod() {
      return method;
   }

   public void setMethod(final String method) {
      this.method = method;
   }

   @JsonInclude(Include.NON_EMPTY)
   public String getModule() {
      return module;
   }

   public static final String MODULE_SEPARATOR = "§";
   public static final String METHOD_SEPARATOR = "#";
   public static final String CLAZZ_SEPARATOR = "$";

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
      final ChangedEntity copy = new ChangedEntity(filename, module);
      copy.setMethod(this.method);
      return copy;
   }

   @JsonIgnore
   public ChangedEntity onlyClazz() {
      return new ChangedEntity(filename, module);
   }

   @JsonIgnore
   public ChangedEntity getSourceContainingClazz() {
      if (!javaClazzName.contains(CLAZZ_SEPARATOR)) {
         return this;
      } else {
         final String clazzName = javaClazzName.substring(0, javaClazzName.indexOf(CLAZZ_SEPARATOR));
         return new ChangedEntity(clazzName, module, "");
      }
   }

   public List<String> getParameters() {
      return parameters;
   }

}