package de.dagere.peass.breaksearch.treeanalysis;

import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;
import de.dagere.peass.measurement.statistics.Relation;
import de.dagere.peass.measurement.statistics.StatisticUtil;

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

   public List<MeasuredNode> getChangedNodes() {
      final MeasuredNode[] nodes = new MeasuredNode[datas.size()];
      int i = 0;
      for (final CauseSearchData data : datas) {
         nodes[i++] = data.getNodes();
      }

      final List<MeasuredNode> changed = getChangedNodes(nodes);
      return changed;
   }

   private List<MeasuredNode> getChangedNodes(final MeasuredNode[] nodes) {
      final List<MeasuredNode> changedNodes = new LinkedList<>();
      boolean oneHasChange = false;
      for (final MeasuredNode node : nodes) {
//         final Relation relation = StatisticUtil.agnosticTTest(node.getStatistic().getStatisticsOld(), node.getStatistic().getStatisticsCurrent(), TreeAnalysis.config);
         final Relation relation = StatisticUtil.isChange(node.getStatistic().getStatisticsOld(), node.getStatistic().getStatisticsCurrent(), TreeAnalysis.config.getStatisticsConfig());
         if (relation == Relation.UNEQUAL
               && (node.getStatistic().getMeanCurrent() > 1.0
               || node.getStatistic().getMeanOld() > 1.0)) {
            oneHasChange = true;
         }
      }
      if (oneHasChange) {
         boolean oneChildChanged = false;
         for (int childIndex = 0; childIndex < nodes[0].getChilds().size(); childIndex++) {
            final MeasuredNode[] childs = new MeasuredNode[nodes.length];
            int nodeIndex = 0;
            boolean allChildsPresent = true;
            for (final MeasuredNode node : nodes) {
               if (node.getChilds().size() > childIndex) {
                  childs[nodeIndex++] = node.getChilds().get(childIndex);
               } else {
                  allChildsPresent = false;
               }
            }
            if (allChildsPresent) {
               final List<MeasuredNode> changedChilds = getChangedNodes(childs);
               if (changedChilds.size() != 0) {
                  changedNodes.addAll(changedChilds);
                  oneChildChanged = true;
               }
            }
            if (!allChildsPresent) {
               printChangedNode(nodes);
               changedNodes.add(nodes[0]);
            }
         }
         if (!oneChildChanged) {
            printChangedNode(nodes);
            changedNodes.add(nodes[0]);
         }
      }
      return changedNodes;
   }

   private void printChangedNode(final MeasuredNode[] nodes) {
      for (final MeasuredNode node : nodes) {
         System.out.println(node.getKiekerPattern() + " " + node.getStatistic());
      }
   }

}