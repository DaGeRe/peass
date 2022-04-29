package de.dagere.peass.dependency.analysis.data;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.dagere.kopeme.datastorage.ParamNameHelper;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Kopemedata;

/**
 * Represents a testcase with its class and its method. If no method is given, the whole class with all methods is represented.
 * 
 * @author reichelt
 *
 */
public class TestCase implements Comparable<TestCase>, Serializable {

   private static final long serialVersionUID = -522183920107191602L;

   private final String module;
   private final String clazz;
   private final String method;

   // Saves parameters without paranthesis
   private final String params;

   public TestCase(final Kopemedata data) {
      if (data.getClazz().contains(ChangedEntity.MODULE_SEPARATOR)) {
         module = data.getClazz().substring(0, data.getClazz().indexOf(ChangedEntity.MODULE_SEPARATOR));
         this.clazz = data.getClazz().substring(data.getClazz().indexOf(ChangedEntity.MODULE_SEPARATOR) + 1, data.getClazz().length());
      } else {
         clazz = data.getClazz();
         module = "";
      }
      method = data.getMethods().get(0).getMethod();
      DatacollectorResult datacollector = data.getMethods().get(0).getDatacollectorResults().get(0);
      LinkedHashMap<String, String> paramObject = null;
      if (datacollector.getResults().size() > 0) {
         paramObject = datacollector.getResults().get(0).getParameters();
      } else if (datacollector.getChunks().size() > 0) {
         paramObject = datacollector.getChunks().get(0).getResults().get(0).getParameters();
      }
      String paramString = ParamNameHelper.paramsToString(paramObject);
      this.params = paramString;

   }

   public TestCase(final String clazz, final String method) {
      if (clazz.contains(File.separator)) { // possibly assertion, if speed becomes issue..
         throw new RuntimeException("Testcase should be full qualified name, not path: " + clazz);
      }
      if (clazz.contains(ChangedEntity.METHOD_SEPARATOR)) {
         throw new RuntimeException("Class and method should be separated: " + clazz);
      }
      if (clazz.contains(ChangedEntity.MODULE_SEPARATOR)) {
         module = clazz.substring(0, clazz.indexOf(ChangedEntity.MODULE_SEPARATOR));
         this.clazz = clazz.substring(clazz.indexOf(ChangedEntity.MODULE_SEPARATOR) + 1, clazz.length());
      } else {
         this.clazz = clazz;
         module = "";
      }
      this.method = method;
      this.params = null;
   }

   public TestCase(final ChangedEntity entity) {
      this(entity.getClazz(), entity.getMethod(), entity.getModule(), null);
   }

   public TestCase(final String clazz, final String method, final String module) {
      this(clazz,
            ((method != null && method.indexOf('(') != -1) ? method.substring(0, method.indexOf('(')) : method),
            module,
            ((method != null && method.indexOf('(') != -1) ? method.substring(method.indexOf('(') + 1, method.length() - 1) : null));
   }

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
      this.clazz = clazz;
      this.method = method;
      this.module = module;
      this.params = params;
   }

   public TestCase(final String testcase) {
      if (testcase.contains(File.separator)) {
         throw new RuntimeException("Testcase should be full qualified name, not path!");
      }
      final int index = testcase.lastIndexOf(ChangedEntity.METHOD_SEPARATOR);
      if (index == -1) {
         int moduleIndex = testcase.indexOf(ChangedEntity.MODULE_SEPARATOR);
         if (moduleIndex == -1) {
            clazz = testcase;
            module = "";
         } else {
            clazz = testcase.substring(moduleIndex + 1);
            module = testcase.substring(0, moduleIndex);
         }
         method = null;
         params = null;
      } else {
         String start = testcase.substring(0, index);
         int moduleIndex = testcase.indexOf(ChangedEntity.MODULE_SEPARATOR);
         if (moduleIndex == -1) {
            clazz = start;
            module = "";
         } else {
            clazz = start.substring(moduleIndex + 1);
            module = start.substring(0, moduleIndex);
         }

         if (testcase.contains("(")) {
            method = testcase.substring(index + 1, testcase.indexOf("("));
            params = testcase.substring(testcase.indexOf("(") + 1, testcase.length() - 1);
         } else {
            method = testcase.substring(index + 1);
            params = null;
         }
      }
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

   public String getMethod() {
      return method;
   }

   @JsonIgnore
   public String getMethodWithParams() {
      if (params == null) {
         return method;
      } else {
         return method + "(" + params + ")";
      }
   }

   public String getModule() {
      return module;
   }

   public String getParams() {
      return params;
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
      if (getClass() != obj.getClass()) {
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
         if (other.getParams() != null) {
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

   public TestCase copy() {
      TestCase test = new TestCase(clazz, method, module, params);
      return test;
   }

   @JsonIgnore
   public TestCase onlyClazz() {
      return new TestCase(clazz, null, module);
   }

   public ChangedEntity onlyClazzEntity() {
      return new ChangedEntity(clazz, module);
   }

}