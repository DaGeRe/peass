package de.dagere.peass.measurement.analysis.statistics;

import java.io.File;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.VersionComparator;

/**
 * Saves the measurement data of one testclass in every version and every run.
 * 
 * @author reichelt
 *
 */
public class TestData {
   
   public static Comparator<String> comparator = VersionComparator.INSTANCE;

	private static final Logger LOG = LogManager.getLogger(TestData.class);

	private final TestCase testcase;
	private final File origin;

	private final SortedMap<String, EvaluationPair> data = new TreeMap<>(comparator);

	public TestData(final TestCase testcase, final File origin) {
		this.testcase = testcase;
		this.origin = origin;
	}
	
	public void addMeasurement(final String versionOfPair, final String currentVersion, final String predecessor, final Kopemedata resultData) {
	   LOG.trace("Pair-Version: {} Class: {} Method: {}", versionOfPair, testcase.getClazz(), testcase.getMethod());
      EvaluationPair currentPair = data.get(versionOfPair);
      // LOG.debug(currentPair);
      if (currentPair == null) {
//         final String predecessor = VersionComparator.getPreviousVersionForTestcase(testcase, versionOfPair);
         LOG.debug("Version: {} Predecessor: {}", versionOfPair, predecessor);
         // TODO Workaround if data are incomplete, e.g. because of build error
         if (versionOfPair != null){
            currentPair = new EvaluationPair(versionOfPair, predecessor, new TestCase(resultData.getTestcases()));
            data.put(versionOfPair, currentPair);
         }
      } 

      if (currentPair != null){
         final Result result = resultData.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult().get(0);
         if (versionOfPair.equals(currentVersion)) {
            currentPair.getCurrent().add(result);
         } else {
            currentPair.getPrevius().add(result);
         }
      }
	}

	public SortedMap<String, EvaluationPair> getMeasurements() {
		return data;
	}
	
	public String getTestClass() {
		return testcase.getClazz();
	}

	public String getTestMethod() {
		return testcase.getMethod();
	}
	
	public int getVersions(){
		return data.size();
	}

   public TestCase getTestCase() {
      return testcase;
   }

   public File getOrigin() {
      return origin;
   }

}
