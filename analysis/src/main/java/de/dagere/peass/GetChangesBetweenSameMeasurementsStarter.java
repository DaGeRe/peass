package de.dagere.peass;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.peran.analysis.helper.AnalysisUtil;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Compares measurements of same versions and testcases in order to find out whether there results differ.
 * @author reichelt
 *
 */
public class GetChangesBetweenSameMeasurementsStarter implements Callable<Void> {

   @Option(names = { "-dependencyFile", "--dependencyFile" }, description = "Internal only")
   private File dependencyFile;
   
   @Option(names = { "-data", "--data" }, description = "Internal only")
   private File[] data;
   
   

   public static void main(final String[] args) throws JAXBException, JsonGenerationException, JsonMappingException, IOException {
      try {
         final CommandLine commandLine = new CommandLine(new GetChangesBetweenSameMeasurementsStarter());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
      
   }
   
   @Override
   public Void call() throws Exception {
      final long start = System.currentTimeMillis();
      
      final String projectName = VersionComparator.getProjectName();
      AnalysisUtil.setProjectName(projectName);
//      oldKnowledge = VersionKnowledge.getOldChanges();
      
      
      for (File folder : data) {
         final ChangeReader reader = new ChangeReader(folder, projectName);
         reader.readFile(folder);
         final GetChangesBetweensameMeasurements comparator = new GetChangesBetweensameMeasurements(reader.getAllData());
         comparator.examineDiffs();
      }
      

      
      final long end = System.currentTimeMillis();
      System.out.println("Duration: "+ (end - start / 10E6));
      return null;
   }

   
}
