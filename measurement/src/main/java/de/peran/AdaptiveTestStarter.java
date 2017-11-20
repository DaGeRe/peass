package de.peran;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.peran.dependency.PeASSFolderUtil;
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

	@Override
	protected void executeCompareTests(final String version, final String versionOld, final TestCase testcase) throws IOException, InterruptedException, JAXBException {
		LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in versions {} and {}", versionOld, version);

		File logFile = new File(PeASSFolderUtil.getLogFolder(), version);
		if (logFile.exists()) {
			logFile = new File(PeASSFolderUtil.getLogFolder(), version + "_new");
		}
		logFile.mkdir();
		
		final TestSet testset = new TestSet();
		testset.addTest(testcase.getClazz(), testcase.getMethod());
		for (int vmid = 0; vmid < tester.getVMCount(); vmid++) {
			tester.evaluateOnce(testset, versionOld, vmid, logFile);
			tester.evaluateOnce(testset, version, vmid, logFile);

			if (vmid > 2) {
				boolean unequal = false;

				for (final Entry<String, List<String>> entry : testset.entrySet()) {
					for (final String method : entry.getValue()) {
						final File kopemeFile = new File(PeASSFolderUtil.getFullMeasurementFolder(), method + ".xml");
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
						boolean change = TestUtils.tTest(ArrayUtils.toPrimitive(before.toArray(new Double[0])), ArrayUtils.toPrimitive(after.toArray(new Double[0])), 0.05);
						unequal = !change;
					}
				}

				if (unequal) {
					LOG.info("In vm iteration {}, all test results seemed to be equal - skipping rest of vm executions.");
					break;
				}
			}
		}
	}


}
