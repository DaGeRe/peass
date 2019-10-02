package de.peass.breaksearch.treeanalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.peass.measurement.searchcause.data.CauseSearchData;

class TestVersionPairManager {
   private Map<String, Map<String, TestVersionPair>> data = new HashMap<>();

   public void addData(final CauseSearchData data2) {
      Map<String, TestVersionPair> versionData = data.get(data2.getVersion());
      if (versionData == null) {
         versionData = new HashMap<String, TestVersionPair>();
         data.put(data2.getVersion(), versionData);
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
}