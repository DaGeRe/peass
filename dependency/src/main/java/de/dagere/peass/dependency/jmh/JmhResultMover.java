package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Kopemedata.Testcases;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.utils.Constants;

public class JmhResultMover {

   private final PeASSFolders folders;
   private final File sourceResultFolder;

   public JmhResultMover(final PeASSFolders folders) {
      this.folders = folders;
      final File[] files = folders.getTempMeasurementFolder().listFiles(new FileFilter() {

         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().startsWith("kieker-");
         }
      });
      if (files.length > 0) {
         sourceResultFolder = files[0];
      } else {
         sourceResultFolder = null;
      }
   }

   public void moveToMethodFolder(final TestCase testcase, final File sourceJsonResultFile) {
      final File moduleResultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      final File clazzResultFolder = new File(moduleResultsFolder, testcase.getClazz());

      final File kiekerTimeFolder = new File(clazzResultFolder, Long.toString(System.currentTimeMillis()) + File.separator + testcase.getMethod());
      kiekerTimeFolder.mkdirs();

      final File expectedKoPeMeFile = new File(clazzResultFolder, testcase.getMethod() + ".xml");
      convertToXMLData(sourceJsonResultFile, expectedKoPeMeFile);
      
      if (sourceResultFolder != null) {
         final File kiekerSubfolder = new File(kiekerTimeFolder, sourceResultFolder.getName());
         sourceResultFolder.renameTo(kiekerSubfolder);
      }
   }

   private void convertToXMLData(final File sourceJsonResultFile, final File expectedKoPeMeFile) {
      try {
         ArrayNode benchmarks = (ArrayNode) Constants.OBJECTMAPPER.readTree(sourceJsonResultFile);
         JsonNode benchmark = benchmarks.get(0);
         String name = benchmark.get("benchmark").asText();
         JsonNode primaryMetric = benchmark.get("primaryMetric");
         ArrayNode rawData = (ArrayNode) primaryMetric.get("rawData");
         
         Kopemedata transformed = new Kopemedata();
         transformed.setTestcases(new Testcases());
         TestcaseType testclazz = new TestcaseType();
         transformed.getTestcases().getTestcase().add(testclazz);
         testclazz.setName(name.substring(0, name.lastIndexOf('.')));
         
         XMLDataStorer.storeData(expectedKoPeMeFile, transformed);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
