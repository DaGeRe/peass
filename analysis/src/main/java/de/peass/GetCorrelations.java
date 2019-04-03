package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.properties.ChangeProperties;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.VersionChangeProperties;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.read.PropertyReadHelper;

public class GetCorrelations {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final File propertyFile = new File("/home/reichelt/daten3/diss/repos/properties/properties/commons-io/commons-io.json");
      final VersionChangeProperties props = FolderSearcher.MAPPER.readValue(propertyFile, VersionChangeProperties.class);

      int count = 0;
      for (final Map.Entry<String, ChangeProperties> version : props.getVersions().entrySet()) {
         for (final Entry<String, List<ChangeProperty>> testcase : version.getValue().getProperties().entrySet()) {
            count += testcase.getValue().size();
         }
      }
      
      final Map<String, double[]> keywordMap = new HashMap<>();
      
      for (final String keyword : PropertyReadHelper.keywords) {
         keywordMap.put(keyword, new double[count]);
      }
      

      final double[] changes = new double[count];
      final double[] changesAbs = new double[count];
      
      final double[] traces = new double[count];
      final double[] absolute = new double[count];
      final double[] traceDiff = new double[count];
      final double[] commitSize = new double[count];
      final double[] commitSizeAbs = new double[count];

      int index = 0;
      for (final Map.Entry<String, ChangeProperties> version : props.getVersions().entrySet()) {
         for (final Entry<String, List<ChangeProperty>> testcase : version.getValue().getProperties().entrySet()) {
            for (final ChangeProperty prop : testcase.getValue()) {
               if (!Double.isNaN(prop.getChangePercent())) {
                  
                  changes[index] = prop.getChangePercent();
                  changesAbs[index] = Math.abs(prop.getChangePercent());
                  
                  traces[index] = prop.getCalls();
                  absolute[index] = prop.getOldTime();
                  traceDiff[index] = prop.getCalls() - prop.getCallsOld();
                  commitSize[index] = prop.getAffectedLines();
                  commitSizeAbs[index] = Math.abs(prop.getAffectedLines());
                  
                  for (final String keyword : PropertyReadHelper.keywords) {
                     final double[] occurence = keywordMap.get(keyword);
                     final Integer added = prop.getAddedMap().containsKey(keyword) ? prop.getAddedMap().get(keyword) : 0;
                     final Integer removed = prop.getRemovedMap().containsKey(keyword) ? prop.getRemovedMap().get(keyword) : 0;
                     occurence[index] = added + removed;
                  }
                  index++;
               }
            }
         }
      }

      System.out.println(new PearsonsCorrelation().correlation(traces, changes));
      System.out.println(new PearsonsCorrelation().correlation(absolute, changes));
      System.out.println(new PearsonsCorrelation().correlation(traceDiff, changes));
      System.out.println(new PearsonsCorrelation().correlation(commitSize, changes));
      System.out.println(new PearsonsCorrelation().correlation(commitSizeAbs, changes));
      
      for (final String keyword : PropertyReadHelper.keywords) {
         final double[] occurence = keywordMap.get(keyword);
         System.out.println(keyword + ": " + new PearsonsCorrelation().correlation(occurence, changes));
      }
   }
}
