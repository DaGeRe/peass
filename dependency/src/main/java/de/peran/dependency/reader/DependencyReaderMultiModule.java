package de.peran.dependency.reader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ParseProblemException;

import de.peran.dependency.ChangeManager;
import de.peran.dependency.DependencyManager;
import de.peran.dependency.analysis.data.TestDependencies;
import de.peran.dependency.execution.TestExecutor;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Initialversion;
import de.peran.generated.Versiondependencies.Versions;
import de.peran.vcs.VersionIterator;

public class DependencyReaderMultiModule extends DependencyReaderBase {
	private static final Logger LOG = LogManager.getLogger(DependencyReader.class);

	private final File moduleFolder;

	public DependencyReaderMultiModule(final File projectFolder, final File dependencyFile, final String url, final VersionIterator iterator, File moduleFolder) {
		super(new Versiondependencies(), projectFolder, dependencyFile);
		this.moduleFolder = moduleFolder;

		this.iterator = iterator;

		dependencyResult.setUrl(url);
		dependencyResult.setModule(moduleFolder.getName());
		dependencyResult.setVersions(new Versions());

		handler = new DependencyManager(projectFolder, moduleFolder);

		DependencyReader.searchFirstRunningCommit(iterator, handler.getExecutor(), projectFolder);
	}

	public DependencyReaderMultiModule(final File projectFolder, final File dependencyFile, final String url, final VersionIterator iterator, File moduleFolder,
			final Versiondependencies initialdependencies) {
		super(initialdependencies, projectFolder, dependencyFile);
		this.moduleFolder = moduleFolder;

		this.iterator = iterator;

		handler = new DependencyManager(projectFolder, moduleFolder);

		readCompletedVersions();
	}

	/**
	 * Reads the dependencies of the tests
	 */
	public void readDependencies() {
		try {
			if (dependencyResult.getInitialversion() == null) {
				if (!readInitialVersion()) {
					return;
				}
			}

			LOG.debug("Analysiere {} Einträge", iterator.getSize());

			int overallSize = 0, prunedSize = 0;
			prunedSize += dependencyMap.size();

			if (iterator.goTo0thCommit()){
//				while ()
			}
			
			final ChangeManager changeManager = new ChangeManager(projectFolder, moduleFolder);
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

				deleteTemporaryFiles();
			}

			LOG.debug("Finished dependency-reading");

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean readInitialVersion() throws IOException, InterruptedException {
		if (!handler.initialyGetTraces()) {
			return false;
		}
		dependencyMap = handler.getDependencyMap();
		final Initialversion initialversion = createInitialVersion(iterator.getTag(), dependencyMap, handler.getExecutor().getJDKVersion());
		dependencyResult.setInitialversion(initialversion);
		DependencyReaderUtil.write(dependencyResult, dependencyFile);

		return true;
	}

	/**
	 * Deletes temporary files, in order to not get memory problems
	 */
	private void deleteTemporaryFiles() {// TODO: Move to handler
		try {
			final File lastTmpFile = handler.getExecutor().getLastTmpFile();
			if (lastTmpFile != null) {
				final File[] tmpKiekerStuff = lastTmpFile.listFiles((FilenameFilter) new WildcardFileFilter("kieker*"));
				for (final File kiekerFolder : tmpKiekerStuff) {
					LOG.debug("Deleting: {}", kiekerFolder.getAbsolutePath());
					FileUtils.deleteDirectory(kiekerFolder);
				}
			}

		} catch (final IOException | IllegalArgumentException e) {
			LOG.info("Problems deleting last temp file..");
			e.printStackTrace();
		}
	}

}
