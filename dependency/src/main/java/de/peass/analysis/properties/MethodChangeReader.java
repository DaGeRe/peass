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

   private final File methodSourceFolder;
   private final ChangedEntity clazz;

   private final String version;

   private final String method, methodOld;

   public MethodChangeReader(final File methodSourceFolder, final File sourceFolder, final File oldSourceFolder, final ChangedEntity clazz, final String version)
         throws FileNotFoundException {
      this.methodSourceFolder = methodSourceFolder;
      this.clazz = clazz;
      this.version = version;

      method = FileComparisonUtil.getMethodSource(sourceFolder, clazz, clazz.getMethod());
      methodOld = FileComparisonUtil.getMethodSource(oldSourceFolder, clazz, clazz.getMethod());
   }

   public void readMethodChangeData() throws IOException {
      final File goalFile = getMethodDiffFile(methodSourceFolder, version, clazz);
      if (!method.equals(methodOld)) {

         final File main = getMethodMainFile(methodSourceFolder, version, clazz);
         final File old = getMethodOldFile(methodSourceFolder, version, clazz);

         FileUtils.writeStringToFile(main, method, Charset.defaultCharset());
         FileUtils.writeStringToFile(old, methodOld, Charset.defaultCharset());
         DiffUtil.generateDiffFile(goalFile, Arrays.asList(new File[] { old, main }), "");
      } else {
         FileUtils.writeStringToFile(goalFile, method, Charset.defaultCharset());
      }
   }

   public Patch<String> getKeywordChanges(final ChangedEntity clazz) throws FileNotFoundException {
      final Patch<String> patch = DiffUtils.diff(Arrays.asList(method.split("\n")), Arrays.asList(methodOld.split("\n")));
      return patch;
   }
   
   public static File getMethodMainFile(final File methodSourceFolder, final String version, final ChangedEntity clazz) {
      return getMethodModifierFile(methodSourceFolder, version, clazz, "main");
   }
   
   public static File getMethodOldFile(final File methodSourceFolder, final String version, final ChangedEntity clazz) {
      return getMethodModifierFile(methodSourceFolder, version, clazz, "old");
   }

   public static File getMethodDiffFile(final File methodSourceFolder, final String version, final ChangedEntity clazz) {
      return getMethodModifierFile(methodSourceFolder, version, clazz, "diff");
   }

   private static File getMethodModifierFile(final File methodSourceFolder, final String version, final ChangedEntity clazz, final String modifier) {
      final File versionFolder = new File(methodSourceFolder, version);
      versionFolder.mkdirs();
      final String clazzFolderName = (clazz.getModule() != null && !clazz.getModule().equals("")) ? clazz.getModule() + ChangedEntity.MODULE_SEPARATOR + clazz.getJavaClazzName()
            : clazz.getJavaClazzName();
      final File clazzFolder = new File(versionFolder, clazzFolderName);
      clazzFolder.mkdirs();
      final String methodString = clazz.getMethod() + "_" + clazz.getParametersPrintable();
      final File methodDiffFile = new File(clazzFolder, methodString + "_" + modifier + ".txt");
      return methodDiffFile;
   }
}
