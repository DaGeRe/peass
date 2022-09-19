package de.dagere.peass.dependency.analysis.data;

import java.io.File;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.dagere.peass.dependency.analysis.testData.TestClazzCall;

/**
 * Represents a testcase with its class and its method. If no method is given, the whole class with all methods is represented.
 * 
 * @author reichelt
 *
 */
public class TestCase implements Comparable<TestCase>, Serializable {

   private static final long serialVersionUID = -522183920107191602L;

   protected final String module;
   protected final String clazz;
   protected final String method;

   // Saves parameters without paranthesis
   protected final String params;

   @JsonCreator
   public TestCase(@JsonProperty("clazz") final String clazz,
         @JsonProperty("method") final String method,
         @JsonProperty("module") final String module,
         @JsonProperty("params") final String params) {
      if (clazz.contains(File.separator)) {
         throw new RuntimeException("Testcase " + clazz + " should be full qualified name, not path!");
      }
      if (clazz.contains(ChangedEntity.METHOD_SEPARATOR)) {
         throw new RuntimeException("Class and method should be separated: " + clazz);
      }
      if (clazz.contains(ChangedEntity.MODULE_SEPARATOR)) {
         throw new RuntimeException("Class and module should be separated: " + clazz);
      }
      if (method != null && (method.contains("(") || method.contains(")"))) {
         throw new RuntimeException("Method must not contain paranthesis: " + method);
      }
      if (module == null) {
         throw new RuntimeException("Module may not be null, since Files are created on it; please set it to the empty String.");
      }
         
      this.clazz = clazz;
      this.method = method;
      this.module = module;
      this.params = params;
   }

   public String getClazz() {
      return clazz;
   }

   @JsonIgnore
   public String getClassWithModule() {
      if (module != null && !"".equals(module)) {
         return module + ChangedEntity.MODULE_SEPARATOR + clazz;
      } else {
         return clazz;
      }
   }

   /**
    * getMethod should only be called on TestMethodCall
    * @return
    */
   @Deprecated
   public String getMethod() {
      return method;
   }

   public String getModule() {
      return module;
   }

   @JsonIgnore
   public String getTestclazzWithModuleName() {
      String testcase;
      if (module != null && !module.equals("")) {
         testcase = module + ChangedEntity.MODULE_SEPARATOR + clazz;
      } else {
         testcase = clazz;
      }
      return testcase;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
      result = prime * result + ((method == null) ? 0 : method.hashCode());
      return result;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      final TestCase other = (TestCase) obj;
      if (clazz == null) {
         if (other.clazz != null) {
            return false;
         }
      } else if (!clazz.equals(other.clazz)) {
         final String shortClazz = clazz.substring(clazz.lastIndexOf('.') + 1);
         final String shortClazzOther = other.getClazz().substring(other.getClazz().lastIndexOf('.') + 1);
         if (!shortClazz.equals(shortClazzOther)) { // Dirty Hack - better transfer clazz-info always
            return false;
         }
      }
      if (method == null) {
         if (other.method != null) {
            return false;
         }
      } else if (!method.equals(other.method)) {
         return false;
      }
      if (params == null) {
         if (other.params != null) {
            return false;
         }
      } else if (!params.equals(other.params)) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      String result;
      if (module != null && !"".equals(module)) {
         result = module + ChangedEntity.MODULE_SEPARATOR + clazz;
      } else {
         result = clazz;
      }
      if (method != null) {
         result += ChangedEntity.METHOD_SEPARATOR + method;
      }
      if (params != null) {
         result += "(" + params + ")";
      }
      return result;
   }

   @JsonIgnore
   public String getPackage() {
      int lastDotIndex = clazz.lastIndexOf('.');
      if (lastDotIndex != -1) {
         return clazz.substring(0, lastDotIndex);
      } else {
         return "";
      }
   }

   @JsonIgnore
   public String getExecutable() {
      if (method != null) {
         return clazz + "#" + method;
      } else {
         return clazz;
      }
   }

   @JsonIgnore
   public String getShortClazz() {
      return clazz.substring(clazz.lastIndexOf('.') + 1, clazz.length());
   }

   @JsonIgnore
   public String getPureClazz() {
      String shortClazz = getShortClazz();
      int innerSeparator = shortClazz.indexOf(ChangedEntity.CLAZZ_SEPARATOR);
      if (innerSeparator != -1) {
         return shortClazz.substring(innerSeparator + 1, shortClazz.length());
      } else {
         return shortClazz;
      }
   }

   @JsonIgnore
   public String getLinkUsable() {
      return toString().replace("#", "_");
   }

   @Override
   public int compareTo(final TestCase arg0) {
      return toString().compareTo(arg0.toString());
   }

   public ChangedEntity toEntity() {
      return new ChangedEntity(clazz, module, method);
   }

   @JsonIgnore
   public TestClazzCall onlyClazz() {
      return new TestClazzCall(clazz, module);
   }

   public ChangedEntity onlyClazzEntity() {
      return new ChangedEntity(clazz, module);
   }

}