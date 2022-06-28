package de.dagere.peass.analysis.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.changesreading.FileComparisonUtil;
import de.dagere.peass.dependency.traces.diff.DiffUtilJava;
import difflib.DiffUtils;
import difflib.Patch;

public class MethodChangeReader {

   private final ChangedMethodManager manager;
   private final ChangedEntity clazz;

   private final String commit;

   private final String method, methodOld;

   public MethodChangeReader(final File methodSourceFolder, final File sourceFolder, final File oldSourceFolder, final ChangedEntity clazz, 
         final String version, final ExecutionConfig config)
         throws FileNotFoundException {
      this.manager = new ChangedMethodManager(methodSourceFolder);
      this.clazz = clazz;
      this.commit = version;
      
      method = FileComparisonUtil.getMethodSource(sourceFolder, clazz, clazz.getMethod(), config);
      methodOld = FileComparisonUtil.getMethodSource(oldSourceFolder, clazz, clazz.getMethod(), config);
   }

   public void readMethodChangeData() throws IOException {
      final File goalFile = manager.getMethodDiffFile(commit, clazz);
      if (!method.equals(methodOld)) {

         final File main = manager.getMethodMainFile(commit, clazz);
         final File old = manager.getMethodOldFile(commit, clazz);

         FileUtils.writeStringToFile(main, method, Charset.defaultCharset());
         FileUtils.writeStringToFile(old, methodOld, Charset.defaultCharset());
         DiffUtilJava.generateDiffFile(goalFile, Arrays.asList(new File[] { old, main }), "");
      } else {
         FileUtils.writeStringToFile(goalFile, method, Charset.defaultCharset());
      }
   }

   public Patch<String> getKeywordChanges(final ChangedEntity clazz) throws FileNotFoundException {
      final Patch<String> patch = DiffUtils.diff(Arrays.asList(method.split("\n")), Arrays.asList(methodOld.split("\n")));
      return patch;
   }
   
}
