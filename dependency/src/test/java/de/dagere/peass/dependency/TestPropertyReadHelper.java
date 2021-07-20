package de.dagere.peass.dependency;

import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.properties.ChangeProperty;
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
   
   @Test
   public void testNullVersion() throws IOException {
      Change changeMock = Mockito.mock(Change.class);
      Mockito.when(changeMock.getMethod()).thenReturn("myTestMethod");
      PropertyReadHelper helper = new PropertyReadHelper("000001", null, new ChangedEntity("Test"), changeMock, null, null, null, null);
      ChangeProperty emptyProperty = helper.read();
      MatcherAssert.assertThat(emptyProperty, IsNull.notNullValue());
      Assert.assertEquals("myTestMethod", emptyProperty.getMethod());
   }
}
