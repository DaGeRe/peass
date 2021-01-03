package de.peass.analysis.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.FileComparisonUtil;
import de.peass.dependency.traces.DiffUtil;
import difflib.DiffUtils;
import difflib.Patch;

public class MethodChangeReader {

   private final File outFolder;
   private final ChangedEntity clazz;

   private final String version;

   private final String method, methodOld;

   public MethodChangeReader(final File outFolder, final File sourceFolder, final File oldSourceFolder, final ChangedEntity clazz, final String version)
         throws FileNotFoundException {
      this.outFolder = outFolder;
      this.clazz = clazz;
      this.version = version;

      method = FileComparisonUtil.getMethod(sourceFolder, clazz, clazz.getMethod());
      methodOld = FileComparisonUtil.getMethod(oldSourceFolder, clazz, clazz.getMethod());
   }

   public void readMethodChangeData() throws IOException {
      final File clazzFolder = getClazzFolder();
      final String methodString = clazz.getMethod() + "_" + clazz.getParametersPrintable();
      final File goalFile = new File(clazzFolder, methodString + "_diff.txt");
      if (!method.equals(methodOld)) {

         final File main = new File(clazzFolder, methodString + "_main.txt");
         final File old = new File(clazzFolder, methodString + "_old.txt");

         FileUtils.writeStringToFile(main, method, Charset.defaultCharset());
         FileUtils.writeStringToFile(old, methodOld, Charset.defaultCharset());
         DiffUtil.generateDiffFile(goalFile, Arrays.asList(new File[] { old, main }), "");
      } else {
         FileUtils.writeStringToFile(goalFile, method, Charset.defaultCharset());
      }
   }

   private File getClazzFolder() {
      final File versionFolder = new File(outFolder, version);
      versionFolder.mkdirs();
      final String clazzFolderName = clazz.getModule() != null ? 
            clazz.getModule() + ChangedEntity.MODULE_SEPARATOR + clazz.getJavaClazzName()
            : clazz.getJavaClazzName();
      final File clazzFolder = new File(versionFolder, clazzFolderName);
      clazzFolder.mkdirs();
      return clazzFolder;
   }

   public Patch<String> getKeywordChanges(final ChangedEntity clazz) throws FileNotFoundException {
      final Patch<String> patch = DiffUtils.diff(Arrays.asList(method.split("\n")), Arrays.asList(methodOld.split("\n")));
      return patch;
   }
}
