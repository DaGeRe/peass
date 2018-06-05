package de.peran.dependency.analysis.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//TODO What happens to changes, that do occur in classes which are only file-local?
public class ChangedEntity implements Comparable<ChangedEntity> {

   private static final Logger LOG = LogManager.getLogger(ChangedEntity.class);

   private final String clazz;
   private String method;
   private final String module;
   private final String javaClazzName;

   public ChangedEntity(String clazz, String module) {
      this.clazz = clazz;
      this.module = module;
      final String tempClazzName = clazz.replace(".java", "");
      LOG.trace(tempClazzName + " " + clazz);
      javaClazzName = tempClazzName.replace("src/main/java/", "").replace("src/test/java/", "").replace("src/test/", "").replace("src/java/", "").replace('/', '.');
      LOG.trace(javaClazzName);
   }

   public ChangedEntity(String testClassName, String moduleOfClass, String testMethodName) {
      this(testClassName, moduleOfClass);
      method = testMethodName;
   }

   public String getJavaClazzName() {
      return javaClazzName;
   }

   public String getClazz() {
      return clazz;
   }

   public String getMethod() {
      return method;
   }

   public void setMethod(String method) {
      this.method = method;
   }

   public String getModule() {
      return module;
   }

   @Override
   public String toString() {
      String result = module + "-" + clazz;
      if (method != null) {
         result += "." + method;
      }
      return result;
   }

   @Override
   public boolean equals(Object obj) {
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
   public int compareTo(ChangedEntity o) {
      return javaClazzName.compareTo(o.javaClazzName);
   }

   public ChangedEntity copy() {
      final ChangedEntity copy = new ChangedEntity(clazz, module);
      copy.setMethod(this.method);
      return copy;
   }
}