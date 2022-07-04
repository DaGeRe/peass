package de.dagere.peass.dependency.reader;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.data.CalledMethods;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestDependencies;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.InitialCallList;
import de.dagere.peass.dependency.persistence.InitialCommit;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.vcs.VersionIterator;

/**
 * Identifies the first commit that is analyzable by Peass (i.e. runnable with current JDK, Maven and dependencies) 
 *
 */
public class InitialCommitReader {
   
   private static final Logger LOG = LogManager.getLogger(InitialCommitReader.class);
   
   protected final StaticTestSelection dependencyResult;
   protected DependencyManager dependencyManager;
   protected VersionIterator iterator;
   protected TestDependencies dependencyMap;
   
   
   public InitialCommitReader(final StaticTestSelection dependencyResult, final DependencyManager dependencyManager, final VersionIterator iterator) {
      this.dependencyResult = dependencyResult;
      this.dependencyManager = dependencyManager;
      this.iterator = iterator;
      dependencyMap = dependencyManager.getDependencyMap();
   }
   
   public boolean readInitialCommit() throws IOException, InterruptedException, XmlPullParserException {
      if (!dependencyManager.initialyGetTraces(iterator.getTag())) {
         return false;
      }
      final InitialCommit initialversion = createInitialVersion();
      dependencyResult.setInitialcommit(initialversion);
      return true;
   }

   private InitialCommit createInitialVersion() {
      int jdkversion = dependencyManager.getExecutor().getJDKVersion();
      final InitialCommit initialversion = new InitialCommit();
      initialversion.setCommit(iterator.getTag());
      initialversion.setJdk(jdkversion);
      LOG.debug("Starting writing: {}", dependencyMap.getDependencyMap().size());
      for (final Entry<TestCase, CalledMethods> dependencyEntry : dependencyMap.getDependencyMap().entrySet()) {
         final TestCase testcase = dependencyEntry.getKey();
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
   
   public void readCompletedVersions(CommitComparatorInstance comparator) {
      fillInitialTestDependencies();
      checkCorrectness();

      final InitialCommit initialversion = createInitialVersion();
      dependencyResult.setInitialcommit(initialversion);

      if (dependencyResult.getVersions().size() > 0) {
         for (final Map.Entry<String, VersionStaticSelection> version : dependencyResult.getVersions().entrySet()) {
            String tag = version.getKey();
            String startTag = iterator.getTag();
            if (comparator.isBefore(tag, startTag) || tag.equals(startTag)) {
               addVersionTestDependencies(version.getValue());
            }
         }
      }
      checkCorrectness();

      LOG.debug("Analyzing {} commits", iterator.getRemainingSize());
   }

   private void addVersionTestDependencies(final VersionStaticSelection version) {
      for (final Entry<ChangedEntity, TestSet> dependency : version.getChangedClazzes().entrySet()) {
         final ChangedEntity callee = dependency.getKey();
         for (final Entry<TestCase, Set<String>> testcase : dependency.getValue().getTestcases().entrySet()) {
            for (final String testMethod : testcase.getValue()) {
               final Map<ChangedEntity, Set<String>> calledClasses = new HashMap<>();
               final Set<String> methods = new HashSet<>();
               methods.add(callee.getMethod());
               calledClasses.put(new ChangedEntity(callee.getClazz(), callee.getModule()), methods);
               final TestCase testClazz = testcase.getKey();
               TestCase test = new TestCase(testClazz.getClazz(), testMethod, testClazz.getModule());
               dependencyManager.addDependencies(test, calledClasses);
            }
         }
      }
   }

   private void fillInitialTestDependencies() {
      for (final Entry<TestCase, InitialCallList> dependency : dependencyResult.getInitialcommit().getInitialDependencies().entrySet()) {
         for (final ChangedEntity dependentClass : dependency.getValue().getEntities()) {
            TestCase testClassName = dependency.getKey();
            addDependencies(testClassName, dependentClass);
         }
      }
   }

   private void addDependencies(final TestCase testClassName , final ChangedEntity dependentClass) {
      final Map<ChangedEntity, Set<String>> testDependencies = dependencyMap.getOrAddDependenciesForTest(testClassName);
      final ChangedEntity dependencyEntity = new ChangedEntity(dependentClass.getClazz(), dependentClass.getModule());
      Set<String> methods = testDependencies.get(dependencyEntity);
      if (methods == null) {
         methods = new HashSet<>();
         testDependencies.put(dependencyEntity, methods);
      }
      String method = dependentClass.getMethod() + dependentClass.getParameterString();
      methods.add(method);
   }
   
   private void checkCorrectness() {
      for (final Entry<TestCase, CalledMethods> entry : dependencyMap.getDependencyMap().entrySet()) {
         if (entry.getKey().getModule() == null) {
            throw new RuntimeException("Entry " + entry.getKey() + " has null module!");
         }
      }
   }
}
