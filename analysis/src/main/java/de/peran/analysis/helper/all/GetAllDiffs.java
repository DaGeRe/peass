package de.peran.analysis.helper.all;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.properties.ChangeProperties;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.PropertyProcessor;
import de.peass.analysis.properties.VersionChangeProperties;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.traces.OneTraceGenerator;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.utils.Constants;
import de.peass.utils.StreamGobbler;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.AnalysisUtil;

public class GetAllDiffs {
   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      final File dependencyFolder = new File(CleanAll.defaultDependencyFolder);

      for (final String project : new String[] { "commons-io", "commons-dbcp", "commons-compress", "commons-csv", "commons-fileupload" }) {
         final File viewFolder = new File("results/views/" + project);
         viewFolder.mkdirs();
         final File dependencyFile = new File(dependencyFolder, "deps_" + project + ".xml");
         final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);
         AnalysisUtil.setProjectName(project);
         final File resultFile = new File("results" + File.separator + project + File.separator + "properties.json");

         final VersionChangeProperties versionProperties = FolderSearcher.MAPPER.readValue(resultFile, VersionChangeProperties.class);

         versionProperties.executeProcessor(new PropertyProcessor() {

            @Override
            public void process(final String version, final String testcase, final ChangeProperty change, final ChangeProperties changeProperties) {
               final File versionViewFolder = new File(dependencyFolder, "views_" + project + "/view_" + version);
               final File testcaseFolder = new File(versionViewFolder, testcase + File.separator + change.getMethod());
               final File file1 = new File(testcaseFolder, version.substring(0, 6) + OneTraceGenerator.NOCOMMENT);
               File file2 = null;
               for (final File file : testcaseFolder.listFiles((FileFilter) new WildcardFileFilter("*_method"))) {
                  if (!file.equals(file1)) {
                     file2 = file;
                  }
               }
               try {
                  final Process checkDiff = Runtime.getRuntime().exec("diff --ignore-all-space " + file1.getAbsolutePath() + " " + file2.getAbsolutePath());
                  final String isDifferent = StreamGobbler.getFullProcess(checkDiff, false);
                  final File goal = new File(viewFolder, version.substring(0, 6) + "_" + testcase + "_" + change.getMethod());
                  final BufferedWriter writer = new BufferedWriter(new FileWriter(goal));
                  writer.write(isDifferent);
                  writer.flush();
                  writer.close();
               } catch (final IOException e) {
                  e.printStackTrace();
               }

            }
         });
      }
   }
}
