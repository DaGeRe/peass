package de.peran;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependencyprocessors.DependencyTester;
import de.peran.dependencyprocessors.PairProcessor;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.utils.OptionConstants;
import de.peran.utils.TestLoadUtil;

/**
 * Runs the dependency test by running the test, where something could have changed, pairwise for every new version. This makes it faster to get potential change candidates, but it
 * takes longer for a whole project.
 * 
 * @author reichelt
 *
 */
public class DependencyTestPairStarter extends PairProcessor {

   private static final Logger LOG = LogManager.getLogger(DependencyTestPairStarter.class);

   protected final DependencyTester tester;
   private final List<String> versions = new LinkedList<>();
   private final int startindex, endindex;
   private final ChangedTraceTests changedTests;
   private final TestCase test;

   public DependencyTestPairStarter(final String[] args) throws ParseException, JAXBException, IOException {
      super(args);
      final int vms = Integer.parseInt(line.getOptionValue(OptionConstants.VMS.getName(), "15"));
      final int repetitions = Integer.parseInt(line.getOptionValue(OptionConstants.REPETITIONS.getName(), "1"));
      final boolean useKieker = Boolean.parseBoolean(line.getOptionValue(OptionConstants.USEKIEKER.getName(), "false"));

      if (line.hasOption(OptionConstants.DURATION.getName())) {
         final int duration = Integer.parseInt(line.getOptionValue(OptionConstants.DURATION.getName()));
         tester = new DependencyTester(folders, duration, vms, true, repetitions, useKieker);
      } else {
         final int warmup = Integer.parseInt(line.getOptionValue(OptionConstants.WARMUP.getName(), "10"));
         final int iterationen = Integer.parseInt(line.getOptionValue(OptionConstants.ITERATIONS.getName(), "10"));
         tester = new DependencyTester(folders, warmup, iterationen, vms, true, repetitions, useKieker);
      }

      if (line.hasOption(OptionConstants.TEST.getName())) {
         test = new TestCase(line.getOptionValue(OptionConstants.TEST.getName()));
      } else {
         test = null;
      }

      this.changedTests = TestLoadUtil.loadChangedTests(line);

      versions.add(dependencies.getInitialversion().getVersion());

      dependencies.getVersions().getVersion().forEach(version -> versions.add(version.getVersion()));

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
                  for (final Version ticVersion : dependencies.getVersions().getVersion()) {
                     if (ticVersion.getVersion().equals(sicVersion)) {
                        potentialStart = ticVersion.getVersion();
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
                  for (final Version ticVersion : dependencies.getVersions().getVersion()) {
                     if (next) {
                        potentialStart = ticVersion.getVersion();
                        break;
                     }
                     if (ticVersion.getVersion().equals(sicVersion)) {
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
   protected void processVersion(final Version versioninfo) {
      try {
         final int currentIndex = versions.indexOf(versioninfo.getVersion());
         final boolean executeThisVersion = currentIndex >= startindex && currentIndex <= endindex;

         final String version = versioninfo.getVersion();
         LOG.info("Bearbeite {} Mit Tests: {}", version, executeThisVersion);

         final Set<TestCase> testcases = findTestcases(versioninfo);

         for (final TestCase testcase : testcases) {
            boolean executeThisTest = true;
            if (test != null) {
               if (!test.equals(testcase)) {
                  executeThisTest = false;
               }
            }
            if (executeThisTest && executeThisVersion && lastTestcaseCalls.containsKey(testcase)) {
               if (changedTests != null) {
                  final TestSet calls = changedTests.getVersions().get(version);
                  boolean hasChanges = false;
                  if (calls != null) {
                     for (final Map.Entry<ChangedEntity, List<String>> clazzCalls : calls.entrySet()) {
                        final String changedClazz = clazzCalls.getKey().getJavaClazzName();
                        if (changedClazz.equals(testcase.getClazz()) && clazzCalls.getValue().contains(testcase.getMethod())) {
                           hasChanges = true;
                        }
                     }
                  }
                  if (hasChanges) {
                     final String versionOld = lastTestcaseCalls.get(testcase);
                     tester.evaluate(version, versionOld, testcase);
                  } else {
                     LOG.debug("Skipping " + testcase + " because of execution-JSON in " + versioninfo.getVersion());
                  }
               } else {
                  final String versionOld = lastTestcaseCalls.get(testcase);
                  tester.evaluate(version, versionOld, testcase);
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
