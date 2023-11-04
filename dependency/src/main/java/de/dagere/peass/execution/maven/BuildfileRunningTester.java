package de.dagere.peass.execution.maven;

import de.dagere.peass.execution.utils.TestExecutor;

public interface BuildfileRunningTester {
   boolean isCommitRunning(final String commit, TestExecutor executor);
}