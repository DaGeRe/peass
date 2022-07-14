package de.dagere.peass.visualization.html;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.properties.ChangedMethodManager;
import de.dagere.peass.ci.ContinuousDependencyReader;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;
import de.dagere.peass.visualization.GraphNode;

public class SourceWriter {
   
   private static final Logger LOG = LogManager.getLogger(SourceWriter.class);
   
   private final GraphNode root;
   private final BufferedWriter fileWriter;
   private final ChangedMethodManager manager;
   private final Map<String, String> nameSourceCurrent = new HashMap<>();
   private final Map<String, String> nameSourceOld = new HashMap<>();
   private final String commit;

   public SourceWriter(final GraphNode root, final BufferedWriter fileWriter, final File methodSourceFolder, final String commit) {
      this.root = root;
      this.fileWriter = fileWriter;
      this.manager = new ChangedMethodManager(methodSourceFolder);
      this.commit = commit;
   }

   public void writeSources() throws IOException {
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

      final File currentSourceFile = manager.getMethodMainFile(commit, methodEntity);
      final File oldSourceFile = manager.getMethodOldFile(commit, methodEntity);
      if (currentSourceFile.exists() && oldSourceFile.exists()) {
         node.setHasSourceChange(true);
         final String sourceCurrent = FileUtils.readFileToString(currentSourceFile, Charset.defaultCharset());
         nameSourceCurrent.put(key, sourceCurrent);
         final String sourceOld = FileUtils.readFileToString(oldSourceFile, Charset.defaultCharset());
         nameSourceOld.put(key, sourceOld);
      } else {
         final File diffSourceFile = manager.getMethodDiffFile(commit, methodEntity);
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