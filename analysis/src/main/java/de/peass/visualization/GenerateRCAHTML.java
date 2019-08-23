package de.peass.visualization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peass.measurement.searchcause.data.CauseSearchData;
import de.peass.measurement.searchcause.data.MeasuredNode;

public class GenerateRCAHTML {
   
   private static final ObjectMapper MAPPER = new ObjectMapper();
   
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
      final File source = new File(args[0]);
      if (source.isDirectory()) {
         for (final File file : source.listFiles()) {
            createVisualization(file);
         }
      } else {
         createVisualization(source);
      }
      
      
      
   }

   private static void createVisualization(final File source) throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, FileNotFoundException {
      final CauseSearchData data = MAPPER.readValue(source, CauseSearchData.class);

      final MeasuredNode parent = data.getNodes();
      final String longestPrefix = getLongestPrefix(parent);
      System.out.println(longestPrefix);
      setPrefix(parent, longestPrefix);

      final MeasuredNode measured = data.getNodes();
      final Node root = new Node();
      root.setName(measured.getCall());

      processNode(measured, root);

      writeHTML(MAPPER, root, data);
   }

   private static void setPrefix(final MeasuredNode parent, final String longestPrefix) {
      parent.setCall(parent.getCall().substring(longestPrefix.length()));
      for (final MeasuredNode node : parent.getChilds()) {
         setPrefix(node, longestPrefix);
      }
   }

   private static String getLongestPrefix(final MeasuredNode parent) {
      String longestPrefix = parent.getCall();
      for (final MeasuredNode node : parent.getChilds()) {
         longestPrefix = StringUtils.getCommonPrefix(longestPrefix, getLongestPrefix(node));
      }
      return longestPrefix;
   }

   private static void writeHTML(final ObjectMapper MAPPER, final Node root, final CauseSearchData data) throws IOException, JsonProcessingException, FileNotFoundException {
      final File output = new File(data.getTestcase() + ".html");
      final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(output));

      fileWriter.write("<!DOCTYPE html>\n" +
            "<script>\n" +
            "var treeData = [\n");
      fileWriter.write(MAPPER.writeValueAsString(root));
      fileWriter.write("];\n</script>");
      final InputStream htmlStream = GenerateRCAHTML.class.getClassLoader().getResourceAsStream("visualization/RestOfHTML.html");
      final BufferedReader reader = new BufferedReader(new InputStreamReader(htmlStream));

      String line;
      while ((line = reader.readLine()) != null) {
         fileWriter.write(line + "\n");
      }
      fileWriter.flush();
      fileWriter.close();
   }

   private static void processNode(final MeasuredNode measuredParent, final Node parent) {
      for (final MeasuredNode measuredChild : measuredParent.getChilds()) {
         final Node newChild = new Node();
         newChild.setName(measuredChild.getCall());
         newChild.setParent(measuredParent.getCall());
         if (measuredChild.getStatistic().isChange()) {
            if (measuredChild.getStatistic().getTvalue() > 0) {
               newChild.setColor("#FF0000");
            } else {
               newChild.setColor("#00FF00");
            }
         } else {
            newChild.setColor("#FFFFFF");
         }
         parent.getChildren().add(newChild);
         processNode(measuredChild, newChild);
      }
   }
}
