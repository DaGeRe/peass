package de.dagere.peass.analysis.properties;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;

public class ChangedMethodManager {

   private final File methodSourceFolder;

   public ChangedMethodManager(File methodSourceFolder) {
      this.methodSourceFolder = methodSourceFolder;
   }

   public File getMethodMainFile(final String commit, final ChangedEntity methodEntity) {
      return getMethodModifierFile(commit, methodEntity, "main");
   }

   public File getMethodOldFile(final String commit, final ChangedEntity methodEntity) {
      return getMethodModifierFile(commit, methodEntity, "old");
   }

   public File getMethodDiffFile(final String commit, final ChangedEntity methodEntity) {
      return getMethodModifierFile(commit, methodEntity, "diff");
   }

   private File getMethodModifierFile(final String commit, final ChangedEntity methodEntity, final String modifier) {
      final File commitFolder = new File(methodSourceFolder, commit);
      commitFolder.mkdirs();
      final String clazzFolderName = (methodEntity.getModule() != null && !methodEntity.getModule().equals(""))
            ? methodEntity.getModule() + ChangedEntity.MODULE_SEPARATOR + methodEntity.getJavaClazzName()
            : methodEntity.getJavaClazzName();
      final File clazzFolder = new File(commitFolder, clazzFolderName);
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
