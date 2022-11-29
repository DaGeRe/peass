package de.dagere.peass.visualization.html;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.properties.ChangedMethodManager;
import de.dagere.peass.visualization.GraphNode;

public class SourceWriter {

   private static final Logger LOG = LogManager.getLogger(SourceWriter.class);

   private final BufferedWriter fileWriter;
   private final ChangedMethodManager manager;
   private final String mainCommit, analyzedCommit;

   public SourceWriter(final BufferedWriter fileWriter, final File methodSourceFolder, final String mainCommit, String analyzedCommit) {
      this.fileWriter = fileWriter;
      this.manager = new ChangedMethodManager(methodSourceFolder);
      this.mainCommit = mainCommit;
      this.analyzedCommit = analyzedCommit;
   }

   public void writeSources(final GraphNode root) throws IOException {

      SingleTreeSourceReader reader;
      if (root.getOtherKiekerPattern() != null) {
         reader = new SourceReader(manager, mainCommit);
         ((SourceReader) reader).readSources(root);
      } else {
         reader = new SingleTreeSourceReader(manager, mainCommit, analyzedCommit);
         reader.readSources(root);
      }

      fileWriter.write("var source = {");
      fileWriter.write("\"current\":\n{\n ");
      for (final Map.Entry<String, String> sources : reader.getNameSourceCurrent().entrySet()) {
         fileWriter.write("\"" + sources.getKey() + "\":\n `" + sources.getValue() + "`,");
      }
      fileWriter.write("},\n");

      if (reader instanceof SourceReader) {
         fileWriter.write("\"old\":\n{\n ");
         SourceReader sourceReader = (SourceReader) reader;
         for (final Map.Entry<String, String> sources : sourceReader.getNameSourceOld().entrySet()) {
            fileWriter.write("\"" + sources.getKey() + "\":\n `" + sources.getValue() + "`,");
         }
         fileWriter.write("},\n");
      }

      fileWriter.write("};\n");
   }

}