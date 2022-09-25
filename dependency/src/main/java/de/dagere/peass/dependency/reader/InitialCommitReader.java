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
import de.dagere.peass.dependency.analysis.testData.TestClazzCall;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.InitialCallList;
import de.dagere.peass.dependency.persistence.InitialCommit;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.vcs.CommitIterator;

/**
 * Identifies the first commit that is analyzable by Peass (i.e. runnable with current JDK, Maven and dependencies) 
 *
 */
public class InitialCommitReader {
   
   private static final Logger LOG = LogManager.getLogger(InitialCommitReader.class);
   
   protected final StaticTestSelection dependencyResult;
   protected DependencyManager dependencyManager;
   protected CommitIterator iterator;
   protected TestDependencies dependencyMap;
   
   
   public InitialCommitReader(final StaticTestSelection dependencyResult, final DependencyManager dependencyManager, final CommitIterator iterator) {
      this.dependencyResult = dependencyResult;
      this.dependencyManager = dependencyManager;
      this.iterator = iterator;
      dependencyMap = dependencyManager.getDependencyMap();
   }
   
   public boolean readInitialCommit() throws IOException, InterruptedException, XmlPullParserException {
      if (!dependencyManager.initialyGetTraces(iterator.getCommitName())) {
         return false;
      }
      final InitialCommit initialcommit = createInitialCommit();
      dependencyResult.setInitialcommit(initialcommit);
      return true;
   }

   private InitialCommit createInitialCommit() {
      int jdkversion = dependencyManager.getExecutor().getJDKVersion();
      final InitialCommit initialversion = new InitialCommit();
      initialversion.setCommit(iterator.getCommitName());
      initialversion.setJdk(jdkversion);
      LOG.debug("Starting writing: {}", dependencyMap.getDependencyMap().size());
      for (final Entry<TestMethodCall, CalledMethods> dependencyEntry : dependencyMap.getDependencyMap().entrySet()) {
         final TestMethodCall testcase = dependencyEntry.getKey();
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

      final InitialCommit initialversion = createInitialCommit();
      dependencyResult.setInitialcommit(initialversion);

      if (dependencyResult.getCommits().size() > 0) {
         for (final Map.Entry<String, CommitStaticSelection> version : dependencyResult.getCommits().entrySet()) {
            String tag = version.getKey();
            String startTag = iterator.getCommitName();
            if (comparator.isBefore(tag, startTag) || tag.equals(startTag)) {
               addCommitTestDependencies(version.getValue());
            }
         }
      }
      checkCorrectness();

      LOG.debug("Analyzing {} commits", iterator.getRemainingSize());
   }

   private void addCommitTestDependencies(final CommitStaticSelection commitStaticSelection) {
      // changedClazzes will be null if the commit has no changes
      if (commitStaticSelection.getChangedClazzes() != null) {
         for (final Entry<ChangedEntity, TestSet> dependency : commitStaticSelection.getChangedClazzes().entrySet()) {
            final ChangedEntity callee = dependency.getKey();
            for (final Entry<TestClazzCall, Set<String>> testcase : dependency.getValue().getTestcases().entrySet()) {
               for (final String testMethod : testcase.getValue()) {
                  final Map<ChangedEntity, Set<String>> calledClasses = new HashMap<>();
                  final Set<String> methods = new HashSet<>();
                  methods.add(callee.getMethod());
                  calledClasses.put(new ChangedEntity(callee.getClazz(), callee.getModule()), methods);
                  final TestCase testClazz = testcase.getKey();
                  TestMethodCall test = new TestMethodCall(testClazz.getClazz(), testMethod, testClazz.getModule());
                  dependencyManager.addDependencies(test, calledClasses);
               }
            }
         }
      }
   }

   private void fillInitialTestDependencies() {
      for (final Entry<TestMethodCall, InitialCallList> dependency : dependencyResult.getInitialcommit().getInitialDependencies().entrySet()) {
         for (final ChangedEntity dependentClass : dependency.getValue().getEntities()) {
            TestMethodCall testName = dependency.getKey();
            addDependencies(testName, dependentClass);
         }
      }
   }

   private void addDependencies(final TestMethodCall testName , final ChangedEntity dependentClass) {
      final Map<ChangedEntity, Set<String>> testDependencies = dependencyMap.getOrAddDependenciesForTest(testName);
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
      for (final Entry<TestMethodCall, CalledMethods> entry : dependencyMap.getDependencyMap().entrySet()) {
         if (entry.getKey().getModule() == null) {
            throw new RuntimeException("Entry " + entry.getKey() + " has null module!");
         }
      }
   }
}
