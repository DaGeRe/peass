package de.dagere.peass.dependency.analysis.testData;

import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestClazzCall extends TestCase {
   private static final long serialVersionUID = 7326687309901903065L;

   public TestClazzCall(String clazz) {
      super(clazz, null, "", null);
   }

   public TestClazzCall(String clazz, String module) {
      super(clazz, null, module, null);
   }
   
   public TestClazzCall copy() {
      TestClazzCall test = new TestClazzCall(clazz, module);
      return test;
   }
}
