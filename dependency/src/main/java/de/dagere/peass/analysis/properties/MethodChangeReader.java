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

   private final File methodSourceFolder;
   private final ChangedEntity clazz;

   private final String version;

   private final String method, methodOld;

   public MethodChangeReader(final File methodSourceFolder, final File sourceFolder, final File oldSourceFolder, final ChangedEntity clazz, 
         final String version, final ExecutionConfig config)
         throws FileNotFoundException {
      this.methodSourceFolder = methodSourceFolder;
      this.clazz = clazz;
      this.version = version;
      
      method = FileComparisonUtil.getMethodSource(sourceFolder, clazz, clazz.getMethod(), config);
      methodOld = FileComparisonUtil.getMethodSource(oldSourceFolder, clazz, clazz.getMethod(), config);
   }

   public void readMethodChangeData() throws IOException {
      final File goalFile = getMethodDiffFile(methodSourceFolder, version, clazz);
      if (!method.equals(methodOld)) {

         final File main = getMethodMainFile(methodSourceFolder, version, clazz);
         final File old = getMethodOldFile(methodSourceFolder, version, clazz);

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

   public static File getMethodMainFile(final File methodSourceFolder, final String version, final ChangedEntity methodEntity) {
      return getMethodModifierFile(methodSourceFolder, version, methodEntity, "main");
   }

   public static File getMethodOldFile(final File methodSourceFolder, final String version, final ChangedEntity methodEntity) {
      return getMethodModifierFile(methodSourceFolder, version, methodEntity, "old");
   }

   public static File getMethodDiffFile(final File methodSourceFolder, final String version, final ChangedEntity methodEntity) {
      return getMethodModifierFile(methodSourceFolder, version, methodEntity, "diff");
   }

   private static File getMethodModifierFile(final File methodSourceFolder, final String version, final ChangedEntity methodEntity, final String modifier) {
      final File versionFolder = new File(methodSourceFolder, version);
      versionFolder.mkdirs();
      final String clazzFolderName = (methodEntity.getModule() != null && !methodEntity.getModule().equals(""))
            ? methodEntity.getModule() + ChangedEntity.MODULE_SEPARATOR + methodEntity.getJavaClazzName()
            : methodEntity.getJavaClazzName();
      final File clazzFolder = new File(versionFolder, clazzFolderName);
      clazzFolder.mkdirs();
      String methodFilename = methodEntity.getMethod()
            .replace("<", "(")
            .replace(">", ")");
      final String methodString = methodFilename + "_" + methodEntity.getParametersPrintable();
      String filename = methodString + "_" + modifier + ".txt";
      if (filename.length() > 255) {
         File mappingFile = new File(clazzFolder, "mapping.txt");
         File resultFile = readInMappingFile(clazzFolder, methodString, mappingFile);
         if (resultFile != null) {
            return resultFile;
         }

         File methodFileCandidate = searchUnusedFilename(modifier, clazzFolder, methodFilename);
         updateMappingFile(methodString, mappingFile, methodFileCandidate);

         return methodFileCandidate;
      } else {
         final File methodDiffFile = new File(clazzFolder, filename);
         return methodDiffFile;
      }
   }

   private static File searchUnusedFilename(final String modifier, final File clazzFolder, final String methodFilename) {
      int candidateIndex = 0;
      File methodFileCandidate = new File(clazzFolder, methodFilename + "_" + candidateIndex + "_" + modifier + ".txt");
      while (methodFileCandidate.exists()) {
         candidateIndex++;
         methodFileCandidate = new File(clazzFolder, methodFilename + "_" + candidateIndex + "_" + modifier + ".txt");
      }
      return methodFileCandidate;
   }

   private static void updateMappingFile(final String methodString, final File mappingFile, final File methodFileCandidate) {
      try {
         String finalName = methodString + ";" + methodFileCandidate.getName() + "\n";
         Files.write(mappingFile.toPath(), finalName.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private static File readInMappingFile(final File clazzFolder, final String methodString, final File mappingFile) {
      File resultFile = null;
      if (mappingFile.exists()) {
         try {
            String mappingString = FileUtils.readFileToString(mappingFile, StandardCharsets.UTF_8);
            for (String line : mappingString.split("\n")) {
               String[] split = line.split(";");
               if (split[0].equals(methodString)) {
                  resultFile = new File(clazzFolder, split[1]);
                  break;
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return resultFile;
   }
}
