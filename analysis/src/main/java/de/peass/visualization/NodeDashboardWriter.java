package de.peass.visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.peass.measurement.rca.data.CauseSearchData;

public class NodeDashboardWriter {
   private final File destination;
   private final CauseSearchData data;

   public NodeDashboardWriter(File destination, CauseSearchData data) {
      this.destination = destination;
      this.data = data;
   }

   public void write(String jsName) {
      try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(destination)) ){
         new HTMLEnvironmentGenerator(fileWriter).writeHTML("visualization/nodeDashboard.html");
         fileWriter.write("<script src='"+jsName+"'></script>\n");
         fileWriter.write("<script src='peass-dashboard-start.js'></script>\n");
         fileWriter.write("</body></html>");
      } catch (IOException e) {
         e.printStackTrace();
      }
      
   }

}
