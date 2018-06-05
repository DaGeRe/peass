package de.peran;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestSet;

/**
 * Executes test and skips those where results clearly indicate a performance change
 * 
 * @author reichelt
 *
 */
public class AdaptiveTestStarter extends DependencyTestPairStarter {

   private static final Logger LOG = LogManager.getLogger(AdaptiveTestStarter.class);

   public AdaptiveTestStarter(final String[] args) throws ParseException, JAXBException, IOException {
      super(args);
   }

   public static void main(final String[] args) throws ParseException, JAXBException, IOException {
      final AdaptiveTestStarter starter = new AdaptiveTestStarter(args);
      starter.processCommandline();
   }
   
   //TODO: Should be method of tester instead of TestStarter -> is not used currently
   protected void executeCompareTests(final String version, final String versionOld, final TestCase testcase) throws IOException, InterruptedException, JAXBException {
      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in versions {} and {}", versionOld, version);

      File logFile = new File(folders.getLogFolder(), version);
      if (logFile.exists()) {
         logFile = new File(folders.getLogFolder(), version + "_new");
      }
      logFile.mkdir();

      final TestSet testset = new TestSet();
      testset.addTest(testcase);
      for (int vmid = 0; vmid < tester.getVMCount(); vmid++) {
         tester.evaluateOnce(testset, versionOld, vmid, logFile);
         tester.evaluateOnce(testset, version, vmid, logFile);

         if (vmid > 10) {
            boolean savelyDecidable = false;

            final File kopemeFile = new File(folders.getFullMeasurementFolder(), testcase.getMethod() + ".xml");
            final XMLDataLoader loader = new XMLDataLoader(kopemeFile);
            final List<Double> before = new LinkedList<>();
            final List<Double> after = new LinkedList<>();
            for (final Result result : loader.getFullData().getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult()) {
               if (result.getVersion().getGitversion().equals(versionOld)) {
                  before.add(result.getValue());
               }
               if (result.getVersion().getGitversion().equals(version)) {
                  after.add(result.getValue());
               }
            }
            final double tvalue = TestUtils.t(ArrayUtils.toPrimitive(before.toArray(new Double[0])), ArrayUtils.toPrimitive(after.toArray(new Double[0])));
            if (Math.abs(tvalue) > 10 || Math.abs(tvalue) < 0.01) {
               LOG.info("In vm iteration {}, t-value was {} - skipping rest of vm executions.", vmid, tvalue);
               savelyDecidable = true;
            }

            if (savelyDecidable) {
               break;
            }
         }
      }
   }

}
