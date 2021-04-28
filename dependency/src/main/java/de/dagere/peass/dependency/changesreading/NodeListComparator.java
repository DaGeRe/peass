package de.dagere.peass.dependency.changesreading;

import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

public class NodeListComparator {
   
   private static final Logger LOG = LogManager.getLogger(NodeListComparator.class);
   
   private final List<Node> changes;
   
   final Iterator<? extends Node> node2iterator;
   final Iterator<? extends Node> node1iterator;
   Node child1, child2, last1, last2;
   
   public NodeListComparator(List<Node> changes, List<? extends Node> childs1, List<? extends Node> childs2) {
      super();
      this.changes = changes;
      
      node2iterator = childs2.iterator();
      node1iterator = childs1.iterator();
      last1 = node1iterator.next();
      last2 = node2iterator.next();
   }



   /**
    * Helps getting the changes of a node list of unequal length, e.g. if old version has 5 methods and new 6, it tells whether the old 5 methods, if they are still there and in
    * the same order, are changed.
    * 
    * @param changes
    * @param childs1
    * @param childs2
    */
   public void compareUnequalNodeList() {
      while (node1iterator.hasNext() && node2iterator.hasNext()) {
         child1 = node1iterator.next();
         child2 = node2iterator.next();
         if (child1.getClass() != child2.getClass()) {
            changes.add(child1);
            changes.add(child2);
         } else {
            if (!child1.equals(child2)) {
               if (child1 instanceof MethodDeclaration && child2 instanceof MethodDeclaration && last1 instanceof MethodDeclaration && last2 instanceof MethodDeclaration) {
                  handleMethodOrderChange();
               } else if (child1 instanceof BlockStmt || child2 instanceof BlockStmt) {
                  changes.add(child1);
                  changes.add(child2);
               } else {
                  /*
                   * Currently, nothing is done in this case, as a declaration change should cause an call change (which will be an BlockStmt-Change), or the method is not called,
                   * than the declaration change will not be relevant. TODO Is it detected, if the e.g. a public static ClassA asd = new ClassA(1) instead of new ClassA(2)? (cause
                   * this is a declaration as well)
                   */
                  changes.addAll(FileComparisonUtil.comparePairwise(child1, child2));
               }
            } else {
               LOG.trace("Equal: {} {}", child1, child2);
            }
         }
         last1 = child1;
         last2 = child2;
      }
   }



   public void handleMethodOrderChange() {
      final MethodDeclaration md1 = (MethodDeclaration) child1;
      final MethodDeclaration md2 = (MethodDeclaration) child2;
      final MethodDeclaration last1Method = (MethodDeclaration) last1;
      final MethodDeclaration last2Method = (MethodDeclaration) last2;
      if (md2.getDeclarationAsString().equals(last1Method.getDeclarationAsString())) {
         changes.addAll(FileComparisonUtil.comparePairwise(last1Method, md2));
         if (node2iterator.hasNext()) {
            child2 = node2iterator.next();
            changes.addAll(FileComparisonUtil.comparePairwise(md1, child2));
         }
      }
      if (md1.getDeclarationAsString().equals(last2Method.getDeclarationAsString())) {
         changes.addAll(FileComparisonUtil.comparePairwise(md1, last2Method));
         if (node1iterator.hasNext()) {
            child1 = node1iterator.next();
            changes.addAll(FileComparisonUtil.comparePairwise(child1, md1));
         }
      }
   }
}
