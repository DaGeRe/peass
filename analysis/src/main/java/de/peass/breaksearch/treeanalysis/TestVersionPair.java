package de.peass.breaksearch.treeanalysis;

import java.util.LinkedList;
import java.util.List;

import de.peass.measurement.searchcause.data.CauseSearchData;
import de.peass.measurement.searchcause.serialization.MeasuredNode;

class TestVersionPair {
   private List<CauseSearchData> datas = new LinkedList<>();

   public void add(final CauseSearchData data2) {
      datas.add(data2);
   }

   public void printDiff() {
      final CauseSearchData[] dataArray = datas.toArray(new CauseSearchData[0]);
      System.out.println("Data: " + dataArray.length);
      for (int i = 0; i < dataArray.length; i++) {
         for (int j = i + 1; j < dataArray.length; j++) {
            final MeasuredNode node1 = dataArray[i].getNodes();
            final MeasuredNode node2 = dataArray[j].getNodes();
            final CorrelationAnalyzer correlationAnalyzer = new CorrelationAnalyzer(node1, node2);
            final boolean equal = correlationAnalyzer.treeStructureEqual();
            System.out.println(i + " " + j + " " + equal);
            correlationAnalyzer.printInfo();
            

            correlationAnalyzer.setThreshold(0);
            correlationAnalyzer.printInfo();
            System.out.println();
         }
      }
   }

   public int getValueCount() {
      return datas.get(0).getNodes().getChilds().size();
   }

}