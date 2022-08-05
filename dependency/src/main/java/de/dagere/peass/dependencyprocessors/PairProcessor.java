package de.dagere.peass.dependencyprocessors;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.InitialCallList;
import de.dagere.peass.dependency.persistence.InitialCommit;
import de.dagere.peass.dependency.persistence.StaticTestSelection;

/**
 * Base for processing pair of potentially changed testcases, which are given by a static selection file or execution file.
 * @author reichelt
 *
 */
public abstract class PairProcessor extends CommitProcessor{

	protected final Map<TestCase, String> lastTestcaseCalls = new HashMap<>();
	
	public PairProcessor(final File projectFolder, final StaticTestSelection dependencies){
      super(projectFolder, dependencies);
	}
	
	public PairProcessor()  {
	}

	@Override
	public void processInitialVersion(final InitialCommit commitinfo) {
		for (final Map.Entry<TestMethodCall, InitialCallList> initDependency : commitinfo.getInitialDependencies().entrySet()) {
         final TestCase testcase = initDependency.getKey();
			lastTestcaseCalls.put(testcase, commitinfo.getCommit());
		}
	}
}
