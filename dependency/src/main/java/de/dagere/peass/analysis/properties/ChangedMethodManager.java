package de.dagere.peass.analysis.properties;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;

import de.dagere.nodeDiffGenerator.data.MethodCall;

public class ChangedMethodManager {

   private static final String DIFF_MODIFIER = "diff";
   private static final String OLD_MODIFIER = "old";
   private static final String MAIN_MODIFIER = "main";

   private final File methodSourceFolder;

   public ChangedMethodManager(File methodSourceFolder) {
      this.methodSourceFolder = methodSourceFolder;
   }

   public File getMethodMainFile(final String commit, final MethodCall methodEntity) {
      return getMethodModifierFile(commit, methodEntity, MAIN_MODIFIER);
   }

   public File getMethodOldFile(final String commit, final MethodCall methodEntity) {
      return getMethodModifierFile(commit, methodEntity, OLD_MODIFIER);
   }

   public File getMethodDiffFile(final String commit, final MethodCall methodEntity) {
      return getMethodModifierFile(commit, methodEntity, DIFF_MODIFIER);
   }

   private File getMethodModifierFile(final String commit, final MethodCall methodEntity, final String modifier) {
      final File commitFolder = new File(methodSourceFolder, commit);
      commitFolder.mkdirs();
      final String clazzFolderName = (methodEntity.getModule() != null && !methodEntity.getModule().equals(""))
            ? methodEntity.getModule() + MethodCall.MODULE_SEPARATOR + methodEntity.getJavaClazzName()
            : methodEntity.getJavaClazzName();
      final File clazzFolder = new File(commitFolder, clazzFolderName);
      clazzFolder.mkdirs();
      String methodFilename = methodEntity.getMethod()
            .replace("<", "(")
            .replace(">", ")");
      final String methodString = methodFilename + "_" + methodEntity.getParametersPrintable();
      String filename = methodString + "_" + modifier + ".txt";

      if (filename.length() > 128) {
         File mappingFile = new File(clazzFolder, "mapping.txt");
         File resultFile = readInMappingFile(clazzFolder, methodString, mappingFile, modifier);
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

   private static File readInMappingFile(final File clazzFolder, final String methodString, final File mappingFile, String modifier) {
      File resultFile = null;
      if (mappingFile.exists()) {
         try {
            String mappingString = FileUtils.readFileToString(mappingFile, StandardCharsets.UTF_8);
            for (String line : mappingString.split("\n")) {
               String[] split = line.split(";");
               if (split[0].equals(methodString)) {
                  String diffFileName = split[1];
                  if (!DIFF_MODIFIER.equals(modifier)) {
                     String pureFilename = diffFileName.substring(0, diffFileName.lastIndexOf('_') + 1);
                     String changedFilename = pureFilename + modifier + ".txt";
                     resultFile = new File(clazzFolder, changedFilename);
                  } else {
                     resultFile = new File(clazzFolder, diffFileName);
                  }
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
