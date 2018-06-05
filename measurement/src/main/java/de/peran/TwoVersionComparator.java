package de.peran;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.dependency.PeASSFolders;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependencyprocessors.DependencyTester;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;
import de.peran.statistics.DependencyStatisticAnalyzer;
import de.peran.utils.OptionConstants;

/**
 * Compares the performance of two versions.
 * 
 * @author reichelt
 *
 */
public class TwoVersionComparator {

	private static final Logger LOG = LogManager.getLogger(TwoVersionComparator.class);

	public static final String TEST = "test";

	public static void main(final String args[]) throws JAXBException, ParseException, IOException, InterruptedException {

		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.ITERATIONS,
				OptionConstants.WARMUP, OptionConstants.DEPENDENCYFILE, OptionConstants.REPETITIONS, OptionConstants.VMS);

		final Option test = new Option(TEST, "test", true, "testcase for analysis");
		test.setRequired(true);
		options.addOption(test);

		final CommandLineParser clp = new DefaultParser();

		final CommandLine line = clp.parse(options, args);

		final int vms = Integer.parseInt(line.getOptionValue(OptionConstants.VMS.getName(), "10"));
		final String startrevision = line.getOptionValue(OptionConstants.STARTVERSION.getName());
		final String endrevision = line.getOptionValue(OptionConstants.ENDVERSION.getName());
		final int repetitions = Integer.parseInt(line.getOptionValue(OptionConstants.REPETITIONS.getName(), "150"));

		final int iterations = Integer.parseInt(line.getOptionValue(OptionConstants.ITERATIONS.getName(), "3000"));
		final int warmup = Integer.parseInt(line.getOptionValue(OptionConstants.WARMUP.getName(), "3000"));
		final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
		final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
		if (!projectFolder.exists()) {
			System.out.println("Projektordner " + projectFolder + " existiert nicht.");
			System.exit(1);
		}
		final DependencyTester tester = new DependencyTester(new PeASSFolders(projectFolder), warmup, iterations, vms, false, repetitions, false);
		final Versiondependencies versiondependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
		final List<Version> allVersions = versiondependencies.getVersions().getVersion();
		Version v1 = null, v2 = null;
		// if (versiondependencies.getInitialversion().getVersion().equals(startrevision)){
		// v1 = versiondependencies.getInitialversion();
		// }
		for (final Iterator<Version> iterator = allVersions.iterator(); iterator.hasNext();) {
			final Version version = iterator.next();
			final String currentVersion = version.getVersion();
			LOG.debug(currentVersion);
			if (currentVersion.equals(startrevision)) {
				v1 = version;
			}
			if (currentVersion.equals(endrevision)) {
				v2 = version;
			}
		}
		if (v1 == null || v2 == null) {
			LOG.error("Nicht gefunden: {} {} bzw. {} {}", startrevision, v1, endrevision, v2);
			// System.exit(1);
		}

		if (line.hasOption(TEST)) {
			final String testName = line.getOptionValue(TEST);
			// removeAllNonMatchingTests(v1, testName);
			final TestCase selectedTest = new TestCase(testName);
			removeAllNonMatchingTests(v2, selectedTest);
			// if (v2.getDependency())
		}

		tester.compareVersions(startrevision, v2);
	}

	private static void removeAllNonMatchingTests(final Version v1, final TestCase selectedTest) {
		// if (testName)
		for (final Dependency dep : v1.getDependency()) {
			for (final Iterator<Testcase> it = dep.getTestcase().iterator(); it.hasNext();) {
				final Testcase testcase = it.next();
				final String name = testcase.getClazz();
				if (!name.startsWith(selectedTest.getClazz())) {
					it.remove();
				} else if (selectedTest.getMethod() != null) {
					for (final Iterator<String> methodIterator = testcase.getMethod().iterator(); methodIterator.hasNext();) {
						final String method = methodIterator.next();
						if (!method.equals(selectedTest.getMethod())) {
							methodIterator.remove();
						}
					}
				}
			}
		}

		int testcount = 0;
		for (final Dependency dep : v1.getDependency()) {
			for (final Iterator<Testcase> it = dep.getTestcase().iterator(); it.hasNext();) {
				LOG.debug("Test: " + it.next());
				testcount++;
			}
		}
		if (testcount == 0) {
			throw new RuntimeException("No test selected - maybe test is not matching because full package name is missing?");
		}
	}

}
