package de.peass.measurement;

import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.serialization.MeasuredNode;

public class PersistedTestDataBuilder {
   
   final CauseSearchData data = new CauseSearchData();

   public PersistedTestDataBuilder() {
      final MeasuredNode rootMeasured = new MeasuredNode();
      rootMeasured.setCall("Test#test");
      rootMeasured.setKiekerPattern("public void Test.test");
      data.setNodes(rootMeasured);
   }
   
   public void addSecondLevel() {
      final MeasuredNode child1 = new MeasuredNode();
      child1.setCall("ClassA.methodA");
      child1.setKiekerPattern("public void ClassA.methodA");

      final MeasuredNode child2 = new MeasuredNode();
      child2.setCall("ClassC.methodC");
      child2.setKiekerPattern("public void ClassC.methodC");

      final MeasuredNode rootMeasured = data.getNodes();
      data.setNodes(rootMeasured);
      rootMeasured.getChilds().add(child1);
      rootMeasured.getChilds().add(child2);
   }
   
   public CauseSearchData getData() {
      return data;
   }
}
