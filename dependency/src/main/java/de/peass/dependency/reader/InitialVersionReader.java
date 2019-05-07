package de.peass.dependency.reader;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.DependencyManager;
import de.peass.dependency.analysis.data.CalledMethods;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestDependencies;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.InitialDependency;
import de.peass.dependency.persistence.InitialVersion;
import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.vcs.VersionIterator;

public class InitialVersionReader {
   
   private static final Logger LOG = LogManager.getLogger(InitialVersionReader.class);
   
   protected final Dependencies dependencyResult;
   protected DependencyManager dependencyManager;
   protected VersionIterator iterator;
   protected TestDependencies dependencyMap;
   
   
   public InitialVersionReader(Dependencies dependencyResult, DependencyManager dependencyManager, VersionIterator iterator) {
      super();
      this.dependencyResult = dependencyResult;
      this.dependencyManager = dependencyManager;
      this.iterator = iterator;
      dependencyMap = dependencyManager.getDependencyMap();
   }
   
   public boolean readInitialVersion() throws IOException, InterruptedException, XmlPullParserException {
      if (!dependencyManager.initialyGetTraces(iterator.getTag())) {
         return false;
      }
      final InitialVersion initialversion = createInitialVersion();
      dependencyResult.setInitialversion(initialversion);
      return true;
   }

   public InitialVersion createInitialVersion() {
      int jdkversion = dependencyManager.getExecutor().getJDKVersion();
      final InitialVersion initialversion = new InitialVersion();
      initialversion.setVersion(iterator.getTag());
      initialversion.setJdk(jdkversion);
      LOG.debug("Starting writing: {}", dependencyMap.getDependencyMap().size());
      for (final Entry<ChangedEntity, CalledMethods> dependencyEntry : dependencyMap.getDependencyMap().entrySet()) {
         final ChangedEntity testcase = dependencyEntry.getKey();
         for (final Map.Entry<ChangedEntity, Set<String>> calledClassEntry : dependencyEntry.getValue().getCalledMethods().entrySet()) {
            final ChangedEntity dependentclass = calledClassEntry.getKey();
            if (!dependentclass.getJavaClazzName().contains("junit") && !dependentclass.getJavaClazzName().contains("log4j")) {
               for (final String dependentmethod : calledClassEntry.getValue()) {
                  final ChangedEntity callee = new ChangedEntity(dependentclass.getClazz(), dependentclass.getModule(), dependentmethod);
                  initialversion.addDependency(testcase, callee);
               }
            }
         }
         initialversion.sort(testcase);
      }
      return initialversion;
   }
   
   public void readCompletedVersions() {
      fillInitialTestDependencies();
      checkCorrectness();

      final InitialVersion initialversion = createInitialVersion();
      dependencyResult.setInitialversion(initialversion);

      if (dependencyResult.getVersions().size() > 0) {
         for (final Map.Entry<String, Version> version : dependencyResult.getVersions().entrySet()) {
            String tag = version.getKey();
            String startTag = iterator.getTag();
            if (VersionComparator.isBefore(tag, startTag) || tag.equals(startTag)) {
               addVersionTestDependencies(version.getValue());
            }
         }
      }
      checkCorrectness();
      

      LOG.debug("Analysiere {} Einträge", iterator.getSize());
   }

   private void addVersionTestDependencies(final Version version) {
      for (final Entry<ChangedEntity, TestSet> dependency : version.getChangedClazzes().entrySet()) {
         final ChangedEntity callee = dependency.getKey();
         for (final Entry<ChangedEntity, Set<String>> testcase : dependency.getValue().getTestcases().entrySet()) {
            for (final String testMethod : testcase.getValue()) {
               final Map<ChangedEntity, Set<String>> calledClasses = new HashMap<>();
               final Set<String> methods = new HashSet<>();
               methods.add(callee.getMethod());
               calledClasses.put(new ChangedEntity(callee.getClazz(), callee.getModule()), methods);
               final ChangedEntity testClazz = testcase.getKey();
               dependencyManager.addDependencies(new ChangedEntity(testClazz.getClazz(), testClazz.getModule(), testMethod), calledClasses);
            }
         }
      }
   }

   private void fillInitialTestDependencies() {
      for (final Entry<ChangedEntity, InitialDependency> dependency : dependencyResult.getInitialversion().getInitialDependencies().entrySet()) {
         for (final ChangedEntity dependentClass : dependency.getValue().getEntities()) {
            final Map<ChangedEntity, Set<String>> dependents = dependencyMap.getOrAddDependenciesForTest(dependency.getKey());
            final ChangedEntity dependencyEntity = new ChangedEntity(dependentClass.getClazz(), dependentClass.getModule());
            Set<String> methods = dependents.get(dependencyEntity);
            if (methods == null) {
               methods = new HashSet<>();
               dependents.put(dependencyEntity, methods);
            }
            methods.add(dependentClass.getMethod());
         }
      }
   }
   
   private void checkCorrectness() {
      for (final Entry<ChangedEntity, CalledMethods> entry : dependencyMap.getDependencyMap().entrySet()) {
         if (entry.getKey().getModule() == null) {
            throw new RuntimeException("Entry " + entry.getKey() + " has null module!");
         }
      }
   }
}
