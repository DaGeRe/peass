package de.peass.measurement.searchcause.serialization;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.inference.TestUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.data.OneVMResult;

public class MeasuredNode {
   private String call;
   private String kiekerPattern;
   private String otherCall;
   private TestcaseStatistic statistic;
   private List<MeasuredNode> childs = new LinkedList<>();
   
   @JsonInclude(Include.NON_NULL)
   private MeasuredValues values;
   @JsonInclude(Include.NON_NULL)
   private MeasuredValues valuesPredecessor;

   public String getOtherCall() {
      return otherCall;
   }

   public void setOtherCall(final String otherCall) {
      this.otherCall = otherCall;
   }

   public String getCall() {
      return call;
   }

   public void setCall(final String call) {
      this.call = call;
   }
   
   public String getKiekerPattern() {
      return kiekerPattern;
   }

   public void setKiekerPattern(final String kiekerPattern) {
      this.kiekerPattern = kiekerPattern;
   }

   public TestcaseStatistic getStatistic() {
      return statistic;
   }

   public void setStatistic(final TestcaseStatistic statistic) {
      this.statistic = statistic;
   }

   public List<MeasuredNode> getChilds() {
      return childs;
   }

   public void setChilds(final List<MeasuredNode> childs) {
      this.childs = childs;
   }

   public MeasuredValues getValues() {
      return values;
   }

   public void setValues(final MeasuredValues values) {
      this.values = values;
   }
   
   public MeasuredValues getValuesPredecessor() {
      return valuesPredecessor;
   }

   public void setValuesPredecessor(final MeasuredValues valuesPredecessor) {
      this.valuesPredecessor = valuesPredecessor;
   }

   @JsonIgnore
   public boolean isChange(final double type2error) {
      final double tValue = TestUtils.t(statistic.getStatisticsCurrent(), statistic.getStatisticsOld());
      System.out.println(tValue);
      final boolean value = TestUtils.tTest(statistic.getStatisticsCurrent(), statistic.getStatisticsOld(), type2error);
      return value;
   }

   public void setValues(final CallTreeNode rawDataNode, final String version, final String predecessor) {
      values = new MeasuredValues();
      valuesPredecessor = new MeasuredValues();
      persistValues(rawDataNode.getResults(version), values);
      persistValues(rawDataNode.getResults(predecessor), valuesPredecessor);
   }

   private void persistValues(final List<OneVMResult> results, final MeasuredValues values) {
      for (int i = 0; i < results.size(); i++) {
         final List<Double> value = new LinkedList<>();
         values.getValues().put(i, value);
         value.add(results.get(i).getAverage());
      }
   }
}
