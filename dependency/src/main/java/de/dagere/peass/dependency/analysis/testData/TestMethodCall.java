package de.dagere.peass.dependency.analysis.testData;

import java.util.LinkedHashMap;

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
      super(data.getClazz().contains(ChangedEntity.MODULE_SEPARATOR) ? data.getClazz().substring(data.getClazz().indexOf(ChangedEntity.MODULE_SEPARATOR) + 1, data.getClazz().length()) : data.getClazz(),
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
}
