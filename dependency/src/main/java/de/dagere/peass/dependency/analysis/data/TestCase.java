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
public abstract class TestCase implements Comparable<TestCase>, Serializable {

   private static final long serialVersionUID = -522183920107191602L;

   protected final String module;
   protected final String clazz;

   protected TestCase(String clazz, String module) {
      if (clazz.contains(File.separator)) {
         throw new RuntimeException("Testcase " + clazz + " should be full qualified name, not path!");
      }
      if (clazz.contains(ChangedEntity.METHOD_SEPARATOR)) {
         throw new RuntimeException("Class and method should be separated: " + clazz);
      }
      if (clazz.contains(ChangedEntity.MODULE_SEPARATOR)) {
         throw new RuntimeException("Class and module should be separated: " + clazz);
      }
      this.clazz = clazz;
      this.module = module;
   }

   // @JsonCreator
   // public TestCase(@JsonProperty("clazz") final String clazz,
   // @JsonProperty("method") final String method,
   // @JsonProperty("module") final String module,
   // @JsonProperty("params") final String params) {

   // if (method != null && (method.contains("(") || method.contains(")"))) {
   // throw new RuntimeException("Method must not contain paranthesis: " + method);
   // }
   // if (module == null) {
   // throw new RuntimeException("Module may not be null, since Files are created on it; please set it to the empty String.");
   // }
   //
   // this.clazz = clazz;
   // this.method = method;
   // this.module = module;
   // this.params = params;
   // }

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

   public String getModule() {
      return module;
   }

   public abstract ChangedEntity toEntity();
   
   @JsonIgnore
   public abstract String getExecutable();
   
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

   @JsonIgnore
   public TestClazzCall onlyClazz() {
      return new TestClazzCall(clazz, module);
   }

   public ChangedEntity onlyClazzEntity() {
      return new ChangedEntity(clazz, module);
   }

}