package de.peass.dependency.analysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.ClazzFileFinder;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.execution.TestExecutor;

public class ModuleClassMapping {

   public static final ModuleClassMapping SINGLE_MODULE_MAPPING = new ModuleClassMapping();

   private ModuleClassMapping() {
   }

   private static final Logger LOG = LogManager.getLogger(ModuleClassMapping.class);

   private final Map<String, String> mapping = new HashMap<>();

   public ModuleClassMapping(final File baseFolder, final List<File> modules) {
      for (final File module : modules) {
         final List<String> classes = ClazzFileFinder.getClasses(module);
         String moduleName;
         if (module.equals(baseFolder)) {
            moduleName = "";
         } else {
            moduleName = getModuleName(baseFolder, module);
         }
         LOG.debug("Module: {} Name: {}", module.getAbsolutePath(), moduleName);
         for (final String clazz : classes) {
            mapping.put(clazz, moduleName);
         }
      }
   }
   
   public ModuleClassMapping(final File baseFolder) throws IOException, XmlPullParserException {
      final List<File> modules = TestExecutor.getModules(new PeASSFolders(baseFolder));
      for (final File module : modules) {
         final List<String> classes = ClazzFileFinder.getClasses(module);
         String moduleName;
         if (module.equals(baseFolder)) {
            moduleName = "";
         } else {
            moduleName = getModuleName(baseFolder, module);
         }
         LOG.debug("Module: {} Name: {}", module.getAbsolutePath(), moduleName);
         for (final String clazz : classes) {
            mapping.put(clazz, moduleName);
         }
      }
   }
   
   

   public ModuleClassMapping(final TestExecutor executor) throws IOException, XmlPullParserException {
      this(executor.getProjectFolder(), executor.getModules());
   }

   public String getModuleOfClass(final String clazz) {
      if (this == SINGLE_MODULE_MAPPING) {
         return "";
      }
      return mapping.get(clazz);
   }

   public static String getModuleName(final File baseFolder, final File module) {
      String moduleName;
      final int pathIndex = baseFolder.getAbsolutePath().length() + 1;
      final String modulePath = module.getAbsolutePath();
      if (modulePath.length() > pathIndex) {
         moduleName = modulePath.substring(pathIndex);
      } else {
         moduleName = "";
      }
      return moduleName;
   }
}
