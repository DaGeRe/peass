/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peran.dependency.reader;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ParseProblemException;

import de.peran.dependency.ChangeManager;
import de.peran.dependency.DependencyManager;
import de.peran.dependency.execution.TestExecutor;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions;
import de.peran.vcs.VersionIterator;

/**
 * Reads the dependencies of a project
 * 
 * @author reichelt
 *
 */
public class DependencyReader extends DependencyReaderBase {

	private static final Logger LOG = LogManager.getLogger(DependencyReader.class);
	
	boolean init = false;

	public DependencyReader(final File projectFolder, final File dependencyFile, final String url, final VersionIterator iterator) {
		super(new Versiondependencies(), projectFolder, dependencyFile);

		this.iterator = iterator;

		dependencyResult.setUrl(url);
		dependencyResult.setVersions(new Versions());

		handler = new DependencyManager(projectFolder);

		searchFirstRunningCommit(iterator, handler.getExecutor(), projectFolder);
	}
	
	public DependencyReader(final File projectFolder, final File dependencyFile, final String url, final VersionIterator iterator, final Versiondependencies initialdependencies) {
		super(new Versiondependencies(), projectFolder, dependencyFile);

		this.iterator = iterator;

		dependencyResult.setUrl(url);
		dependencyResult.setVersions(initialdependencies.getVersions());
		dependencyResult.setInitialversion(initialdependencies.getInitialversion());

		handler = new DependencyManager(projectFolder);
		readCompletedVersions();
		init = true;
	}
	
	

	/**
	 * Searches the first commit where a mvn clean packages runs correct, i.e. returns 1
	 * 
	 * @param projectFolder
	 */
	public static void searchFirstRunningCommit(final VersionIterator iterator, final TestExecutor executor, final File projectFolder) {
		boolean successGettingCommit = iterator.goToFirstCommit();
		while (!successGettingCommit && iterator.hasNextCommit()) {
			successGettingCommit = iterator.goToNextCommit();
		}
		if (!successGettingCommit) {
			throw new RuntimeException("Repository does not contain usable commit - maybe SVN and path has changed?");
		} else {
			LOG.info("Found first commit: " + iterator.getTag());
		}
		boolean getTracesSuccess = false;
		while (!getTracesSuccess) {
			getTracesSuccess = executor.isVersionRunning();

			if (!getTracesSuccess) {
				LOG.debug("pom.xml does not exist / version is not running {}", iterator.getTag());
				iterator.goToNextCommit();
			}
		}
	}

	/**
	 * Reads the dependencies of the tests
	 */
	public void readDependencies() {
		try {
			if (!init){
				if (!readInitialVersion()) {
					return;
				}
			}

			LOG.debug("Analysiere {} Einträge", iterator.getSize());

			int overallSize = 0, prunedSize = 0;
			prunedSize += dependencyMap.size();

			final ChangeManager changeManager = new ChangeManager(projectFolder);
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

				handler.getExecutor().deleteTemporaryFiles();
			}

			LOG.debug("Finished dependency-reading");

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Versiondependencies getDependencies() {
		return dependencyResult;
	}


}
