package de.dagere.peass.breaksearch;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;



public class IsThereTimeReductionVM {

   private static final Logger LOG = LogManager.getLogger(IsThereTimeReductionVM.class);
   static int speedups = 0;
   static int wrongspeedup = 0, doublespeedup = 0;
   static int wrongChange = 0;
   static int count = 0;
   static int komisch = 0;
   static int vms = 0;

   public static void main(final String[] args)  {
      final File folder = new File(args[0]);
      if (new File(folder, "measurementsFull").exists()) {
         checkFolder(new File(folder, "measurementsFull"));
      } else {
         for (final File slaveFolder : folder.listFiles()) {
            final File contentFolder = new File(slaveFolder, "measurementsFull");
            if (contentFolder.exists()) {
               System.out.println("Checking: " + contentFolder);
               checkFolder(contentFolder);
            }
         }
      }
      System.out.println("Speedups: " + speedups + " Wrong: " + wrongspeedup + " Doublespeedup:" + doublespeedup + " Tests: " + count);
      System.out.println("Komisch: " + komisch);
      System.out.println("Wrong change: " + wrongChange);
      System.out.println("VMs: " + vms);
   }

   private static void checkFolder(final File contentFolder)  {
      for (final File xmlFile : contentFolder.listFiles()) {
         if (xmlFile.getName().endsWith(".json")) {

            Kopemedata kopemedata = JSONDataLoader.loadData(xmlFile);
            for (VMResultChunk chunk : kopemedata.getChunks()) {
               final Map<String, List<VMResult>> data = new HashMap<>();
               String version = chunk.getResults().get(1).getCommit();
               String versionOld = chunk.getResults().get(0).getCommit();
               for (VMResult r : chunk.getResults()) {
                  List<VMResult> current = data.get(r.getCommit());
                  if (current == null) {
                     current = new LinkedList<>();
                     data.put(r.getCommit(), current);
                  }
                  current.add(r);
                  vms++;
               }
               testSpeedup(xmlFile, data, versionOld, version);
            }
            
         }
      }
   }


   private static void testSpeedup(final File xmlFile, final Map<String, List<VMResult>> data, final String versionOld, final String version) {
      count++;
      final int executionCount = data.get(version).size();
      if (executionCount > 3) {
         final boolean isChange = getTValue(data, versionOld, version);
         LOG.info(version + " " + versionOld + " " + data.keySet());
         LOG.info(xmlFile.getName() + " " + executionCount + " " + version + " " + data.get(version).size() + " " + data.get(versionOld).size());
         LOG.info(data.get(version).stream().map(value -> value.getValue()).collect(Collectors.toList()));
         LOG.info(data.get(versionOld).stream().map(value -> value.getValue()).collect(Collectors.toList()));
         for (int vmid = 3; vmid <= executionCount - 1; vmid++) {
            // System.out.println("Test: " + xmlFile.getName() + " VM: " + vmid + " Version: " + version + " " + versionOld);
            final List<Double> before = new LinkedList<>();
            final List<Double> after = new LinkedList<>();
            for (int resultIndex = 0; resultIndex < vmid; resultIndex++) {
               before.add(data.get(version).get(resultIndex).getValue());
               after.add(data.get(versionOld).get(resultIndex).getValue());
            }

            final double[] valsBefore = ArrayUtils.toPrimitive(before.toArray(new Double[0]));
            final double[] valsAfter = ArrayUtils.toPrimitive(after.toArray(new Double[0]));
            boolean decidable = false;
//            final boolean decidable = EarlyBreakDecider.isSavelyDecidable2(vmid - 1, valsBefore, valsAfter);
            // AdaptiveDependencyTester.isBreakPossible(measurementFolder, version, versionOld, testcase, vmid)

            if (decidable) {
               final boolean tNew = TestUtils.tTest(valsBefore, valsAfter, 0.01);
               System.out.println("VM: " + (vmid + 1) + " " + before.size() + " Speedup: " + decidable + " " + xmlFile.getAbsolutePath() + " " + version);

               speedups++;
               if (tNew != isChange) {
                  wrongspeedup++;
                  if (tNew) {
                     wrongChange++;
                  }
               }
               if (vmid + 1 != executionCount) {
                  System.out.println("Largespeedup " + executionCount + " " + (vmid + 1));
                  doublespeedup++;
               }
            } else {
               if (vmid == executionCount && vmid < 100) {
                  System.out.println("Komisch!");
                  komisch++;
               }
            }
         }
      }
   }

   private static boolean getTValue(final Map<String, List<VMResult>> data, final String versionOld, final String version) {
      final List<Double> before = data.get(versionOld).stream().map(value -> value.getValue()).collect(Collectors.toList());
      final List<Double> after = data.get(version).stream().map(value -> value.getValue()).collect(Collectors.toList());

      // T statistic can not be determined if less than 2 values (produces exception..)
      final double[] valsBefore = ArrayUtils.toPrimitive(before.toArray(new Double[0]));
      final double[] valsAfter = ArrayUtils.toPrimitive(after.toArray(new Double[0]));
      final boolean tvalue = TestUtils.tTest(valsBefore, valsAfter, 0.01);
      return tvalue;
   }
}
