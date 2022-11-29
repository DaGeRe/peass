package de.dagere.peass.visualization.html;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.utils.Log;

import de.dagere.peass.analysis.properties.ChangedMethodManager;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.measurement.rca.data.BasicNode;
import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;

public class SingleTreeSourceReader {
   
   private static final Logger LOG = LogManager.getLogger(SingleTreeSourceReader.class);

   protected final ChangedMethodManager manager;
   protected final String mainCommit, analyzedCommit;

   protected final Map<String, String> nameSourceCurrent = new HashMap<>();

   public SingleTreeSourceReader(ChangedMethodManager manager, String mainCommit, String analyzedCommit) {
      this.manager = manager;
      this.mainCommit = mainCommit;
      this.analyzedCommit = analyzedCommit;
   }

   public void readSources(final BasicNode parent) throws IOException {
      getNodeSource(parent);
      for (final BasicNode node : parent.getChildren()) {
         readSources(node);
      }
   }

   private void getNodeSource(final BasicNode node) throws IOException {
      final String currentPattern = node.getKiekerPattern();

      ChangedEntity methodEntity = getChangedEntity(node, currentPattern);
      final String key = KiekerPatternConverter.getKey(currentPattern);

      // TODO: We need to decide here which commit is currently analyzed and whether we want predecessor or current file

      final File currentSourceFile = manager.getMethodMainFile(mainCommit, methodEntity);
      final File oldSourceFile = manager.getMethodOldFile(mainCommit, methodEntity);

      if (currentSourceFile.exists() && mainCommit.equals(analyzedCommit)) {
         final String sourceCurrent = FileUtils.readFileToString(currentSourceFile, Charset.defaultCharset());
         nameSourceCurrent.put(key, sourceCurrent);
      } else if (oldSourceFile.exists() && !mainCommit.equals(analyzedCommit)) {
         final String sourceOld = FileUtils.readFileToString(oldSourceFile, Charset.defaultCharset());
         nameSourceCurrent.put(key, sourceOld);
      } else {
         File equalFile = manager.getMethodDiffFile(mainCommit, methodEntity);
         if (equalFile.exists()) {
            final String sourceEqual = FileUtils.readFileToString(equalFile, Charset.defaultCharset());
            nameSourceCurrent.put(key, sourceEqual);
         } else {
            LOG.error("No method file for " + methodEntity + " existed");
         }
         
      }
   }

   protected ChangedEntity getChangedEntity(final BasicNode node, final String currentPattern) {
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

   public Map<String, String> getNameSourceCurrent() {
      return nameSourceCurrent;
   }
}
