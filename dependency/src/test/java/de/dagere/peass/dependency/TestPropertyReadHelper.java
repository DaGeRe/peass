package de.dagere.peass.dependency;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.analysis.properties.PropertyReadHelper;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;

public class TestPropertyReadHelper {
   @Test
   public void testDetermineEntity() {
      ChangedEntity entity = PropertyReadHelper.determineEntity("module" + ChangedEntity.MODULE_SEPARATOR + "clazz" + ChangedEntity.METHOD_SEPARATOR + "method");
      
      Assert.assertEquals("module", entity.getModule());
      Assert.assertEquals("clazz", entity.getClazz());
      Assert.assertEquals("method", entity.getMethod());
   }
   
   @Test
   public void testDetermineEntityModuleless() {
      ChangedEntity entity = PropertyReadHelper.determineEntity("clazz" + ChangedEntity.METHOD_SEPARATOR + "method");
      
      Assert.assertEquals("", entity.getModule());
      Assert.assertEquals("clazz", entity.getClazz());
      Assert.assertEquals("method", entity.getMethod());
   }
   
   @Test
   public void testDetermineEntityParameters() {
      ChangedEntity entity = PropertyReadHelper.determineEntity("clazz" + ChangedEntity.METHOD_SEPARATOR + "method(int,String)");
      
      Assert.assertEquals("", entity.getModule());
      Assert.assertEquals("clazz", entity.getClazz());
      Assert.assertEquals("method", entity.getMethod());
      Assert.assertEquals("String", entity.getParameters().get(1));
   }
}
