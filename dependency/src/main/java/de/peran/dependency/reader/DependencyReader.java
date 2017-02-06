/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the Affero GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Affero GNU General Public License for more details.
 *
 *     You should have received a copy of the Affero GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peran.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tmatesoft.svn.core.SVNLogEntry;

import com.github.javaparser.ParseProblemException;

import de.peran.dependency.ChangedTestClassesHandler;
import de.peran.dependency.TestDependencies;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Initialversion;
import de.peran.generated.Versiondependencies.Initialversion.Initialdependency;
import de.peran.generated.Versiondependencies.Versions;
import de.peran.utils.StreamGobbler;
import de.peran.vcs.GitCommit;
import de.peran.vcs.VersionIterator;
import de.peran.vcs.VersionIteratorGit;
import de.peran.vcs.VersionIteratorSVN;

/**
 * Reads the dependencies of a project
 * @author reichelt
 *
 */
public class DependencyReader {
	
	private static final Logger LOG = LogManager.getLogger(DependencyReader.class);
	
	private final VersionIterator iterator;
	private final File projectFolder;
	private final File dependencyFile;
	private final Versiondependencies dependencyResult = new Versiondependencies();
	
	public DependencyReader(final File projectFolder, final File dependencyFile, final List<GitCommit> entries) {
		this.projectFolder = projectFolder;
		this.dependencyFile = dependencyFile;
		
		iterator = new VersionIteratorGit(entries, projectFolder);

		dependencyResult.setUrl("git-URL"); // TODO
		dependencyResult.setVersions(new Versions());

		searchFirstRunningCommit(projectFolder);
	}
	
	public DependencyReader(final File projectFolder, final String url, final File dependencyFile, final List<SVNLogEntry> entries) {
		this.projectFolder = projectFolder;
		this.dependencyFile = dependencyFile;
		
		iterator = new VersionIteratorSVN(projectFolder, entries, url);
		dependencyResult.setUrl(url);
		dependencyResult.setVersions(new Versions());
	}
	
	
	/**
	 * Searches the first commit where a mvn clean packages runs correct, i.e. returns 1
	 * @param projectFolder
	 */
	private void searchFirstRunningCommit(final File projectFolder) {
		iterator.goToFirstCommit();
		boolean getTracesSuccess = false;
		while (!getTracesSuccess) {
			final File potentialPom = new File(projectFolder, "pom.xml");
			LOG.debug(potentialPom);
			getTracesSuccess = potentialPom.exists();
			if (getTracesSuccess) {
				LOG.debug("pom.xml existiert");
				final ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package", "-DskipTests=true");
				pb.directory(projectFolder);
				try {
					final Process process = pb.start();
					final String result = StreamGobbler.getFullProcess(process, true, 5000);
					System.out.println(result);
					process.waitFor();
					final int returncode = process.exitValue();
					if (returncode != 0) {
						getTracesSuccess = false;
					}
				} catch (final IOException | InterruptedException e) {
					e.printStackTrace();
					getTracesSuccess = false;
				}
			}
			
			if (!getTracesSuccess){
				LOG.debug("pom.xml nicht valide/existiert nicht in {}", iterator.getTag());
				iterator.goToNextCommit();
			}
		}
	}

	/**
	 * Reads the dependencies of the tests
	 */
	public void readDependencies() {
		try {
			final ChangedTestClassesHandler handler = new ChangedTestClassesHandler(projectFolder);
			handler.initialyGetTraces();
			final TestDependencies dependencyMap = handler.getDependencyMap();
			writeInitialDependency(iterator.getTag(), dependencyMap);
			
			LOG.debug("Analysiere {} Einträge", iterator.getSize());

			int overallSize = 0, prunedSize = 0;
			prunedSize += dependencyMap.size();

			handler.saveOldClasses();
			while (iterator.hasNextCommit()){
				iterator.goToNextCommit();
				
				try {
					final Map<String, List<String>> testsToRun = DependencyReaderUtil.analyseVersion(dependencyFile,
							handler, dependencyMap, dependencyResult, iterator.getTag());
					overallSize += dependencyMap.size();
					prunedSize += testsToRun.size();
				} catch (final ParseProblemException ppe) {
					ppe.printStackTrace();
				}

				LOG.info("Overall-tests: {} Executed tests with pruning: {}", overallSize, prunedSize);
			}
			

			LOG.debug("Finished dependency-reading");

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the initial dependency, where the order of changedclass and testclass is changed.
	 * @param url
	 * @param startVersion
	 * @param dependencyMap
	 * @return
	 */
	public void writeInitialDependency(final String startVersion, final TestDependencies dependencyMap) { //TODO Neues XSD Format
		final Initialversion initialversion = new Initialversion();
		initialversion.setVersion(startVersion);
		LOG.debug("Starting writing: {}", dependencyMap.getDependencyMap().size());
		for (final Map.Entry<String, Map<String, Set<String>>> dependencyEntry : dependencyMap.getDependencyMap().entrySet()) {
			final Initialdependency dependency = new Initialdependency();
			dependency.setTestclass(dependencyEntry.getKey());
			for (final Map.Entry<String, Set<String>> dependentClassEntry : dependencyEntry.getValue().entrySet()) {
				final String dependentclass = dependentClassEntry.getKey();
				if (!dependentclass.contains("junit") && !dependentclass.contains("log4j")){
					for (final String dependentmethod : dependentClassEntry.getValue()){
						dependency.getDependentclass().add(dependentclass + "." + dependentmethod);
					}
				}
					
			}
			initialversion.getInitialdependency().add(dependency);
		}
		dependencyResult.setInitialversion(initialversion);
		
		DependencyReaderUtil.write(dependencyResult, dependencyFile);
	}
}
