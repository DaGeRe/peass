package de.dagere.peass.measurement.rca.kiekerReading;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;

public class ChangedTreeBuilder {

   public static final String VERSION0 = "000000";
   public static final String VERSION1 = "000001";

   private CallTreeNode root;
   private CallTreeNode A1;
   private CallTreeNode A1_B1_constructor, A1_B1;
   private CallTreeNode B1_constructor;
   private CallTreeNode B1;

   public ChangedTreeBuilder() {
      MeasurementConfig config = new MeasurementConfig(2);
      config.setWarmup(0);
      config.getFixedCommitConfig().setCommit(VERSION1);
      config.getFixedCommitConfig().setCommitOld(VERSION0);
      root = new CallTreeNode("de.dagere.peass.Test#test", "public void de.dagere.peass.Test.test()", "public void de.dagere.peass.Test.test()",
            config);
      CallTreeNode otherRoot = new CallTreeNode("de.dagere.peass.Test#test", "public void de.dagere.peass.Test.test()", "public void de.dagere.peass.Test.test()",
            config);
      root.setOtherCommitNode(otherRoot);
      otherRoot.setOtherCommitNode(root);

      addSimpleNode(root, otherRoot, "de.dagere.peass.ClazzA#<init>()", "new de.dagere.peass.ClazzA.<init>()");

      A1 = root.appendChild("de.dagere.peass.ClazzA#method", "public void de.dagere.peass.ClazzA.method()", "public void de.dagere.peass.ClazzA.method()");
      CallTreeNode A2 = otherRoot.appendChild("de.dagere.peass.ClazzA#method", "public void de.dagere.peass.ClazzA.method()", "public void de.dagere.peass.ClazzA.method()");
      A1.setOtherCommitNode(A2);
      A2.setOtherCommitNode(A1);

      A1_B1_constructor = root.appendChild(CauseSearchData.ADDED, CauseSearchData.ADDED, "new de.dagere.peass.ClazzB.<init>()");
      CallTreeNode A1_B2_constructor = otherRoot.appendChild("de.dagere.peass.ClazzB#<init>", "new de.dagere.peass.ClazzB.<init>()", CauseSearchData.ADDED);
      A1_B1_constructor.setOtherCommitNode(A1_B2_constructor);
      A1_B2_constructor.setOtherCommitNode(A1_B1_constructor);

      A1_B1 = root.appendChild(CauseSearchData.ADDED, CauseSearchData.ADDED, "public void de.dagere.peass.ClazzB.method()");
      CallTreeNode A1_B2 = otherRoot.appendChild("de.dagere.peass.ClazzB#method", "public void de.dagere.peass.ClazzB.method()", CauseSearchData.ADDED);
      A1_B1.setOtherCommitNode(A1_B2);
      A1_B2.setOtherCommitNode(A1_B1);

      B1_constructor = addSimpleNode(root, otherRoot, "de.dagere.peass.ClazzB#<init>()", "new de.dagere.peass.ClazzB.<init>()");

      B1 = root.appendChild("de.dagere.peass.ClazzB#method", "public void de.dagere.peass.ClazzB.method()", "public void de.dagere.peass.ClazzB.method()");
      CallTreeNode B2 = otherRoot.appendChild("de.dagere.peass.ClazzB#method", "public void de.dagere.peass.ClazzB.method()", "public void de.dagere.peass.ClazzB.method()");
      B1.setOtherCommitNode(B2);
      B2.setOtherCommitNode(B1);

   }

   private CallTreeNode addSimpleNode(CallTreeNode root, CallTreeNode otherRoot, String call, String kiekerPattern) {
      CallTreeNode mainNode = root.appendChild(call, kiekerPattern, kiekerPattern);
      CallTreeNode otherNode = otherRoot.appendChild(call, kiekerPattern, kiekerPattern);
      mainNode.setOtherCommitNode(otherNode);
      otherNode.setOtherCommitNode(mainNode);
      return mainNode;
   }

   public CallTreeNode getRoot() {
      return root;
   }

   public CallTreeNode getA1() {
      return A1;
   }
   
   public CallTreeNode getA1_B1_constructor() {
      return A1_B1_constructor;
   }

   public CallTreeNode getA1_B1() {
      return A1_B1;
   }
   
   public CallTreeNode getB1_constructor() {
      return B1_constructor;
   }

   public CallTreeNode getB1() {
      return B1;
   }
}
