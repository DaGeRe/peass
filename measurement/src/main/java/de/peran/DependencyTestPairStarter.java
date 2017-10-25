package de.peran;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.peran.dependency.PeASSFolderUtil;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependencyprocessors.DependencyTester;
import de.peran.dependencyprocessors.PairProcessor;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.utils.OptionConstants;

/**
 * Runs the dependency test by running the test, where something could have
 * changed, pairwise for every new version. This makes it faster to get
 * potential change candidates, but it takes longer for a whole project.
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

	public DependencyTestPairStarter(final String args[]) throws ParseException, JAXBException, IOException {
		super(args);
		final int vms = Integer.parseInt(line.getOptionValue(OptionConstants.VMS.getName(), "15"));
		final int repetitions = Integer.parseInt(line.getOptionValue(OptionConstants.REPETITIONS.getName(), "1"));
		if (line.hasOption(OptionConstants.EXECUTIONFILE.getName())) {
			final ObjectMapper mapper = new ObjectMapper();
			ChangedTraceTests testsTemp;
			try {
				testsTemp = mapper.readValue(new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName())), ChangedTraceTests.class);
			} catch (JsonMappingException e) {
				ObjectMapper objectMapper = new ObjectMapper();
				SimpleModule module = new SimpleModule();
				module.addDeserializer(ChangedTraceTests.class, new ChangedTraceTests.Deserializer());
				objectMapper.registerModule(module);
				testsTemp = objectMapper.readValue(new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName())), ChangedTraceTests.class);
			}
			changedTests = testsTemp;
		} else {
			changedTests = null;
		}

		if (line.hasOption(OptionConstants.DURATION.getName())) {
			int duration = Integer.parseInt(line.getOptionValue(OptionConstants.DURATION.getName()));
			if (dependencies.getModule() != null){
				File moduleFolder = new File(projectFolder, dependencies.getModule());
				tester = new DependencyTester(projectFolder, moduleFolder, duration, vms, true, repetitions);
			} else {
				tester = new DependencyTester(projectFolder, projectFolder, duration, vms, true, repetitions);
			}

		} else {
			final int warmup = Integer.parseInt(line.getOptionValue(OptionConstants.WARMUP.getName()), 10);
			final int iterationen = Integer.parseInt(line.getOptionValue(OptionConstants.ITERATIONS.getName()), 10);
			tester = new DependencyTester(projectFolder, warmup, iterationen, vms, true, repetitions);
		}

		versions.add(dependencies.getInitialversion().getVersion());

		dependencies.getVersions().getVersion().forEach(version -> versions.add(version.getVersion()));

		startindex = getStartVersion();
		endindex = getEndVersion();
	}

	private int getStartVersion() {
		int startindex = startversion != null ? versions.indexOf(startversion) : 0;
		// Only bugfix if dependencyfile and executefile do not fully match
		if (changedTests != null) {
			if (startversion != null && startindex == -1) {
				String potentialStart = "";
				if (changedTests.getVersions().containsKey(startversion)) {
					for (String sicVersion : changedTests.getVersions().keySet()) {
						for (Version ticVersion : dependencies.getVersions().getVersion()) {
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
				startindex = versions.indexOf(potentialStart);
			}
		}
		return startindex;
	}

	private int getEndVersion() {
		int endindex = endversion != null ? versions.indexOf(endversion) : versions.size();
		// Only bugfix if dependencyfile and executefile do not fully match
		if (changedTests != null) {
			if (endversion != null && endindex == -1) {
				String potentialStart = "";
				if (changedTests.getVersions().containsKey(endversion)) {
					for (String sicVersion : changedTests.getVersions().keySet()) {
						boolean next = false;
						for (Version ticVersion : dependencies.getVersions().getVersion()) {
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
				endindex = versions.indexOf(potentialStart);
			}
		}
		return endindex;
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
				if (executeThisVersion && lastTestcaseCalls.containsKey(testcase)) {
					if (changedTests != null) {
						final TestSet calls = changedTests.getVersions().get(version);
						boolean hasChanges = false;
						if (calls != null) {
							for (final Map.Entry<String, List<String>> clazzCalls : calls.entrySet()) {
								if (clazzCalls.getKey().equals(testcase.getClazz()) && clazzCalls.getValue().contains(testcase.getMethod())) {
									hasChanges = true;
								}
							}
						}
						if (hasChanges) {
							final String versionOld = lastTestcaseCalls.get(testcase);
							executeCompareTests(version, versionOld, testcase);
						} else {
							LOG.debug("Skipping " + testcase + " because of execution-JSON in " + versioninfo.getVersion());
						}
					} else {
						final String versionOld = lastTestcaseCalls.get(testcase);
						executeCompareTests(version, versionOld, testcase);
					}
				}
				lastTestcaseCalls.put(testcase, version);
			}
		} catch (IOException | InterruptedException | JAXBException e) {
			e.printStackTrace();
		}
	}

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
		}
	}

	public static void main(final String[] args) throws ParseException, JAXBException, IOException {
		final DependencyTestPairStarter starter = new DependencyTestPairStarter(args);
		starter.processCommandline();
	}

}
