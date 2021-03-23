package de.peass.visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.analysis.properties.MethodChangeReader;
import de.peass.ci.ContinuousDependencyReader;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.kieker.KiekerPatternConverter;

class SourceWriter {
   
   private static final Logger LOG = LogManager.getLogger(ContinuousDependencyReader.class);
   
   private final GraphNode root;
   private final BufferedWriter fileWriter;
   private File methodSourceFolder;
   private final Map<String, String> nameSourceCurrent = new HashMap<>();
   private final Map<String, String> nameSourceOld = new HashMap<>();
   private final String version;

   public SourceWriter(final GraphNode root, final BufferedWriter fileWriter, final File methodSourceFolder, final String version) {
      this.root = root;
      this.fileWriter = fileWriter;
      this.methodSourceFolder = methodSourceFolder;
      this.version = version;
   }

   void writeSources() throws IOException {
      readSources(root);
      fileWriter.write("var source = {");
      fileWriter.write("\"current\":\n{\n ");
      for (final Map.Entry<String, String> sources : nameSourceCurrent.entrySet()) {
         fileWriter.write("\"" + sources.getKey() + "\":\n `" + sources.getValue() + "`,");
      }
      fileWriter.write("},\n");
      fileWriter.write("\"old\":\n{\n ");
      for (final Map.Entry<String, String> sources : nameSourceOld.entrySet()) {
         fileWriter.write("\"" + sources.getKey() + "\":\n `" + sources.getValue() + "`,");
      }
      fileWriter.write("},\n");
      fileWriter.write("};\n");
   }

   private void readSources(final GraphNode parent) throws IOException {
      getNodeSource(parent);
      for (final GraphNode node : parent.getChildren()) {
         readSources(node);
      }
   }

   private void getNodeSource(final GraphNode node) throws IOException {
      final String currentPattern = node.getKiekerPattern();

      if (!currentPattern.equals(CauseSearchData.ADDED)) {
         readMethod(node, currentPattern);
      }
      if (!currentPattern.equals(node.getOtherKiekerPattern()) && !node.getOtherKiekerPattern().equals(CauseSearchData.ADDED)) {
         readMethod(node, node.getOtherKiekerPattern());
      }
   }

   private void readMethod(final GraphNode node, final String currentPattern) throws IOException {
      ChangedEntity methodEntity = getChangedEntity(node, currentPattern);

      final String key = KiekerPatternConverter.getKey(currentPattern);

      final File currentSourceFile = MethodChangeReader.getMethodMainFile(methodSourceFolder, version, methodEntity);
      final File oldSourceFile = MethodChangeReader.getMethodOldFile(methodSourceFolder, version, methodEntity);
      if (currentSourceFile.exists() && oldSourceFile.exists()) {
         node.setHasSourceChange(true);
         final String sourceCurrent = FileUtils.readFileToString(currentSourceFile, Charset.defaultCharset());
         nameSourceCurrent.put(key, sourceCurrent);
         final String sourceOld = FileUtils.readFileToString(oldSourceFile, Charset.defaultCharset());
         nameSourceOld.put(key, sourceOld);
      } else {
         final File diffSourceFile = MethodChangeReader.getMethodDiffFile(methodSourceFolder, version, methodEntity);
         if (diffSourceFile.exists()) {
            final String source = FileUtils.readFileToString(diffSourceFile, Charset.defaultCharset());
            nameSourceCurrent.put(key, source);
            nameSourceOld.put(key, source);
         } else {
            LOG.warn("Did not find file: {}", diffSourceFile);
         }
      }
   }

   private ChangedEntity getChangedEntity(final GraphNode node, final String currentPattern) {
      int openingParenthesis = currentPattern.lastIndexOf('(');
      final String call = currentPattern.substring(currentPattern.lastIndexOf(' ') + 1, openingParenthesis);
      final int dotIndex = call.lastIndexOf(".");
      String method = call.substring(dotIndex + 1);
      String clazz = call.substring(0, dotIndex);
      ChangedEntity methodEntity = new ChangedEntity(clazz, node.getModule(), method);
      final String parameterString = currentPattern.substring(openingParenthesis + 1, currentPattern.length() - 1);
      methodEntity.createParameters(parameterString);
      return methodEntity;
   }

}