package de.peass.visualization;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.peass.measurement.analysis.statistics.TestcaseStatistic;

class Node {
   private String name;
   private String parent;
   private List<Node> children = new LinkedList<>();
   private String color;
   private TestcaseStatistic statistic;
   
   @JsonInclude(Include.NON_NULL)
   private double[] values = null;
   @JsonInclude(Include.NON_NULL)
   private double[] valuesPredecessor = null;

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

   public List<Node> getChildren() {
      return children;
   }

   public void setChildren(final List<Node> children) {
      this.children = children;
   }
}