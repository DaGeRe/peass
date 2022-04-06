package de.dagere.peass.dependency;

import java.io.File;
import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.io.Files;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.properties.ChangeProperty;
import de.dagere.peass.analysis.properties.PropertyReadHelper;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.EntityUtil;

public class TestPropertyReadHelper {
   @Test
   public void testDetermineEntity() {
      ChangedEntity entity = EntityUtil.determineEntity("module" + ChangedEntity.MODULE_SEPARATOR + "clazz" + ChangedEntity.METHOD_SEPARATOR + "method");
      
      Assert.assertEquals("module", entity.getModule());
      Assert.assertEquals("clazz", entity.getClazz());
      Assert.assertEquals("method", entity.getMethod());
   }
   
   @Test
   public void testDetermineEntityModuleless() {
      ChangedEntity entity = EntityUtil.determineEntity("clazz" + ChangedEntity.METHOD_SEPARATOR + "method");
      
      Assert.assertEquals("", entity.getModule());
      Assert.assertEquals("clazz", entity.getClazz());
      Assert.assertEquals("method", entity.getMethod());
   }
   
   @Test
   public void testDetermineEntityParameters() {
      ChangedEntity entity = EntityUtil.determineEntity("clazz" + ChangedEntity.METHOD_SEPARATOR + "method(int,String)");
      
      Assert.assertEquals("", entity.getModule());
      Assert.assertEquals("clazz", entity.getClazz());
      Assert.assertEquals("method", entity.getMethod());
      Assert.assertEquals("String", entity.getParameters().get(1));
   }
   
   @Test
   public void testNullVersion() throws IOException {
      Change changeMock = Mockito.mock(Change.class);
      Mockito.when(changeMock.getMethod()).thenReturn("myTestMethod");
      ExecutionConfig config = new ExecutionConfig();
      config.setCommit("000001");
      
      File projectFolder = new File("target/current");
      Files.touch(new File(projectFolder, "pom.xml"));
      PropertyReadHelper helper = new PropertyReadHelper(config, new ChangedEntity("Test"), changeMock, projectFolder, null, null, null);
      ChangeProperty emptyProperty = helper.read();
      MatcherAssert.assertThat(emptyProperty, IsNull.notNullValue());
      Assert.assertEquals("myTestMethod", emptyProperty.getMethod());
   }
   
   @Test
   public void testDetermineEntityWithDot() {
      ChangedEntity entity = EntityUtil.determineEntityWithDotSeparator("public void clazz.method(int, Object, long)");
      
      Assert.assertEquals("clazz", entity.getClazz());
      Assert.assertEquals("method", entity.getMethod());
      Assert.assertEquals("int_Object_long", entity.getParametersPrintable());
   }
}
