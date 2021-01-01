package de.peass.visualization;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.peass.measurement.rca.data.BasicNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.kieker.KiekerPatternConverter;
import de.peass.measurement.rca.serialization.MeasuredValues;
import de.peass.visualization.GraphNode.State;

public class GraphNode extends BasicNode {

   public static enum State {
      FASTER, SLOWER, UNKNOWN;
   }

   private String name, key, otherKey;
   private String parent;
   private String color;
   private TestcaseStatistic statistic;
   private boolean hasSourceChange = false;
   private State state;
   private double inVMDeviationPredecessor, inVMDeviation;

   @JsonInclude(Include.NON_NULL)
   private double[] values = null;
   @JsonInclude(Include.NON_NULL)
   private double[] valuesPredecessor = null;
   
   @JsonInclude(Include.NON_NULL)
   private MeasuredValues vmValues = null;
   @JsonInclude(Include.NON_NULL)
   private MeasuredValues vmValuesPredecessor = null;
   
   
   
   private List<GraphNode> children = new LinkedList<>();

   public GraphNode(final String call, final String kiekerPattern, final String otherKiekerPattern) {
      super(call, kiekerPattern, otherKiekerPattern);
      key = KiekerPatternConverter.getKey(kiekerPattern);
      if (!otherKiekerPattern.equals(CauseSearchData.ADDED)) {
         otherKey = KiekerPatternConverter.getKey(otherKiekerPattern);
      }
      if (!kiekerPattern.equals(otherKiekerPattern)) {
         hasSourceChange = true;
      }
   }

   public String getKey() {
      return key;
   }

   public void setKey(String key) {
      this.key = key;
   }
   
   public String getOtherKey() {
      return otherKey;
   }
   
   public void setOtherKey(String otherKey) {
      this.otherKey = otherKey;
   }

   public double[] getValues() {
      return values;
   }

   public void setValues(final double[] values) {
      this.values = values;
   }

   public double[] getValuesPredecessor() {
      return valuesPredecessor;
   }

   public void setValuesPredecessor(final double[] valuesPredecessor) {
      this.valuesPredecessor = valuesPredecessor;
   }
   
   public MeasuredValues getVmValues() {
      return vmValues;
   }

   public void setVmValues(MeasuredValues vmValues) {
      this.vmValues = vmValues;
   }

   public MeasuredValues getVmValuesPredecessor() {
      return vmValuesPredecessor;
   }

   public void setVmValuesPredecessor(MeasuredValues vmValuesPredecessor) {
      this.vmValuesPredecessor = vmValuesPredecessor;
   }

   public TestcaseStatistic getStatistic() {
      return statistic;
   }

   public void setStatistic(final TestcaseStatistic statistic) {
      this.statistic = statistic;
   }
   
   public double getInVMDeviationPredecessor() {
      return inVMDeviationPredecessor;
   }

   public void setInVMDeviationPredecessor(double inVMDeviationPredecessor) {
      this.inVMDeviationPredecessor = inVMDeviationPredecessor;
   }

   public double getInVMDeviation() {
      return inVMDeviation;
   }

   public void setInVMDeviation(double inVMDeviation) {
      this.inVMDeviation = inVMDeviation;
   }

   public String getColor() {
      return color;
   }

   public void setColor(final String color) {
      this.color = color;
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getParent() {
      return parent;
   }

   public void setParent(final String parent) {
      this.parent = parent;
   }

   @Override
   public List<GraphNode> getChildren() {
      return children;
   }

   public void setChildren(final List<GraphNode> children) {
      this.children = children;
   }

   @Override
   public String toString() {
      return "Graph: " + kiekerPattern;
   }

   public boolean isHasSourceChange() {
      return hasSourceChange;
   }

   public void setHasSourceChange(final boolean hasSourceChange) {
      this.hasSourceChange = hasSourceChange;
   }

   public State getState() {
      return state;
   }

   public void setState(final State state) {
      this.state = state;
   }

   public void setSlower() {
      setColor(NodePreparator.COLOR_SLOWER);
      setState(State.SLOWER);
      getStatistic().setChange(true);
   }

   public void setFaster() {
      setColor(NodePreparator.COLOR_FASTER);
      setState(State.FASTER);
      getStatistic().setChange(true);
   }
}