package de.dagere.peass.dependency.analysis.data;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;

public class TestChangedEntity {
   
   @Test
   public void testParametersSimple() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA");
      entity.createParameters("de.peass.Test, int, String");
      System.out.println(entity.getParametersPrintable());
      Assert.assertThat(entity.getParameters(), Matchers.hasSize(3));
   }
   
   @Test
   public void testParametersGenerics() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA");
      entity.createParameters("Map<String, Map<String, int>>, int, String");
      System.out.println(entity.getParametersPrintable());
      Assert.assertThat(entity.getParameters(), Matchers.hasSize(3));
   }
   
   @Test
   public void testParametersDoubleGenerics() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA");
      entity.createParameters("Map<String, Map<String, int>>, Map<String, Map<String, Integer>>, Set<Integer>");
      System.out.println(entity.getParametersPrintable());
      Assert.assertThat(entity.getParameters(), Matchers.hasSize(3));
   }
   
   @Test
   public void testParametersTrippleGenerics() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA");
      entity.createParameters("Triple<String, int, String>>, Map<String, Map<String, Integer>>, Set<Integer>");
      System.out.println(entity.getParametersPrintable());
      Assert.assertThat(entity.getParameters(), Matchers.hasSize(3));
   }
   
   @Test
   public void testParametersParenthesis() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA");
      entity.createParameters("(Test, int, String)");
      System.out.println(entity.getParametersPrintable());
      Assert.assertThat(entity.getParameters().get(0), Matchers.not(Matchers.containsString("(")));
      Assert.assertThat(entity.getParameters().get(2), Matchers.not(Matchers.containsString(")")));
   }
}
