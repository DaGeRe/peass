package de.peass.visualization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.peass.measurement.rca.data.CauseSearchData;

public class HTMLEnvironmentGenerator {

   final BufferedWriter fileWriter;

   public HTMLEnvironmentGenerator(final BufferedWriter fileWriter) {
      this.fileWriter = fileWriter;
   }

   public void writeInfoDivs(final CauseSearchData data) throws IOException {
      fileWriter.write("<script src=\"https://cdn.plot.ly/plotly-latest.min.js\"></script>");
      fileWriter.write("<div style='position:absolute; right: 0px; width: 800px; background-color: #BBBBBB; "
            + "border: 2px solid blue; border-radius: 1em 1em 1em 1em; padding: 1em;'>\n");
      fileWriter.write("Version: " + data.getMeasurementConfig().getVersion() + "<br>\n");
      fileWriter.write("Test Case: " + data.getTestcase() + "<br>\n"
            + "<a href=\"#\" onclick=\"collapse();\">Collapse</a>");
      fileWriter.write("</div>\n");

      fileWriter.write("<div style='position:absolute; left: 0px; width: 800px; height: 100px; background-color: #BBBBBB; "
            + "border: 2px solid blue; border-radius: 1em 1em 1em 1em; padding: 1em;' id='infos'>\n");
      fileWriter.write("Statistische Informationen\n");
      fileWriter.write("</div>\n");

      fileWriter.write("<div style='position:absolute; bottom: 0px; "
            + "width: 600px; height: 300px; background-color: #BBBBBB; "
            + "border: 2px solid blue; border-radius: 1em 1em 1em 1em; padding: 1em;' "
            + "id='histogramm'>\n");
      fileWriter.write("Plot\n");
      fileWriter.write("</div>\n");

      fileWriter.write("<div style='position:absolute; bottom: 0px; right:0px; overflow: scroll; "
            + "width: 1000px; height: 300px; background-color: #BBBBBB; "
            + "border: 2px solid blue; border-radius: 1em 1em 1em 1em; padding: 1em;' "
            + "id='quelltext'>\n");
      fileWriter.write("Quelltext\n");
      fileWriter.write("</div>\n");
   }

   void writeHTML(final String name) throws IOException {
      final InputStream htmlStream = VisualizeRCA.class.getClassLoader().getResourceAsStream(name);
      try (final BufferedReader reader = new BufferedReader(new InputStreamReader(htmlStream))) {
         String line;
         while ((line = reader.readLine()) != null) {
            fileWriter.write(line + "\n");
         }
      }
   }
}
