package de.dagere.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.dependency.execution.ProjectModules;

public class TestChangedNames {

   @Test
   public void testPrefix() throws IOException {
      File settingsFile = new File(TestConstants.TEST_RESOURCES, "gradle/settings_with_changed_prefix.gradle");
      
      File goalFile = new File(TestConstants.CURRENT_FOLDER, "settings.gradle");
      FileUtils.copyFile(settingsFile, goalFile);
      
      createModuleFoldersPrefix();
      
      ProjectModules modules = SettingsFileParser.getModules(TestConstants.CURRENT_FOLDER);
      
      Assert.assertEquals(3, modules.getModules().size());
   }
   
   @Test
   public void testSuffix() throws IOException {
      File settingsFile = new File(TestConstants.TEST_RESOURCES, "gradle/settings_with_changed_suffix.gradle");
      
      File goalFile = new File(TestConstants.CURRENT_FOLDER, "settings.gradle");
      FileUtils.copyFile(settingsFile, goalFile);
      
      createModuleFoldersSuffix();
      
      ProjectModules modules = SettingsFileParser.getModules(TestConstants.CURRENT_FOLDER);
      
      Assert.assertEquals(3, modules.getModules().size());
   }
   
   @Test
   public void testBoth() throws IOException {
      File settingsFile = new File(TestConstants.TEST_RESOURCES, "gradle/settings_with_changed_both.gradle");
      
      File goalFile = new File(TestConstants.CURRENT_FOLDER, "settings.gradle");
      FileUtils.copyFile(settingsFile, goalFile);
      
      createModuleFoldersBoth();
      
      ProjectModules modules = SettingsFileParser.getModules(TestConstants.CURRENT_FOLDER);
      
      Assert.assertEquals(3, modules.getModules().size());
   }

   private void createModuleFoldersPrefix() {
      File baseModule = new File(TestConstants.CURRENT_FOLDER, "module-base");
      baseModule.mkdir();
      File usingModule = new File(TestConstants.CURRENT_FOLDER, "module-using");
      usingModule.mkdir();
   }
   
   private void createModuleFoldersSuffix() {
      File baseModule = new File(TestConstants.CURRENT_FOLDER, "base-module");
      baseModule.mkdir();
      File usingModule = new File(TestConstants.CURRENT_FOLDER, "using-module");
      usingModule.mkdir();
   }
   
   private void createModuleFoldersBoth() {
      File baseModule = new File(TestConstants.CURRENT_FOLDER, "my-base-module");
      baseModule.mkdir();
      File usingModule = new File(TestConstants.CURRENT_FOLDER, "my-using-module");
      usingModule.mkdir();
   }
}
