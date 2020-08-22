package de.peran.analysis.helper.all;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.utils.Constants;
import de.peran.FolderSearcher;

public class GetIntendedCommits {
   public static void main(String[] args) throws IOException, JAXBException {
      File dependencyFolder = new File(args.length > 0 ? args[0] : "/home/reichelt/daten/diss/ergebnisse/views/v15_mitEnums/data");
      Map<String, List<String>> projectCommits = getCommits();

      Map<String, Integer> beforeMap = new LinkedHashMap<>();
      Map<String, Integer> selectedMap = new LinkedHashMap<>();
      Map<String, Integer> unselectedMap = new LinkedHashMap<>();
      Map<String, Integer> allMap = new LinkedHashMap<>();
      for (Map.Entry<String, List<String>> entry : projectCommits.entrySet()) {
         System.out.println(entry.getKey() + " - " + entry.getValue());
         File viewFile = new File(dependencyFolder, "views_" + entry.getKey() + "/execute-" + entry.getKey() + ".json");
         ExecutionData tests = FolderSearcher.MAPPER.readValue(viewFile, ExecutionData.class);
         File dependencyFile = new File(dependencyFolder, "deps_" + entry.getKey() + ".xml");
         final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);
         int selected = 0, before = 0, unselected = 0;
         for (String version : entry.getValue()) {
            if (tests.getVersions().keySet().contains(version)) {
               selected++;
               System.out.println(version);
            } else if (VersionComparator.isBefore(version, dependencies.getInitialversion().getVersion())) {
               before++;
               // System.out.println("Before: " + version + " " + dependencies.getInitialversion().getVersion());
            } else {
               unselected++;
            }
         }
         beforeMap.put(entry.getKey(), before);
         selectedMap.put(entry.getKey(), selected);
         unselectedMap.put(entry.getKey(), unselected);
         allMap.put(entry.getKey(), entry.getValue().size());
         System.out.println("Selected: " + selected + " Unselected: " + unselected + " Before: " + before + " All: " + entry.getValue().size());
      }

      System.out.println("Project & " + getLine(beforeMap.keySet()) + " \\hline");
      System.out.println("Selected & " + getLine(selectedMap.values()));
      System.out.println("Workload & \\\\");
      System.out.println("Change & \\\\ \\hline");
      System.out.println("Unselected & " + getLine(unselectedMap.values()));
      System.out.println("Before & " + getLine(beforeMap.values()) + " \\hline");
      System.out.println("All & " + getLine(allMap.values()));
   }

   private static String getLine(Collection<?> beforeMap) {
      String line = "";
      for (Object val : beforeMap) {
         if (val instanceof String) {
            line += ((String) val).replace("commons-", "") + " & ";
         } else {
            line += val + " & ";
         }
      }
      line = line.substring(0, line.length() - 2) + "\\\\";
      return line;
   }

   private static Map<String, List<String>> getCommits() throws FileNotFoundException, IOException {
      Map<String, List<String>> projectCommits = new TreeMap<>();
      File file = new File("../../datamanagement/rerun/getCommits/performancecommits.txt");
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
         String line;
         String currentProject = null;
         while ((line = reader.readLine()) != null) {
            String[] splitted = line.split(" ");
            if (!splitted[0].equals("Overall:")) {
               if (splitted.length == 2 && Arrays.asList(CleanAll.allProjects).contains(splitted[0])) {
                  currentProject = splitted[0];
                  projectCommits.put(currentProject, new LinkedList<>());
               } else {
                  String commit = splitted[0];
                  projectCommits.get(currentProject).add(commit);
               }
            }
         }
      }
      return projectCommits;
   }
}
