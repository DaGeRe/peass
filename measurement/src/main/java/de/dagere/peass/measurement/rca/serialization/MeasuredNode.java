package de.dagere.peass.measurement.rca.serialization;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.inference.TestUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.dagere.peass.measurement.rca.data.BasicNode;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.OneVMResult;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

public class MeasuredNode extends BasicNode {

   // Contains duration in microseconds
   private TestcaseStatistic statistic;
   private List<MeasuredNode> childs = new LinkedList<>();

   @JsonInclude(Include.NON_NULL)
   private MeasuredValues values;
   @JsonInclude(Include.NON_NULL)
   private MeasuredValues valuesPredecessor;


   @JsonCreator
   public MeasuredNode(@JsonProperty("call") final String call,
         @JsonProperty("kiekerPattern") final String kiekerPattern,
         @JsonProperty("otherKiekerPattern") final String otherKiekerPattern) {
      super(call, kiekerPattern, otherKiekerPattern);
   }

   @Override
   public String getCall() {
      return call;
   }

   public void setCall(final String call) {
      this.call = call;
   }

   @Override
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

   /**
    * Gets VMs that are there without processing (i.e. outliers are still contained)
    * 
    * @return
    */
   @JsonIgnore
   public int getPureVMs() {
      return values.getValues().size();
   }

   /**
    * Gets precessor version VMs that are there without processing (i.e. outliers are still contained)
    * 
    * @return
    */
   @JsonIgnore
   public int getVMsOld() {
      return valuesPredecessor.getValues().size();
   }

   public List<MeasuredNode> getChilds() {
      return childs;
   }

   public void setChilds(final List<MeasuredNode> childs) {
      this.childs = childs;
   }

   @JsonIgnore
   @Override
   public List<MeasuredNode> getChildren() {
      return childs;
   }

   public MeasuredNode getChildByPattern(final String name) {
      MeasuredNode result = null;
      for (MeasuredNode node : childs) {
         if (node.getKiekerPattern().contentEquals(name)) {
            result = node;
         }
      }
      return result;
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
//      final double tValue = TestUtils.t(statistic.getStatisticsCurrent(), statistic.getStatisticsOld());
      // System.out.println(tValue);
      final boolean value = TestUtils.tTest(statistic.getStatisticsCurrent(), statistic.getStatisticsOld(), type2error);
      return value;
   }

   public void setValues(final CallTreeNode rawDataNode, final String version, final String predecessor) {
      System.out.println("Persisting: " + version + " " + rawDataNode.getCall());
      values = new MeasuredValues();
      valuesPredecessor = new MeasuredValues();
      persistValues(rawDataNode.getResults(version), values);
      persistValues(rawDataNode.getResults(predecessor), valuesPredecessor);
   }

   private void persistValues(final List<OneVMResult> results, final MeasuredValues values) {
      if (results != null) {
         for (int i = 0; i < results.size(); i++) {
            final OneVMResult oneVMResult = results.get(i);
            final List<StatisticalSummary> value = oneVMResult.getValues();
            values.getValues().put(i, value);
         }
      }
   }

   @Override
   public String toString() {
      return "measured: " + kiekerPattern;
   }
}
