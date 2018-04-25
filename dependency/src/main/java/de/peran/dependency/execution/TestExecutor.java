package de.peran.dependency.execution;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;

import de.peran.dependency.analysis.data.TestSet;

/**
 * Base functionality for executing tests
 * 
 * @author reichelt
 *
 */
public abstract class TestExecutor {

	private static final Logger LOG = LogManager.getLogger(TestExecutor.class);

	public static final String COMPILER_ARTIFACTID = "maven-compiler-plugin";

	protected final File projectFolder, resultsFolder;
	protected File lastTmpFile;
	protected int jdk_version = 8;

	public TestExecutor(final File projectFolder, final File resultsFolder) {
		this.projectFolder = projectFolder;
		this.resultsFolder = resultsFolder;
	}

	public void setJDKVersion(final int jdk_version) {
		this.jdk_version = jdk_version;
	}

	public int getJDKVersion() {
		return jdk_version;
	}
	
	public abstract void executeAllTests(final File logFile);
	public abstract void executeTests(final TestSet tests, final File logFolder);

	protected void setJDK(final Model model) {
		if (jdk_version != 8) {
			final String boot_class_path = System.getenv("BOOT_LIBS");
			if (boot_class_path != null) {
				final Plugin compiler = MavenPomUtil.findPlugin(model, COMPILER_ARTIFACTID, MavenKiekerTestExecutor.ORG_APACHE_MAVEN_PLUGINS);
				MavenPomUtil.extendCompiler(compiler, boot_class_path);
			}
		}
	}


	public abstract boolean isVersionRunning();

	/**
	 * Deletes temporary files, in order to not get memory problems
	 */
	public void deleteTemporaryFiles() {
		try {
			if (lastTmpFile != null && lastTmpFile.exists()) {
				final File[] tmpKiekerStuff = lastTmpFile.listFiles((FilenameFilter) new WildcardFileFilter("kieker*"));
				for (final File kiekerFolder : tmpKiekerStuff) {
					LOG.debug("Deleting: {}", kiekerFolder.getAbsolutePath());
					FileUtils.deleteDirectory(kiekerFolder);
				}
				FileUtils.deleteDirectory(lastTmpFile);
			}

		} catch (final IOException | IllegalArgumentException e) {
			LOG.info("Problems deleting last temp file..");
			e.printStackTrace();
		}
	}

}
