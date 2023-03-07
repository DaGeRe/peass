package de.dagere.peass.dependency.analysis.testData;

import java.io.File;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.dagere.kopeme.datastorage.ParamNameHelper;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;

/**
 * Represents the call to a test method; this should be used in favor of TestCase, if it is known that a method is present.
 *
 */
public class TestMethodCall extends TestCase {
   private static final long serialVersionUID = 1734045509685228003L;

   protected final String method;
   protected final String params;

   public TestMethodCall(final Kopemedata data) {
      this(data.getClazz().contains(ChangedEntity.MODULE_SEPARATOR)
            ? data.getClazz().substring(data.getClazz().indexOf(ChangedEntity.MODULE_SEPARATOR) + 1, data.getClazz().length())
            : data.getClazz(),
            data.getMethods().get(0).getMethod(),
            data.getClazz().contains(ChangedEntity.MODULE_SEPARATOR) ? data.getClazz().substring(0, data.getClazz().indexOf(ChangedEntity.MODULE_SEPARATOR)) : "",
            getParamsFromResult(data.getMethods().get(0).getDatacollectorResults().get(0)));
   }

   public TestMethodCall(final String clazz, final String method, final String module) {
      this(clazz,
            ((method != null && method.indexOf('(') != -1) ? method.substring(0, method.indexOf('(')) : method),
            module,
            ((method != null && method.indexOf('(') != -1) ? method.substring(method.indexOf('(') + 1, method.length() - 1) : null));
   }

   public TestMethodCall(final String clazz, final String method) {
      this(clazz, method, "", null);
   }

   public TestMethodCall(@JsonProperty("clazz") final String clazz,
         @JsonProperty("method") final String method,
         @JsonProperty("module") final String module,
         @JsonProperty("params") final String params) {
      super(clazz, module);
      this.method = method;
      this.params = params;

      if (method != null && (method.contains("(") || method.contains(")"))) {
         throw new RuntimeException("Method must not contain paranthesis: " + method);
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

   public String getParams() {
      return params;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      final TestMethodCall other = (TestMethodCall) obj;
      if (clazz == null) {
         if (other.getClazz() != null) {
            return false;
         }
      } else if (!clazz.equals(other.getClazz())) {
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
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
      result = prime * result + ((method == null) ? 0 : method.hashCode());
      return result;
   }

   @JsonIgnore
   public String getExecutable() {
      return clazz + "#" + method;
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

   public ChangedEntity toEntity() {
      return new ChangedEntity(clazz, module, method);
   }

   public static String getParamsFromResult(DatacollectorResult datacollector) {
      LinkedHashMap<String, String> paramObject = null;
      if (datacollector.getResults().size() > 0) {
         paramObject = datacollector.getResults().get(0).getParameters();
      } else if (datacollector.getChunks().size() > 0) {
         paramObject = datacollector.getChunks().get(0).getResults().get(0).getParameters();
      }
      String paramString = ParamNameHelper.paramsToString(paramObject);
      return paramString;
   }

   public static TestMethodCall createFromClassString(String clazzAndModule, String methodAndParams) {
      String clazz, module, method, params;
      int moduleIndex = clazzAndModule.indexOf(ChangedEntity.MODULE_SEPARATOR);
      if (moduleIndex == -1) {
         clazz = clazzAndModule;
         module = "";
      } else {
         clazz = clazzAndModule.substring(moduleIndex + 1);
         module = clazzAndModule.substring(0, moduleIndex);
      }

      if (methodAndParams.contains("(")) {
         method = methodAndParams.substring(0, methodAndParams.indexOf("("));
         params = methodAndParams.substring(methodAndParams.indexOf("(") + 1, methodAndParams.length() - 1);
      } else {
         method = methodAndParams;
         params = null;
      }
      return new TestMethodCall(clazz, method, module, params);
   }

   public static TestMethodCall createFromString(String testcase) {
      String clazz, module, method, params;
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
      return new TestMethodCall(clazz, method, module, params);
   }

}
