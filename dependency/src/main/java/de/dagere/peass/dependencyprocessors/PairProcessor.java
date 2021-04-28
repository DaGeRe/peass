package de.dagere.peass.dependencyprocessors;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.InitialDependency;
import de.dagere.peass.dependency.persistence.InitialVersion;

/**
 * Base for processing pair of potentially changed testcases, which are given by a dependencyfile.
 * @author reichelt
 *
 */
public abstract class PairProcessor extends VersionProcessor{

	protected final Map<TestCase, String> lastTestcaseCalls = new HashMap<>();
	
	public PairProcessor(final File projectFolder, final Dependencies dependencies){
      super(projectFolder, dependencies);
	}
	
	public PairProcessor()  {
	}

	@Override
	public void processInitialVersion(final InitialVersion versioninfo) {
		for (final Map.Entry<ChangedEntity, InitialDependency> initDependency : versioninfo.getInitialDependencies().entrySet()) {
			final ChangedEntity test = initDependency.getKey();
         final TestCase testcase = new TestCase(test.getClazz(), test.getMethod(), test.getModule());
			lastTestcaseCalls.put(testcase, versioninfo.getVersion());
		}
	}
}
