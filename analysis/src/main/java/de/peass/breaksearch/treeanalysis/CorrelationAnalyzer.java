package de.peass.breaksearch.treeanalysis;

import java.util.Iterator;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.peass.measurement.rca.serialization.MeasuredNode;
import de.peass.measurement.rca.treeanalysis.TreeUtil;

class CorrelationAnalyzer {
   private final MeasuredNode node1, node2;
   private CorrelatedPair means;
   private CorrelatedPair oldMeans;
   private CorrelatedPair tVals;
   private CorrelatedPair bucketTVals;
   private double threshold;

   public CorrelationAnalyzer(final MeasuredNode node1, final MeasuredNode node2) {
      this.node1 = node1;
      this.node2 = node2;
      setThreshold(1.0);

   }

   public boolean treeStructureEqual() {
      return treeStructureEqual(node1, node2);
   }

   private boolean treeStructureEqual(final MeasuredNode node1, final MeasuredNode node2) {
      boolean allEqual = true;
      if (node1.getChilds().size() == node2.getChilds().size() && TreeUtil.childrenEqual(node1, node2)) {
         final Iterator<MeasuredNode> predecessorIterator = node1.getChilds().iterator();
         final Iterator<MeasuredNode> currentIterator = node2.getChilds().iterator();

         for (; currentIterator.hasNext() && predecessorIterator.hasNext();) {
            final MeasuredNode currentPredecessorNode = predecessorIterator.next();
            final MeasuredNode currentVersionNode = currentIterator.next();
            if (currentPredecessorNode.getStatistic().getMeanCurrent() > 1.0 && currentVersionNode.getStatistic().getMeanOld() > 1.0) {
               allEqual &= treeStructureEqual(currentPredecessorNode, currentVersionNode);
            }
         }
      } else {
         System.out.println("Different: " + node1.getKiekerPattern() + " " + node1.getChilds().size() + " " +  node2.getChilds().size());
         for (final MeasuredNode node1Child : node1.getChilds()) {
            System.out.println("First: " + node1Child.getKiekerPattern());
         }
         for (final MeasuredNode node2Child : node2.getChilds()) {
            System.out.println("Second: " + node2Child.getKiekerPattern());
         }
         allEqual = false;
      }
      return allEqual;
   }

   public void setThreshold(final double threshold) {
      this.threshold = threshold;
      means = new CorrelatedPair();
      oldMeans = new CorrelatedPair();
      tVals = new CorrelatedPair();
      bucketTVals = new CorrelatedPair();
      addValues(node1, node2);
      getPredecessorValues(node1, node2);
   }

   private void getPredecessorValues(final MeasuredNode node1, final MeasuredNode node2) {
      if (node1.getChilds().size() == node2.getChilds().size() && TreeUtil.childrenEqual(node1, node2)) {
         // System.out.println(node1.getCall() + " " + node2.getCall());
         final Iterator<MeasuredNode> iterator1 = node1.getChilds().iterator();
         final Iterator<MeasuredNode> iterator2 = node2.getChilds().iterator();

         for (; iterator2.hasNext() && iterator1.hasNext();) {
            final MeasuredNode iterator1node = iterator1.next();
            final MeasuredNode iterator2node = iterator2.next();
            addValues(iterator1node, iterator2node);
            getPredecessorValues(iterator1node, iterator2node);
         }
      }
   }

   private void addValues(final MeasuredNode iterator1node, final MeasuredNode iterator2node) {
      final TestcaseStatistic stat1 = iterator1node.getStatistic();
      final TestcaseStatistic stat2 = iterator2node.getStatistic();
      if (stat1.getMeanCurrent() > threshold && stat2.getMeanOld() > threshold) {
         means.addValue(stat1.getMeanCurrent(), stat2.getMeanCurrent());
         oldMeans.addValue(stat1.getMeanOld(), stat2.getMeanOld());
         tVals.addValue(stat1.getTvalue(), stat2.getTvalue());
         bucketTVals.addValue(tDecisionValue(stat1.getTvalue()), tDecisionValue(stat2.getTvalue()));
      }
   }

   double tDecisionValue(final double tValue) {
      if (tValue > 1.98) {
         return 1;
      } else if (tValue < -1.98) {
         return -1;
      } else {
         return 0;
      }
   }

   public double getMeanCorrelation() {
      return means.getPearsonCorrelation();
   }

   public double getCrossMeanCorrelation() {
      return oldMeans.getPearsonCorrelation();
   }

   public double getMeanOldCorrelation() {
      final double[][] vals = new double[2][];
      vals[0] = means.getVal1();
      vals[1] = oldMeans.getVal2();
      return new PearsonsCorrelation().correlation(vals[0], vals[1]);
   }

   public double getTCorrelation() {
      return tVals.getPearsonCorrelation();
   }

   public double getBucketTCorrelation() {
      return bucketTVals.getPearsonCorrelation();
   }

   public int getSize() {
      return tVals.getVal1().length;
   }

   void printInfo() {
      System.out.println("Values: " + means.getVal1().length);
      System.out.println("Version: " + getMeanCorrelation() + " " + getSize());
      System.out.println("Old: " + getMeanOldCorrelation());
      System.out.println("T: " + getTCorrelation());
      System.out.println("Bucket: " + getBucketTCorrelation());
   }

}