package de.peass.debug;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.TestCleaner;
import de.peass.analysis.groups.Classification;
import de.peass.analysis.groups.TestcaseClass;
import de.peass.analysis.groups.VersionClass;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.utils.Constants;
import de.peass.utils.DivideVersions;
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

            if (url != null) {
               final Classification data = FolderSearcher.MAPPER.readValue(project, Classification.class);
               for (final Map.Entry<String, VersionClass> version : data.getVersions().entrySet()) {
                  for (final Map.Entry<ChangedEntity, TestcaseClass> method : version.getValue().getTestcases().entrySet()) {
                     if (method.getValue().getTypes().contains("WRONG") || method.getValue().getTypes().contains("WRONGTEST")) {
//                        System.out.println(version.getKey() + " " + method.getKey());

                        final String simple = method.getKey().getSimpleClazzName() + method.getKey().getMethod();
                        DivideVersions.createSingleSBatch("wrong_rerun", goal, url, index, version.getKey(), method.getKey().toString(), simple);
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
