package de.peass.measurement;

import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.serialization.MeasuredNode;

public class PersistedTestDataBuilder {
   
   final CauseSearchData data = new CauseSearchData();

   public PersistedTestDataBuilder() {
      final MeasuredNode rootMeasured = new MeasuredNode("Test#test", "public void Test.test", null);
      data.setNodes(rootMeasured);
   }
   
   public void addSecondLevel() {
      final MeasuredNode child1 = new MeasuredNode("ClassA.methodA", "public void ClassA.methodA", null);

      final MeasuredNode child2 = new MeasuredNode("ClassC.methodC", "public void ClassC.methodC", null);

      final MeasuredNode rootMeasured = data.getNodes();
      data.setNodes(rootMeasured);
      rootMeasured.getChilds().add(child1);
      rootMeasured.getChilds().add(child2);
   }
   
   public CauseSearchData getData() {
      return data;
   }
}
