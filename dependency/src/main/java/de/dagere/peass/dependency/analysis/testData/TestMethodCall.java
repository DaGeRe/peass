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

   public TestMethodCall(final Kopemedata data) {
      super(data.getClazz().contains(ChangedEntity.MODULE_SEPARATOR)
            ? data.getClazz().substring(data.getClazz().indexOf(ChangedEntity.MODULE_SEPARATOR) + 1, data.getClazz().length())
            : data.getClazz(),
            data.getMethods().get(0).getMethod(),
            data.getClazz().contains(ChangedEntity.MODULE_SEPARATOR) ? data.getClazz().substring(0, data.getClazz().indexOf(ChangedEntity.MODULE_SEPARATOR)) : "",
            getParamsFromResult(data.getMethods().get(0).getDatacollectorResults().get(0)));
   }

   public TestMethodCall(final String clazz, final String method, final String module) {
      super(clazz,
            ((method != null && method.indexOf('(') != -1) ? method.substring(0, method.indexOf('(')) : method),
            module,
            ((method != null && method.indexOf('(') != -1) ? method.substring(method.indexOf('(') + 1, method.length() - 1) : null));
   }

   public TestMethodCall(final String clazz, final String method) {
      super(clazz, method, "", null);
   }

   public TestMethodCall(@JsonProperty("clazz") final String clazz,
         @JsonProperty("method") final String method,
         @JsonProperty("module") final String module,
         @JsonProperty("params") final String params) {
      super(clazz, method, module, params);
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

   public static TestMethodCall createFromString(String testcase) {
      String clazz, module, method, params;
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
      return new TestMethodCall(clazz, method, module, params);
   }

}
