package de.dagere.peass;

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

import de.dagere.peass.analysis.all.RepoFolders;
import de.dagere.peass.analysis.groups.Classification;
import de.dagere.peass.analysis.groups.TestcaseClass;
import de.dagere.peass.analysis.groups.VersionClass;
import de.dagere.peass.analysis.guessing.GuessDecider;
import de.dagere.peass.analysis.properties.ChangeProperties;
import de.dagere.peass.analysis.properties.ChangeProperty;
import de.dagere.peass.analysis.properties.VersionChangeProperties;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.utils.Constants;
import difflib.Delta;
import difflib.Patch;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class ClassifyByRules implements Callable<Void> {

   @Option(names = { "-learnproject", "--learnproject" }, description = "Name of the project which is used for learning and should be classified", required = true)
   private String learnproject;

   @Option(names = { "-classifyproject", "--classifyproject" }, description = "Name of the project which should be classified", required = true)
   private String classifyproject;

   public static void main(final String[] args) {
      final CommandLine commandLine = new CommandLine(new ClassifyByRules());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {

      final Set<String> allFeatures = getAllFeatures();
      System.out.println("Features: " + allFeatures.size());

      for (final String project : new String[] { learnproject, classifyproject }) {
         classifyProject(allFeatures, project);
      }
      return null;
   }

   private void classifyProject(final Set<String> allFeatures, final String project) throws IOException, JsonParseException, JsonMappingException {
      final RepoFolders repos = new RepoFolders();

      final File learnFile = new File(repos.getClassificationFolder(), project + ".json");
      final File learnPropertyFile = repos.getProjectPropertyFile(project);
      final Classification learn = Constants.OBJECTMAPPER.readValue(learnFile, Classification.class);

      final VersionChangeProperties changes = Constants.OBJECTMAPPER.readValue(learnPropertyFile, VersionChangeProperties.class);
      final File methodFileFolder = new File(learnPropertyFile.getParentFile(), "methods");
      try (BufferedWriter csvWriter = new BufferedWriter(new FileWriter(new File(project + ".csv")))) {
         for (final Entry<String, ChangeProperties> version : changes.getVersions().entrySet()) {
            final File versionFolder = new File(methodFileFolder, version.getKey());

            for (final Entry<String, List<ChangeProperty>> testcase : version.getValue().getProperties().entrySet()) {
               for (final ChangeProperty property : testcase.getValue()) {
                  final VersionClass versionClass = learn.getVersions().get(version.getKey());
                  final ChangedEntity entity = new ChangedEntity(testcase.getKey(), "", property.getMethod());
                  final TestcaseClass testcaseClass = versionClass.getTestcases().get(entity);
                  final GuessDecider guesser = new GuessDecider(versionFolder);

                  final Set<String> types = testcaseClass.getTypes();
                  writeFeatures(allFeatures, csvWriter, property, guesser);

                  writeType(csvWriter, types);
                  csvWriter.write("\n");
                  csvWriter.flush();
               }
            }
         }
      }
   }

   private void writeFeatures(final Set<String> allFeatures, final BufferedWriter csvWriter, final ChangeProperty property, final GuessDecider guesser) throws IOException {
      final List<String> featuresOfThis = new LinkedList<>();
      final List<String> featuresRevised = new LinkedList<>();
      for (final String method : property.getAffectedMethods()) {
         final Patch<String> diff = guesser.getDiff(method);
         featuresOfThis.addAll(getOriginalFeatures(diff));
         featuresRevised.addAll(getRevisedFeatures(diff));
      }
      for (final String feature : allFeatures) {
         final int original = count(featuresOfThis, feature);
         final int revised = count(featuresRevised, feature);
         csvWriter.write(original + ";" + revised + ";");
      }
   }

   private void writeType(final BufferedWriter csvWriter, final Set<String> types) throws IOException {
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
      final Set<String> allFeatures = new HashSet<>();
      for (final String project : new String[] { learnproject, classifyproject }) {
         final RepoFolders repos = new RepoFolders();

         final File learnFile = new File(repos.getClassificationFolder(), project + ".json");
         final File learnPropertyFile = repos.getProjectPropertyFile(project);
         final Classification learn = Constants.OBJECTMAPPER.readValue(learnFile, Classification.class);
         final VersionChangeProperties changes = Constants.OBJECTMAPPER.readValue(learnPropertyFile, VersionChangeProperties.class);
         final File methodFileFolder = new File(learnPropertyFile.getParentFile(), "methods");

         getAllWords(learn, changes, methodFileFolder, allFeatures);
      }
      return allFeatures;
   }

   private int count(final List<String> featuresOfThis, final String feature) {
      int original = 0;
      for (final String part : featuresOfThis) {
         if (part.equals(feature)) {
            original++;
         }
      }
      return original;
   }

   private void getAllWords(final Classification learn, final VersionChangeProperties changes, final File methodFileFolder, final Set<String> allFeatures) throws IOException {
      for (final Entry<String, ChangeProperties> version : changes.getVersions().entrySet()) {
         final File versionFolder = new File(methodFileFolder, version.getKey());
         for (final Entry<String, List<ChangeProperty>> testcase : version.getValue().getProperties().entrySet()) {
            for (final ChangeProperty property : testcase.getValue()) {
               final GuessDecider guesser = new GuessDecider(versionFolder);
               for (final String method : property.getAffectedMethods()) {
                  final Patch<String> diff = guesser.getDiff(method);
                  final List<String> features = getOriginalFeatures(diff);
                  final List<String> featuresRevised = getRevisedFeatures(diff);
                  allFeatures.addAll(features);
                  allFeatures.addAll(featuresRevised);
                  // indexBuilder.addDocument("doc_" + index++, features.toArray(new String[0]), categoryNames);
               }
            }
         }
      }
   }

   private List<String> getOriginalFeatures(final Patch<String> diff) {
      final List<String> features = new LinkedList<String>();
      for (final Delta<String> delta : diff.getDeltas()) {
         for (final String line : delta.getOriginal().getLines()) {
            final List<String> splited = new LinkedList<>();
            for (final String element : line.split(" ")) {
               splited.add(element);
            }
            features.addAll(splited);
         }
      }
      return features;
   }

   private List<String> getRevisedFeatures(final Patch<String> diff) {
      final List<String> features = new LinkedList<String>();
      for (final Delta<String> delta : diff.getDeltas()) {
         for (final String line : delta.getRevised().getLines()) {
            final List<String> splited = new LinkedList<>();
            for (final String element : line.split(" ")) {
               splited.add(element);
            }
            features.addAll(splited);
         }
      }
      return features;
   }

}
