package de.peran.analysis.helper;

import java.io.File;

/**
 * @deprecated Use RepoFolders instead!
 * @author reichelt
 *
 */
@Deprecated
public class AnalysisUtil {
   
   static File projectResultFolder;
   
   public static void setProjectName(File baseFolder, String projectName) {
      projectResultFolder = new File(baseFolder, projectName);
      if (!projectResultFolder.exists()) {
         projectResultFolder.mkdirs();
      }
   }
   
   public static void setProjectName(String projectName) {
      projectResultFolder = new File("results" + File.separator + projectName);
      if (!projectResultFolder.exists()) {
         projectResultFolder.mkdirs();
      }
   }
   
   public static File getProjectResultFolder() {
      return projectResultFolder;
   }
}
