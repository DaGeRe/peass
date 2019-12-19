package de.peass.steadyStateNodewise;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.serialization.MeasuredValues;
import de.peass.utils.Constants;

public class GetGraphs {
   public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
      File file = new File(args[0]);
      CauseSearchData data = Constants.OBJECTMAPPER.readValue(file, CauseSearchData.class);

      MeasuredValues values = data.getNodes().getValues();

      System.out.println(data.getTestcase());

      for (int i = 0; i < 100; i++) {
         boolean hasNon0 = false;
         for (Map.Entry<Integer, List<StatisticalSummary>> vals : values.getValues().entrySet()) {
            double value = 0;
            if (vals.getValue().size() > i) {
               value = vals.getValue().get(i).getMean();
               hasNon0 = true;
            }
            System.out.print(value + " ");
         }
         System.out.println();
         if (!hasNon0) {
            break;
         }
      }
      System.out.print("plot ");
      for (Map.Entry<Integer, List<StatisticalSummary>> vals : values.getValues().entrySet()) {
         System.out.print("'vals2.csv' u 0:" + vals.getKey() + ", ");
      }
      System.out.println();

   }
}
