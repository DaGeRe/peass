package de.dagere.peass.dependencies.traces;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsArrayWithSize;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.PeassStage;

public class TestPeassStageMerging {
   
   @Test
   public void testBasic() {
      String[] regularParameters = new String[] {"int", "String", "MyClass"};
      String[] internTypes = PeassStage.getInternTypeList(regularParameters);
      MatcherAssert.assertThat(internTypes, IsArrayWithSize.arrayWithSize(3));
   }
   
   @Test
   public void testGenerics() {
      String[] regularParameters = new String[] {"Map<Integer", "String>", "String", "MyClass"};
      String[] internTypes = PeassStage.getInternTypeList(regularParameters);
      MatcherAssert.assertThat(internTypes, IsArrayWithSize.arrayWithSize(3));
   }
   
   @Test
   public void testDoubleGenerics() {
      String[] regularParameters = new String[] {"Map<String", "Map<String", "String>>", "String", "MyClass"};
      String[] internTypes = PeassStage.getInternTypeList(regularParameters);
      MatcherAssert.assertThat(internTypes, IsArrayWithSize.arrayWithSize(3));
   }
   
   @Test
   public void testTripleGenerics() {
      String[] regularParameters = new String[] {"Triple<Integer", "String", "MyType>", "String", "MyClass"};
      String[] internTypes = PeassStage.getInternTypeList(regularParameters);
      MatcherAssert.assertThat(internTypes, IsArrayWithSize.arrayWithSize(3));
   }
}
