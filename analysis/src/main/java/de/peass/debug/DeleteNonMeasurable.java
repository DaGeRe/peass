package de.peass.debug;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.peran.FolderSearcher;

/**
 * There were two bugs in PeASS: - Executionfiles had to many entries (e.g. because javafiles were searched in the wrong place and then got sometimes missing) - The execution was
 * done against the last test execution in the executionfile, and not against the version from the view (this can have unintended side effects e.g. if commons io traverses all
 * files, and therefore added misc files slowdown the execution) This file is a workaround: It deletes all measurements from executions that should not have taken place. This
 * produces clean measurement files.
 * 
 * @author reichelt
 *
 */
public class DeleteNonMeasurable {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException, JAXBException {
      final File repoFolder = new File(args[0]);
      final File dependencyFolder = new File(repoFolder, "dependencies-final");
      final File measurementFolder = new File(repoFolder, "measurementdata" + File.separator + "cleanData");

      int remove = 0, ok = 0;
      for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
            "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
         final File executionFile = new File(dependencyFolder, "execute_" + project + ".json");
         final ExecutionData changedTests = FolderSearcher.MAPPER.readValue(executionFile, ExecutionData.class);

         final File projectMeasurements = new File(measurementFolder, project);
         for (final File measurementFile : projectMeasurements.listFiles()) {
            if (measurementFile.getName().endsWith(".xml")) {
               final Kopemedata data = XMLDataLoader.loadData(measurementFile);
               final List<TestcaseType> testcases = data.getTestcases().getTestcase();
               boolean change = false;
               for (final TestcaseType testcase : testcases) {
                  final ChangedEntity entity = new ChangedEntity(data.getTestcases().getClazz(), "", "");
                  for (final Iterator<Chunk> it = testcase.getDatacollector().get(0).getChunk().iterator(); it.hasNext();) {
                     final Chunk chunk = it.next();
                     final String version = chunk.getResult().get(chunk.getResult().size() - 1).getVersion().getGitversion();
                     final TestSet tests = changedTests.getVersions().get(version);
                     if (tests != null) {
                        final Set<String> methods = tests.getTestcases().get(entity);
                        if (methods != null && !methods.contains(testcase.getName())) {
                           remove = remove(remove, data, testcase, it, version);
                           change = true;
                        } else {
                           ok++;
                        }
                     } else {
                        remove = remove(remove, data, testcase, it, version);
                        change = true;
                     }
                  }
               }
               if (change) {
                  XMLDataStorer.storeData(measurementFile, data);
               }
            }
         }
      }
      System.out.println("Remove: " + remove);
      System.out.println("Ok: " + ok);
   }

   static int remove(int remove, final Kopemedata data, final TestcaseType testcase, final Iterator<Chunk> it, final String version) {
      System.out.println("Remove: " + data.getTestcases().getClazz() + "#" + testcase.getName() + " " + version);
      it.remove();
      remove++;
      return remove;
   }
}
