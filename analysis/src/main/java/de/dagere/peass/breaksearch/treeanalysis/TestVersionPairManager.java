package de.dagere.peass.breaksearch.treeanalysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;

class TestVersionPairManager {
   private Map<String, Map<String, TestVersionPair>> data = new HashMap<>();

   public void addData(final CauseSearchData data2) {
      Map<String, TestVersionPair> versionData = data.get(data2.getMeasurementConfig().getExecutionConfig().getCommit());
      if (versionData == null) {
         versionData = new HashMap<String, TestVersionPair>();
         data.put(data2.getMeasurementConfig().getExecutionConfig().getCommit(), versionData);
      }
      TestVersionPair tvp = versionData.get(data2.getTestcase());
      if (tvp == null) {
         tvp = new TestVersionPair();
         versionData.put(data2.getTestcase(), tvp);
      }
      tvp.add(data2);
   }

   public void printAll() {
      for (final Entry<String, Map<String, TestVersionPair>> versionEntry : data.entrySet()) {
         for (final Entry<String, TestVersionPair> testcaseEntry : versionEntry.getValue().entrySet()) {
            System.out.println(versionEntry.getKey() + "-" + testcaseEntry.getKey());
            if (testcaseEntry.getValue().getValueCount() > 0) {
               testcaseEntry.getValue().printDiff();
            }
         }
      }
   }

   public void printChanged() {
      for (final Entry<String, Map<String, TestVersionPair>> versionEntry : data.entrySet()) {
         for (final Entry<String, TestVersionPair> testcaseEntry : versionEntry.getValue().entrySet()) {
            System.out.println(versionEntry.getKey() + "-" + testcaseEntry.getKey());
            final List<MeasuredNode> changed = testcaseEntry.getValue().getChangedNodes();
            for (final MeasuredNode node : changed) {
//               System.out.println(StatisticUtil.getCriticalValueType1(TreeAnalysis.config.getType1error() / 10, node.getStatistic().getVMs()));
//               System.out.println(StatisticUtil.getCriticalValueType2(TreeAnalysis.config.getType2error() / 10, node.getStatistic().getVMs()));
               System.out.println(node.getKiekerPattern() + " " + node.getStatistic());
            }
         }
      }

   }
}