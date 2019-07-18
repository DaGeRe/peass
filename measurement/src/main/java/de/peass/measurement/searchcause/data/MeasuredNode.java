package de.peass.measurement.searchcause.data;

import java.util.List;

import de.peass.measurement.analysis.statistics.TestcaseStatistic;

public class MeasuredNode {
   private String call;
   private TestcaseStatistic statistic;
   private List<MeasuredNode> childs;

   public String getCall() {
      return call;
   }

   public void setCall(String call) {
      this.call = call;
   }

   public TestcaseStatistic getStatistic() {
      return statistic;
   }

   public void setStatistic(TestcaseStatistic statistic) {
      this.statistic = statistic;
   }

   public List<MeasuredNode> getChilds() {
      return childs;
   }

   public void setChilds(List<MeasuredNode> childs) {
      this.childs = childs;
   }
}
