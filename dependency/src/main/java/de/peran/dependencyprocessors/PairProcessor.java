package de.peran.dependencyprocessors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;

import de.peran.dependency.analysis.data.TestCase;
import de.peran.generated.Versiondependencies.Initialversion;
import de.peran.generated.Versiondependencies.Initialversion.Initialdependency;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;

/**
 * Base for processing pair of potentially changed testcases, which are given by a dependencyfile.
 * @author reichelt
 *
 */
public abstract class PairProcessor extends VersionProcessor{

	protected final Map<TestCase, String> lastTestcaseCalls = new HashMap<>();
	
	public PairProcessor(final String[] args, boolean isProjectFolder) throws ParseException, JAXBException {
		super(args, isProjectFolder);
	}
	
	public PairProcessor(final String[] args) throws ParseException, JAXBException {
		super(args, true);
	}

	@Override
	protected void processInitialVersion(final Initialversion versioninfo) {
		for (final Initialdependency initDependency : versioninfo.getInitialdependency()) {
			final String clazz = initDependency.getTestclass().substring(0, initDependency.getTestclass().lastIndexOf("."));
			final String method = initDependency.getTestclass().substring(initDependency.getTestclass().lastIndexOf(".") + 1);
			final TestCase testcase = new TestCase(clazz, method);
			lastTestcaseCalls.put(testcase, versioninfo.getVersion());
		}
	}
	
	protected Set<TestCase> findTestcases(final Version versioninfo) {
		final Set<TestCase> testcases = new HashSet<>();
		for (final Dependency dependency : versioninfo.getDependency()) {
			for (final Testcase testcaseXML : dependency.getTestcase()) {
				for (final String method : testcaseXML.getMethod()) {
					final TestCase testcase = new TestCase(testcaseXML.getClazz(), method, testcaseXML.getModule());
					testcases.add(testcase);
				}
			}
		}
		return testcases;
	}
}
