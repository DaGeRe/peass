package de.peran.dependency.execution;

import java.io.File;

import de.peran.dependency.analysis.data.TestSet;

public class GradleTestExecutor extends TestExecutor {

	public GradleTestExecutor(File projectFolder, File resultsFolder) {
		super(projectFolder, resultsFolder);
	}

	@Override
	public void executeAllTests(File logFile) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void executeTests(TestSet tests, File logFolder) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean isVersionRunning() {
		throw new RuntimeException("Not implemented yet");
	}

}
