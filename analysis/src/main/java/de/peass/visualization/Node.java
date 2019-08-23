package de.peass.visualization;

import java.util.LinkedList;
import java.util.List;

class Node {
   private String name;
   private String parent;
   private List<Node> children = new LinkedList<Node>();
   private String color;

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