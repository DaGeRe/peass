package de.dagere.peass.measurement.statistics.data;

import java.io.File;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;

/**
 * Saves the measurement data of one testclass in every version and every run.
 * 
 * @author reichelt
 *
 */
public class TestData {
   
	private static final Logger LOG = LogManager.getLogger(TestData.class);

	private final TestMethodCall testcase;
	private final File origin;

	private final SortedMap<String, EvaluationPair> data;

	public TestData(final TestMethodCall testcase, final File origin, CommitComparatorInstance comparator) {
		this.testcase = testcase;
		this.origin = origin;
		data = new TreeMap<>(comparator);
	}
	
	public void addMeasurement(final String commitOfPair, final String currentCommit, final String predecessor, final Kopemedata resultData) {
	   LOG.trace("Pair-Version: {} Class: {} Method: {}", commitOfPair, testcase.getClazz(), testcase.getMethod());
      EvaluationPair currentPair = data.get(commitOfPair);
      // LOG.debug(currentPair);
      if (currentPair == null) {
//         final String predecessor = VersionComparator.getPreviousVersionForTestcase(testcase, versionOfPair);
         LOG.debug("Version: {} Predecessor: {}", commitOfPair, predecessor);
         // TODO Workaround if data are incomplete, e.g. because of build error
         if (commitOfPair != null){
            currentPair = new EvaluationPair(commitOfPair, predecessor, new TestMethodCall(resultData));
            data.put(commitOfPair, currentPair);
         }
      } 

      if (currentPair != null){
         final VMResult result = resultData.getFirstResult();
         if (commitOfPair.equals(currentCommit)) {
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
		return testcase.getMethodWithParams();
	}
	
	public int getVersions(){
		return data.size();
	}

   public TestMethodCall getTestCase() {
      return testcase;
   }

   public File getOrigin() {
      return origin;
   }

}
