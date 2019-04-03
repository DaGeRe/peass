package de.peass;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.DependencyTester;
import de.peass.dependencyprocessors.PairProcessor;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.OptionConstants;
import de.peass.utils.TestLoadUtil;

/**
 * Runs the dependency test by running the test, where something could have changed, pairwise for every new version. This makes it faster to get potential change candidates, but it
 * takes longer for a whole project.
 * 
 * @author reichelt
 *
 */
public class DependencyTestPairStarter extends PairProcessor {
   
   static JUnitTestTransformer getTestTransformer(final CommandLine line, final PeASSFolders folders) {
      final int repetitions = Integer.parseInt(line.getOptionValue(OptionConstants.REPETITIONS.getName(), "1"));
      final boolean useKieker = Boolean.parseBoolean(line.getOptionValue(OptionConstants.USEKIEKER.getName(), "false"));
      final int warmup = Integer.parseInt(line.getOptionValue(OptionConstants.WARMUP.getName(), "10"));
      final int iterations = Integer.parseInt(line.getOptionValue(OptionConstants.ITERATIONS.getName(), "10"));
      final JUnitTestTransformer testgenerator = new JUnitTestTransformer(folders.getProjectFolder());
      testgenerator.setDatacollectorlist(DataCollectorList.ONLYTIME);
      testgenerator.setIterations(iterations);
      testgenerator.setLogFullData(true);
      testgenerator.setWarmupExecutions(warmup);
      testgenerator.setUseKieker(useKieker);
      testgenerator.setRepetitions(repetitions);
      if (line.hasOption(OptionConstants.TIMEOUT.getName())) {
         final long timeout = Long.parseLong(line.getOptionValue(OptionConstants.TIMEOUT.getName()));
         testgenerator.setSumTime(timeout);
      }
      return testgenerator;
   }

   private static final Logger LOG = LogManager.getLogger(DependencyTestPairStarter.class);

   protected DependencyTester tester;
   private final List<String> versions = new LinkedList<>();
   private final int startindex, endindex;
   private final ExecutionData changedTests;
   private TestCase test;

   public DependencyTestPairStarter(final String[] args) throws ParseException, JAXBException, IOException {
      super(args);
      final int vms = Integer.parseInt(line.getOptionValue(OptionConstants.VMS.getName(), "15"));
      final int repetitions = Integer.parseInt(line.getOptionValue(OptionConstants.REPETITIONS.getName(), "1"));
      final boolean useKieker = Boolean.parseBoolean(line.getOptionValue(OptionConstants.USEKIEKER.getName(), "false"));

      if (line.hasOption(OptionConstants.DURATION.getName())) {
         final int duration = Integer.parseInt(line.getOptionValue(OptionConstants.DURATION.getName()));
         tester = new DependencyTester(folders, duration, vms, true, repetitions, useKieker);
      } else {
         final JUnitTestTransformer testgenerator = getTestTransformer(line, folders);
         tester = new DependencyTester(folders, true, testgenerator, vms);
      }
      
      if (line.hasOption(OptionConstants.TEST.getName())) {
         test = new TestCase(line.getOptionValue(OptionConstants.TEST.getName()));
      } else {
         test = null;
      }
      LOG.info("Testcase: " + test);
      
      this.changedTests = TestLoadUtil.loadChangedTests(line);

      versions.add(dependencies.getInitialversion().getVersion());

      dependencies.getVersions().keySet().forEach(version -> versions.add(version));

      startindex = getStartVersionIndex();
      endindex = getEndVersion();

   }

   /**
    * Calculates the index of the start version
    * 
    * @return index of the start version
    */
   private int getStartVersionIndex() {
      int currentStartindex = startversion != null ? versions.indexOf(startversion) : 0;
      // Only bugfix if dependencyfile and executefile do not fully match
      if (changedTests != null) {
         if (startversion != null && currentStartindex == -1) {
            String potentialStart = "";
            if (changedTests.getVersions().containsKey(startversion)) {
               for (final String sicVersion : changedTests.getVersions().keySet()) {
                  for (final String ticVersion : dependencies.getVersions().keySet()) {
                     if (ticVersion.equals(sicVersion)) {
                        potentialStart = ticVersion;
                        break;
                     }
                  }
                  if (sicVersion.equals(startversion)) {
                     break;
                  }
               }
            }
            LOG.debug("Version only in executefile, next version in dependencyfile: {}", potentialStart);
            currentStartindex = versions.indexOf(potentialStart);
         }
      }
      return currentStartindex;
   }

   /**
    * Calculates the index of the end version.
    * 
    * @return index of the end version
    */
   private int getEndVersion() {
      int currentEndindex = endversion != null ? versions.indexOf(endversion) : versions.size();
      // Only bugfix if dependencyfile and executefile do not fully match
      if (changedTests != null) {
         if (endversion != null && currentEndindex == -1) {
            String potentialStart = "";
            if (changedTests.getVersions().containsKey(endversion)) {
               for (final String sicVersion : changedTests.getVersions().keySet()) {
                  boolean next = false;
                  for (final String ticVersion : dependencies.getVersions().keySet()) {
                     if (next) {
                        potentialStart = ticVersion;
                        break;
                     }
                     if (ticVersion.equals(sicVersion)) {
                        next = true;
                     }
                  }
                  if (sicVersion.equals(endversion)) {
                     break;
                  }
               }
            }
            LOG.debug("Version only in executefile, next version in dependencyfile: {}", potentialStart);
            currentEndindex = versions.indexOf(potentialStart);
         }
      }
      return currentEndindex;
   }

   @Override
   protected void processVersion(final String version, final Version versioninfo) {
      try {
         final int currentIndex = versions.indexOf(version);
         final boolean executeThisVersion = currentIndex >= startindex && currentIndex <= endindex;

         LOG.trace("Processing Version {} Executing Tests: {}", version, executeThisVersion);

         final Set<TestCase> testcases = versioninfo.getTests().getTests();
         final String versionOld = versioninfo.getPredecessor();

         for (final TestCase testcase : testcases) {
            if (executeThisVersion) {
               if (lastTestcaseCalls.containsKey(testcase)) {
                  boolean executeThisTest = true;
                  if (test != null) {
                     LOG.debug("Checking " + test + " " + testcase);
                     if (!test.equals(testcase)) {
                        executeThisTest = false;
                        LOG.debug("Skipping: " + testcase);
                     }else {
                        LOG.debug("Success!");
                     }
                  }
                 
                  if (executeThisTest) {
                     if (changedTests != null) {
                        final TestSet calls = changedTests.getVersions().get(version);
                        boolean hasChanges = false;
                        if (calls != null) {
                           for (final Map.Entry<ChangedEntity, Set<String>> clazzCalls : calls.entrySet()) {
                              final String changedClazz = clazzCalls.getKey().getJavaClazzName();
                              if (changedClazz.equals(testcase.getClazz()) && clazzCalls.getValue().contains(testcase.getMethod())) {
                                 hasChanges = true;
                              }
                           }
                        }
                        if (hasChanges) {
//                           final String versionOld = lastTestcaseCalls.get(testcase);
                           tester.evaluate(version, versionOld, testcase);
                        } else {
                           LOG.debug("Skipping " + testcase + " because of execution-JSON in " + version);
                        }
                     } else {
//                        final String versionOld = lastTestcaseCalls.get(testcase);
                        tester.evaluate(version, versionOld, testcase);
                        tester.postEvaluate();
                     }
                  }
               }
            }
            lastTestcaseCalls.put(testcase, version);
         }
      } catch (IOException | InterruptedException | JAXBException e) {
         e.printStackTrace();
      }
   }

   public static void main(final String[] args) throws ParseException, JAXBException, IOException {
      final DependencyTestPairStarter starter = new DependencyTestPairStarter(args);
      starter.processCommandline();
   }

}
