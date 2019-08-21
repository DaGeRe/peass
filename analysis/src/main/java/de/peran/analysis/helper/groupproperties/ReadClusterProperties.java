package de.peran.analysis.helper.groupproperties;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.changes.processors.PropertyProcessor;
import de.peass.analysis.properties.ChangeProperties;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.VersionChangeProperties;
import de.peran.FolderSearcher;

public class ReadClusterProperties {
   static class ClusterProperties {
      
      List<Double> changePercent = new LinkedList<>();
      Set<String> comments = new HashSet<>();

      public List<Double> getChangePercent() {
         return changePercent;
      }

      public void setChangePercent(List<Double> changePercent) {
         this.changePercent = changePercent;
      }

      public Set<String> getComments() {
         return comments;
      }

      public void setComments(Set<String> comments) {
         this.comments = comments;
      }
      
      @Override
      public String toString() {
         DescriptiveStatistics stat = new DescriptiveStatistics();
         for (Double val : changePercent) {
            stat.addValue(val);
         }
         return comments.toString() + " " + stat.getMean();
      }
   }

   static class TypeClusterReader implements PropertyProcessor {

      Map<String, ClusterProperties> clusters = new LinkedHashMap<>();

      @Override
      public void process(String version, String testcase, ChangeProperty change, ChangeProperties prop) {
         for (String type : change.getTypes()) {
            ClusterProperties props = clusters.get(type);
            if (props == null) {
               props = new ClusterProperties();
               clusters.put(type, props);
            }
            props.getChangePercent().add(change.getChangePercent());
            props.getComments().add(prop.getCommitText());
         }
      }

      public void printResults() {
         for (Map.Entry<String, ClusterProperties> typeCluster : clusters.entrySet()) {
            System.out.println(typeCluster.getKey());
            DescriptiveStatistics stat = new DescriptiveStatistics();
            for (Double val : typeCluster.getValue().getChangePercent()) {
               stat.addValue(val);
            }
            System.out.println(stat.getMean());
            for (String comment : typeCluster.getValue().getComments()) {
               System.out.println(" " + comment);
            }
         }
         
      }
   }

   public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
      File folder = new File("results/commons-io/");
      // File changesFile = new File(folder, "changes.json");
      File propertiesFile = new File(folder, "properties.json");

      VersionChangeProperties properties = FolderSearcher.MAPPER.readValue(propertiesFile, VersionChangeProperties.class);

      TypeClusterReader typeClusterReader = new TypeClusterReader();
      properties.executeProcessor(typeClusterReader);
      
      typeClusterReader.printResults();
   }
}
