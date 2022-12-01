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
   private final String mainCommit;

   public SourceWriter(final BufferedWriter fileWriter, final File methodSourceFolder, final String mainCommit) {
      this.fileWriter = fileWriter;
      this.manager = new ChangedMethodManager(methodSourceFolder);
      this.mainCommit = mainCommit;
   }

   public void writeSingleTreeSources(final GraphNode root, String analyzedCommit) throws IOException {

      SingleTreeSourceReader reader = new SingleTreeSourceReader(manager, mainCommit, analyzedCommit);
      reader.readSources(root);

      writeCurrentSource(reader);

      fileWriter.write("};\n");
   }

   public void writeSources(final GraphNode root) throws IOException {
      SourceReader reader = new SourceReader(manager, mainCommit);
      reader.readSources(root);

      writeCurrentSource(reader);

      fileWriter.write("\"old\":\n{\n ");
      SourceReader sourceReader = (SourceReader) reader;
      for (final Map.Entry<String, String> sources : sourceReader.getNameSourceOld().entrySet()) {
         String printableJS = sources.getValue().replace("${", "\\${");
         fileWriter.write("\"" + sources.getKey() + "\":\n `" + printableJS + "`,");
      }
      fileWriter.write("},\n");

      fileWriter.write("};\n");
   }
   
   private void writeCurrentSource(SingleTreeSourceReader reader) throws IOException {
      fileWriter.write("var source = {");
      fileWriter.write("\"current\":\n{\n ");
      for (final Map.Entry<String, String> sources : reader.getNameSourceCurrent().entrySet()) {
         String printableJS = sources.getValue().replace("${", "\\${");
         fileWriter.write("\"" + sources.getKey() + "\":\n `" + printableJS + "`,");
      }
      fileWriter.write("},\n");
   }

}