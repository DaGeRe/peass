package de.dagere.peass.dependency;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.testtransformation.TestTransformer;

public class TestFinder {
   
   private static final Logger LOG = LogManager.getLogger(TestFinder.class);
   
   protected final TestExecutor executor;
   
   public TestFinder(TestExecutor executor) {
      this.executor = executor;
   }

   public TestSet findIncludedTests(final ModuleClassMapping mapping) throws IOException {
      List<String> includedModules = getIncludedModules();

      TestTransformer testTransformer = executor.getTestTransformer();
      return testTransformer.findModuleTests(mapping, includedModules, executor.getModules());
   }

   private List<String> getIncludedModules() throws IOException {
      List<String> includedModules;
      if (executor.getTestTransformer().getConfig().getExecutionConfig().getPl() != null) {
         String pl = executor.getTestTransformer().getConfig().getExecutionConfig().getPl();
         includedModules = MavenPomUtil.getDependentModules(executor.getFolders().getProjectFolder(), pl, executor.getEnv());
         LOG.debug("Included modules: {}", includedModules);
      } else {
         includedModules = null;
      }
      return includedModules;
   }
}
