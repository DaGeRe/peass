package de.peran.dependencyprocessors;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peran.dependency.analysis.data.TestCase;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;
import de.peran.vcs.GitCommit;

/**
 * Compares versions regaring their index, i.e. if version a is seen as before version b in the commit log
 * @author reichelt
 *
 */
public class VersionComparator implements Comparator<String> {

	private static final Logger LOG = LogManager.getLogger(VersionComparator.class);
	
	public static final VersionComparator INSTANCE = new VersionComparator();

	@Override
	public int compare(final String version1, final String version2) {
		final int indexOf = versions.indexOf(version1);
		final int indexOf2 = versions.indexOf(version2);
		return indexOf - indexOf2;
	}

	public static String getPreviousVersion(final String version) {
		final int index = versions.indexOf(version);
		return (index > 0) ? versions.get(index - 1) : "NO_BEFORE";
	}

	private static List<String> versions;
	private static Versiondependencies dependents = null;

	public static void setDependencies(final Versiondependencies dependencies) {
		dependents = dependencies;
		versions = new LinkedList<>();
		versions.add(dependents.getInitialversion().getVersion());
		dependents.getVersions().getVersion().stream().forEach(version -> versions.add(version.getVersion()));
	}
	
	public static boolean hasDependencies() {
		return dependents != null;
	}

	public static void setVersions(final List<GitCommit> commits) {
		versions = new LinkedList<>();
		commits.forEach(version -> versions.add(version.getTag()));
	}

	public static int getVersionIndex(final String version) {
		return versions.indexOf(version);
	}

	public static String getNextVersionForTestcase(final TestCase testcase, final String version) {
		boolean found = dependents.getInitialversion().getVersion().equals(version);
		for (final Version currentVersion : dependents.getVersions().getVersion()) {
			if (found) {
				final boolean containsTestcase = testContainsTestcase(testcase, currentVersion);
				if (containsTestcase)
					return currentVersion.getVersion();
			}

			if (currentVersion.getVersion().equals(version)) {
				found = true;
			}
		}
		return null;
	}

	private static boolean testContainsTestcase(final TestCase testcase, final Version currentVersion) {
		boolean containsTestcase = false;
		for (final Dependency dep : currentVersion.getDependency()) {
			for (final Testcase currentTestcase : dep.getTestcase()) {
				if (currentTestcase.getClazz().equals(testcase.getClazz()) && currentTestcase.getMethod().contains(testcase.getMethod())) {
					containsTestcase = true;
				}
			}
		}
		return containsTestcase;
	}

	public static String getPreviousVersionForTestcase(final TestCase testcase, final String version) {
		String previous = dependents.getInitialversion().getVersion();
		LOG.trace("Search previous: {}", version);
		for (final Version currentVersion : dependents.getVersions().getVersion()) {
			LOG.trace("Searching: {} {}", previous, version);
			if (currentVersion.getVersion().equals(version)) {
				LOG.trace("Found: {}", previous);
				return previous;
			}
			final boolean containsTestcase = testContainsTestcase(testcase, currentVersion);
			if (containsTestcase){
				previous = currentVersion.getVersion();
			}

		}
		return null;
	}

	/**
	 * Determines whether version is before version2
	 * @param version
	 * @param startversion
	 * @return	true, is version is before version2, false otherwise
	 */
	public static boolean isBefore(final String version, final String version2) {
		final int indexOf = versions.indexOf(version);
		final int indexOf2 = versions.indexOf(version2);
		return indexOf < indexOf2;
	}

}
