package de.peass.overviewTables;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.reader.VersionKeeper;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;
import de.peran.analysis.helper.all.CleanAll;

/**
 * Creates tab:projektgroessen of PhD thesis.
 * @author reichelt
 *
 */
public class GetProjectSizes {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      
      File dependencyFolder;
      if (System.getenv(Constants.PEASS_REPOS) != null) {
         final String repofolder = System.getenv(Constants.PEASS_REPOS);
         final File folder = new File(repofolder);
         dependencyFolder = new File(folder, "dependencies-final");
      }else {
         dependencyFolder = new File(CleanAll.defaultDependencyFolder);
      }
      
      System.out.println("Projekt & Versionen & Analysierbar & Geändert & Selektiert & Tests\\\\ \\hline");
      for (final String project: CleanAll.allProjects) {
         final File projectFolder = new File("../../projekte/" + project);
         final int commits = GitUtils.getCommits(projectFolder, true).size();
         
         final File executionFile = new File(dependencyFolder, "execute_" + project+".json");
         
         int analyzable = 0;
         
         int tests = 0;
         int executionTests = 0;
         if (executionFile.exists()) {
            final ExecutionData executionData = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
            executionTests = executionData.getVersions().size();
            for (final TestSet test : executionData.getVersions().values()) {
               tests += test.getTests().size();
            }
         }
         
         final File nonRunning = new File(dependencyFolder, "nonRunning_" + project+".json");
         final File nonChanges = new File(dependencyFolder, "nonChanges_" + project+".json");
         
         int changes = 0; 
         if (nonRunning.exists()) {
            final VersionKeeper vk = Constants.OBJECTMAPPER.readValue(nonRunning, VersionKeeper.class);
            analyzable = commits - vk.getNonRunableReasons().size();
            if (nonChanges.exists()) {
               final VersionKeeper vk2 = Constants.OBJECTMAPPER.readValue(nonChanges, VersionKeeper.class);
               changes = analyzable - vk2.getNonRunableReasons().size();
            }
         }
         
         System.out.println(project + " & " + commits + " & " + analyzable +" & " + changes + " & "+ executionTests + " & " + tests + "\\\\");
      }
      
   }
}
