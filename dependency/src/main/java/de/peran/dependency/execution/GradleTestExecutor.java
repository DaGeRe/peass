package de.peran.dependency.execution;

import java.io.File;

import de.peran.dependency.analysis.data.TestSet;

public class GradleTestExecutor extends TestExecutor {

	public GradleTestExecutor(File projectFolder, File moduleFolder, File resultsFolder) {
		super(projectFolder, moduleFolder, resultsFolder);
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
