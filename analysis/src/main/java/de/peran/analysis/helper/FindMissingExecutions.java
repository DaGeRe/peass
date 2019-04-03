package de.peran.analysis.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import de.peass.utils.DivideVersions;

public class FindMissingExecutions {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException, JAXBException {
      final String project = "commons-io";
      
      findMissing(project);
   }

   static void findMissing(final String project) throws IOException, JsonParseException, JsonMappingException, JAXBException {
      
      
      final File dependencyfile = new File("/home/reichelt/daten3/diss/repos/dependencies-final/deps_"+project+".json");
      final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyfile, Dependencies.class);
      VersionComparator.setDependencies(dependencies);
      
      final File executefile = new File("/home/reichelt/daten3/diss/repos/dependencies-final/execute_"+project+".json");
      final ExecutionData tests = Constants.OBJECTMAPPER.readValue(executefile, ExecutionData.class);

      final File folder = new File("/home/reichelt/daten3/diss/repos/measurementdata/cleanData/"+project);

      int count2 = 0;
      for (final Entry<String, TestSet> entry : tests.getVersions().entrySet()) {
         System.out.println(entry.getKey());
         System.out.println(entry.getValue());
         count2 += entry.getValue().getTests().size();
      }
      
      for (final File measurementFile : folder.listFiles()) {
         if (measurementFile.getName().endsWith(".xml")) {
            System.out.println("File:" + measurementFile);
            final Kopemedata data = new XMLDataLoader(measurementFile).getFullData();
            for (final TestcaseType testcase : data.getTestcases().getTestcase()) {
               final String testmethod = testcase.getName();
               for (final Chunk c : testcase.getDatacollector().get(0).getChunk()) {
                  final int size = c.getResult().size();
                  final Result r = c.getResult().get(size-1);
                  final String version = r.getVersion().getGitversion();
                  System.out.println("Version: " + version);
                  final TestSet testSet = tests.getVersions().get(version);
                  if (testSet != null) {
                     System.out.println(testSet.classCount());
                     final ChangedEntity ce = new ChangedEntity(data.getTestcases().getClazz(), "");
                     testSet.removeTest(ce, testmethod);
                     System.out.println(testSet.classCount());
                  }
               }
            }
         }
      }
      
      System.out.println();
      System.out.println("Missing Tests");

      int count = 0;
       for (final Entry<String, TestSet> entry : tests.getVersions().entrySet()) {
          System.out.println(entry.getKey());
          System.out.println(entry.getValue());
          count += entry.getValue().getTests().size();
       }
       System.out.println("Sum: " + count + " Before: " + count2);
       
       final PrintStream file2 = new PrintStream(new FileOutputStream(new File("slurm-"+project+".sh")));
       DivideVersions.generateExecuteCommands(dependencies, tests, "missing", new File("execute-"+project+".sh"), file2);
       file2.flush();
       file2.close();
   }
}
