package de.peran.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ParseProblemException;

import de.peran.dependency.ChangeManager;
import de.peran.dependency.DependencyManager;
import de.peran.dependency.analysis.data.TestDependencies;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Initialversion;
import de.peran.generated.Versiondependencies.Initialversion.Initialdependency;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency;
import de.peran.generated.Versiondependencies.Versions.Version.Dependency.Testcase;
import de.peran.vcs.VersionIterator;

/**
 * Continues dependency reading, based on given dependencies that have alread been read
 * 
 * @author reichelt
 *
 */
public class DependencyReadingContinuer extends DependencyReaderBase {

	public DependencyReadingContinuer() {
		super(null, null, null);
	}

	private static final Logger LOG = LogManager.getLogger(DependencyReader.class);

	private final File projectFolder = null;

	private TestDependencies dependencyMap;

//	public DependencyReadingContinuer(final File projectFolder, final File dependencyFile, final VersionIterator iterator, final Versiondependencies initialdependencies) {
//		super(initialdependencies, dependencyFile)
//	}

	/**
	 * Reads the dependencies of the tests
	 */
	public void readDependencies() {
		try {
//			handler = readCompletedVersions();
			final ChangeManager changeManager = new ChangeManager(projectFolder);

			int overallSize = 0, prunedSize = 0;
			prunedSize += dependencyMap.size();

			iterator.goTo0thCommit();

			changeManager.saveOldClasses();
			while (iterator.hasNextCommit()) {
				iterator.goToNextCommit();

				try {
					final int tests = analyseVersion(changeManager);
					DependencyReaderUtil.write(dependencyResult, dependencyFile);
					overallSize += dependencyMap.size();
					prunedSize += tests;
				} catch (final ParseProblemException ppe) {
					ppe.printStackTrace();
				}

				LOG.info("Overall-tests: {} Executed tests with pruning: {}", overallSize, prunedSize);
			}

			LOG.debug("Finished dependency-reading");

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
	

}
