package de.peass.debug;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.utils.RunCommandWriter;
import de.dagere.peass.utils.RunCommandWriterSlurm;
import de.peass.analysis.groups.Classification;
import de.peass.analysis.groups.TestcaseClass;
import de.peass.analysis.groups.VersionClass;
import de.peran.FolderSearcher;

public class FindWrong {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {

      final File executeCommands = new File("execute-wrong.sh");
      final PrintStream goal = new PrintStream(new FileOutputStream(executeCommands));

      final File folder = new File("/home/reichelt/daten3/diss/repos/properties/classification");
      int index = 0;
      for (final File project : folder.listFiles()) {
         
         if (project.getName().endsWith(".json")) {
            final String projectName = project.getName().substring(0, project.getName().indexOf('.'));
            final String url = Constants.defaultUrls.get(projectName);
            RunCommandWriter writer = new RunCommandWriterSlurm(goal, "wrong_rerun", projectName, url);
            if (url != null) {
               final Classification data = FolderSearcher.MAPPER.readValue(project, Classification.class);
               for (final Map.Entry<String, VersionClass> version : data.getVersions().entrySet()) {
                  for (final Map.Entry<ChangedEntity, TestcaseClass> method : version.getValue().getTestcases().entrySet()) {
                     if (method.getValue().getTypes().contains("WRONG") || method.getValue().getTypes().contains("WRONGTEST")) {
//                        System.out.println(version.getKey() + " " + method.getKey());

                        writer.createSingleMethodCommand(index, version.getKey(), method.getKey().toString());
                        index++;
                     }
                  }
               }
            } else {
               System.err.println("Missing url: " + projectName);
            }
         }
      }
   }
}
