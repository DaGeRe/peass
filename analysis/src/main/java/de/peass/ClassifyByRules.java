package de.peass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.all.RepoFolders;
import de.peass.analysis.groups.Classification;
import de.peass.analysis.groups.TestcaseClass;
import de.peass.analysis.groups.VersionClass;
import de.peass.analysis.guessing.GuessDecider;
import de.peass.analysis.properties.ChangeProperties;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.VersionChangeProperties;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peran.FolderSearcher;
import difflib.Delta;
import difflib.Patch;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class ClassifyByRules implements Callable<Void> {

   @Option(names = { "-learnproject", "--learnproject" }, description = "Name of the project which is used for learning and should be classified", required = true)
   private String learnproject;

   @Option(names = { "-classifyproject", "--classifyproject" }, description = "Name of the project which should be classified", required = true)
   private String classifyproject;

   public static void main(String[] args) {
      CommandLine commandLine = new CommandLine(new ClassifyByRules());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {

      Set<String> allFeatures = getAllFeatures();
      System.out.println("Features: " + allFeatures.size());
      
      for (String project : new String[] { learnproject, classifyproject }) {
         classifyProject(allFeatures, project);
      }
      return null;
   }

   private void classifyProject(Set<String> allFeatures, String project) throws IOException, JsonParseException, JsonMappingException {
      RepoFolders repos = new RepoFolders();

      File learnFile = new File(repos.getClassificationFolder(), project + ".json");
      File learnPropertyFile = repos.getProjectPropertyFile(project);
      Classification learn = FolderSearcher.MAPPER.readValue(learnFile, Classification.class);
      
      final VersionChangeProperties changes = FolderSearcher.MAPPER.readValue(learnPropertyFile, VersionChangeProperties.class);
      File methodFileFolder = new File(learnPropertyFile.getParentFile(), "methods");
      try (BufferedWriter csvWriter = new BufferedWriter(new FileWriter(new File(project + ".csv")))) {
         for (Entry<String, ChangeProperties> version : changes.getVersions().entrySet()) {
            File versionFolder = new File(methodFileFolder, version.getKey());
            
            for (Entry<String, List<ChangeProperty>> testcase : version.getValue().getProperties().entrySet()) {
               for (ChangeProperty property : testcase.getValue()) {
                  VersionClass versionClass = learn.getVersions().get(version.getKey());
                  ChangedEntity entity = new ChangedEntity(testcase.getKey(), "", property.getMethod());
                  TestcaseClass testcaseClass = versionClass.getTestcases().get(entity);
                  GuessDecider guesser = new GuessDecider(versionFolder);

                  Set<String> types = testcaseClass.getTypes();
                  writeFeatures(allFeatures, csvWriter, property, guesser);

                  writeType(csvWriter, types);
                  csvWriter.write("\n");
                  csvWriter.flush();
               }
            }
         }
      }
   }

   private void writeFeatures(Set<String> allFeatures, BufferedWriter csvWriter, ChangeProperty property, GuessDecider guesser) throws IOException {
      List<String> featuresOfThis = new LinkedList<>();
      List<String> featuresRevised = new LinkedList<>();
      for (String method : property.getAffectedMethods()) {
         Patch<String> diff = guesser.getDiff(method);
         featuresOfThis.addAll(getOriginalFeatures(diff));
         featuresRevised.addAll(getRevisedFeatures(diff));
      }
      for (String feature : allFeatures) {
         int original = count(featuresOfThis, feature);
         int revised = count(featuresRevised, feature);
         csvWriter.write(original + ";" + revised + ";");
      }
   }

   private void writeType(BufferedWriter csvWriter, Set<String> types) throws IOException {
      if (types.contains("ADDITIONAL_PROCESSING")) {
         csvWriter.write("ADDITIONAL_PROCESSING");
      } else if (types.contains("COMPLEX_ALGORITHM")) {
         csvWriter.write("COMPLEX_ALGORITHM");
      } else if (types.contains("CONDITION_EXECUTION")) {
         csvWriter.write("CONDITION_EXECUTION");
      } else if (types.contains("REUSE_BUFFER")) {
         csvWriter.write("REUSE_BUFFER");
      } else if (types.contains("COLLECTION_API")) {
         csvWriter.write("COLLECTION_API");
      } else if (types.contains("EFFICIENT_STRING")) {
         csvWriter.write("EFFICIENT_STRING");
      } else if (types.contains("STREAM")) {
         csvWriter.write("STREAM");
      } else if (types.contains("INT_LONG")) {
         csvWriter.write("INT_LONG");
      } else if (types.contains("REDUCED_PROCESSING")) {
         csvWriter.write("REDUCED_PROCESSING");
      } else {
         csvWriter.write("OTHER");
      }
   }

   private Set<String> getAllFeatures() throws IOException, JsonParseException, JsonMappingException {
      Set<String> allFeatures = new HashSet<>();
      for (String project : new String[] { learnproject, classifyproject }) {
         RepoFolders repos = new RepoFolders();

         File learnFile = new File(repos.getClassificationFolder(), project + ".json");
         File learnPropertyFile = repos.getProjectPropertyFile(project);
         Classification learn = FolderSearcher.MAPPER.readValue(learnFile, Classification.class);
         final VersionChangeProperties changes = FolderSearcher.MAPPER.readValue(learnPropertyFile, VersionChangeProperties.class);
         File methodFileFolder = new File(learnPropertyFile.getParentFile(), "methods");
         
         getAllWords(learn, changes, methodFileFolder, allFeatures);
      }
      return allFeatures;
   }

   private int count(List<String> featuresOfThis, String feature) {
      int original = 0;
      for (String part : featuresOfThis) {
         if (part.equals(feature)) {
            original++;
         }
      }
      return original;
   }

   private void getAllWords(Classification learn, final VersionChangeProperties changes, File methodFileFolder, Set<String> allFeatures) throws IOException {
      for (Entry<String, ChangeProperties> version : changes.getVersions().entrySet()) {
         File versionFolder = new File(methodFileFolder, version.getKey());
         for (Entry<String, List<ChangeProperty>> testcase : version.getValue().getProperties().entrySet()) {
            for (ChangeProperty property : testcase.getValue()) {
               GuessDecider guesser = new GuessDecider(versionFolder);
               for (String method : property.getAffectedMethods()) {
                  Patch<String> diff = guesser.getDiff(method);
                  List<String> features = getOriginalFeatures(diff);
                  List<String> featuresRevised = getRevisedFeatures(diff);
                  allFeatures.addAll(features);
                  allFeatures.addAll(featuresRevised);
                  // indexBuilder.addDocument("doc_" + index++, features.toArray(new String[0]), categoryNames);
               }
            }
         }
      }
   }

   private List<String> getOriginalFeatures(Patch<String> diff) {
      List<String> features = new LinkedList<String>();
      for (Delta<String> delta : diff.getDeltas()) {
         for (String line : delta.getOriginal().getLines()) {
            List<String> splited = new LinkedList<>();
            for (String element : line.split(" ")) {
               splited.add(element);
            }
            features.addAll(splited);
         }
      }
      return features;
   }

   private List<String> getRevisedFeatures(Patch<String> diff) {
      List<String> features = new LinkedList<String>();
      for (Delta<String> delta : diff.getDeltas()) {
         for (String line : delta.getRevised().getLines()) {
            List<String> splited = new LinkedList<>();
            for (String element : line.split(" ")) {
               splited.add(element);
            }
            features.addAll(splited);
         }
      }
      return features;
   }

}
