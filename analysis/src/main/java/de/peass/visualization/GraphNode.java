package de.peass.visualization;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.peass.measurement.rca.data.BasicNode;

public class GraphNode extends BasicNode {

   public static enum State{
      FASTER, SLOWER, UNKNOWN;
   }
   
   private String name, key;
   private String parent;
   private String color;
   private TestcaseStatistic statistic;
   private boolean hasSourceChange = false;
   private State state;
   
   @JsonInclude(Include.NON_NULL)
   private double[] values = null;
   @JsonInclude(Include.NON_NULL)
   private double[] valuesPredecessor = null;
   private List<GraphNode> children = new LinkedList<>();
   
   public GraphNode(final String call, final String kiekerPattern) {
      super(call, kiekerPattern);
      key = KiekerPatternConverter.getKey(kiekerPattern);
   }
   
   public String getKey() {
      return key;
   }

   public void setKey(String key) {
      this.key = key;
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

   public TestcaseStatistic getStatistic() {
      return statistic;
   }

   public void setStatistic(final TestcaseStatistic statistic) {
      this.statistic = statistic;
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
}