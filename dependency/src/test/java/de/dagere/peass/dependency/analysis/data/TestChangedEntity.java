package de.dagere.peass.dependency.analysis.data;

import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.dependency.analysis.data.deserializer.ChangedEntityDeserializer;

public class TestChangedEntity {

   @Test
   public void testAlltogether() {
      ChangedEntity entity = new ChangedEntity("de.test.ClazzA#method(int)");
      Assert.assertEquals("de.test.ClazzA", entity.getClazz());
      Assert.assertEquals("method", entity.getMethod());

      ChangedEntity entityWithModule = new ChangedEntity("moduleA/submodul§de.test.ClazzA#method(int)");
      Assert.assertEquals("moduleA/submodul", entityWithModule.getModule());
      Assert.assertEquals("de.test.ClazzA", entityWithModule.getClazz());
      Assert.assertEquals("method", entityWithModule.getMethod());
   }

   @Test
   public void testParametersDirectlyClazz() {
      ChangedEntity entity = new ChangedEntity("de.ClassA#methodA(de.peass.Test,int,String)", "moduleA");
      System.out.println(entity.getParametersPrintable());
      MatcherAssert.assertThat(entity.getParameters(), Matchers.hasSize(3));
      Assert.assertEquals("methodA", entity.getMethod());
   }

   @Test
   public void testParametersDirectly() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA(de.peass.Test,int,String)");
      System.out.println(entity.getParametersPrintable());
      MatcherAssert.assertThat(entity.getParameters(), Matchers.hasSize(3));
      Assert.assertEquals("methodA", entity.getMethod());
   }

   @Test
   public void testParametersSimple() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA");
      entity.createParameters("de.peass.Test, int, String");
      System.out.println(entity.getParametersPrintable());
      MatcherAssert.assertThat(entity.getParameters(), Matchers.hasSize(3));
   }

   @Test
   public void testParametersGenerics() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA");
      entity.createParameters("Map<String, Map<String, int>>, int, String");
      System.out.println(entity.getParametersPrintable());
      MatcherAssert.assertThat(entity.getParameters(), Matchers.hasSize(3));
   }

   @Test
   public void testParametersDoubleGenerics() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA");
      entity.createParameters("Map<String, Map<String, int>>, Map<String, Map<String, Integer>>, Set<Integer>");
      System.out.println(entity.getParametersPrintable());
      MatcherAssert.assertThat(entity.getParameters(), Matchers.hasSize(3));
   }

   @Test
   public void testParametersTrippleGenerics() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA");
      entity.createParameters("Triple<String, int, String>>, Map<String, Map<String, Integer>>, Set<Integer>");
      System.out.println(entity.getParametersPrintable());
      MatcherAssert.assertThat(entity.getParameters(), Matchers.hasSize(3));
   }

   @Test
   public void testParametersParenthesis() {
      ChangedEntity entity = new ChangedEntity("de.ClassA", "moduleA", "methodA");
      entity.createParameters("(Test, int, String)");
      System.out.println(entity.getParametersPrintable());
      MatcherAssert.assertThat(entity.getParameters().get(0), Matchers.not(Matchers.containsString("(")));
      MatcherAssert.assertThat(entity.getParameters().get(2), Matchers.not(Matchers.containsString(")")));
   }

   @Test
   public void testSerialization() throws JsonProcessingException, IOException {
      String entityString = "de.peass.ClassA#methodA";
      ChangedEntity entity = new ChangedEntityDeserializer().deserializeKey(entityString, null);
      Assert.assertEquals("de.peass.ClassA", entity.getClazz());
      Assert.assertEquals("methodA", entity.getMethod());
      Assert.assertEquals("", entity.getModule());
      
      String entityStringWithModule = "moduleA§de.peass.ClassA#methodA";
      ChangedEntity entityWithModule = new ChangedEntityDeserializer().deserializeKey(entityStringWithModule, null);
      Assert.assertEquals("de.peass.ClassA", entityWithModule.getClazz());
      Assert.assertEquals("methodA", entityWithModule.getMethod());
      Assert.assertEquals("moduleA", entityWithModule.getModule());
   }
}
