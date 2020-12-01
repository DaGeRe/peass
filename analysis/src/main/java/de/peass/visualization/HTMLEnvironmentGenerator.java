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
      fileWriter.write("Version: <a href='"
            + "javascript:fallbackCopyTextToClipboard(\"-version " + data.getMeasurementConfig().getVersion() + " -test " + data.getTestcase() + "\")'>"
            + data.getMeasurementConfig().getVersion() + "</a><br>\n");
      fileWriter.write("Test Case: " + data.getTestcase() + "<br>\n"
            + "<a href=\"#\" onclick=\"collapse();\">Collapse</a>");
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
