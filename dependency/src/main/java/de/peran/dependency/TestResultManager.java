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
package de.peran.dependency;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.PomProjectNameReader;
import de.dagere.kopeme.PomProjectNameReader.ProjectInfo;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependency.execution.GradleTestExecutor;
import de.peran.dependency.execution.MavenKiekerTestExecutor;
import de.peran.dependency.execution.MultiModuleTestExecutor;
import de.peran.dependency.execution.TestExecutor;

/**
 * Handles the running of tests
 * 
 * @author reichelt
 *
 */
public class TestResultManager {

	private static final Logger LOG = LogManager.getLogger(TestResultManager.class);

	protected final File projectFolder, moduleFolder;
	protected final File resultsFolder, logFolder;
	protected final TestExecutor executor;

	public TestResultManager(final File projectFolder) {
		super();
		this.projectFolder = projectFolder;
		this.moduleFolder = projectFolder;

		PeASSFolderUtil.setProjectFolder(projectFolder);
		resultsFolder = PeASSFolderUtil.getResultFolder();
		logFolder = PeASSFolderUtil.getLogFolder();

		File pom = new File(projectFolder, "pom.xml");
		if (pom.exists()) {
			executor = new MavenKiekerTestExecutor(projectFolder, projectFolder, resultsFolder);
		}else{
			executor = new GradleTestExecutor(projectFolder, projectFolder, resultsFolder);
				
		}
		
	}

	public TestResultManager(File projectFolder, File moduleFolder) {
		this.projectFolder = projectFolder;
		this.moduleFolder = moduleFolder;

		PeASSFolderUtil.setProjectFolder(projectFolder);
		resultsFolder = PeASSFolderUtil.getResultFolder();
		logFolder = PeASSFolderUtil.getLogFolder();
		executor = new MultiModuleTestExecutor(projectFolder, moduleFolder, resultsFolder);
	}

	/**
	 * Executes the tests with all methods of the given test classes.
	 * 
	 * @param testsToUpdate
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void executeKoPeMeKiekerRun(final TestSet testsToUpdate, final String versionName) throws IOException {
		File logVersionFolder = new File(logFolder, versionName);
		if (!logVersionFolder.exists()) {
			logVersionFolder.mkdir();
		}
		executor.executeTests(testsToUpdate, logVersionFolder);
	}

	public File getXMLFileFolder() {
		final File pomXmlFile = new File(moduleFolder, "pom.xml");
		LOG.debug("POM: {} Existing: {}", pomXmlFile.getAbsolutePath(), pomXmlFile.exists());
		if (!pomXmlFile.exists()) {
			return null;
		}
		final ProjectInfo projectInfo = new PomProjectNameReader().getProjectInfo(pomXmlFile);
		final String groupId = projectInfo.getGroupId();
		final String artifactId = projectInfo.getArtifactId();
		final File xmlFileFolder = new File(resultsFolder, groupId + File.separator + artifactId);
		return xmlFileFolder;
	}

	public void deleteTempFiles() {
		executor.deleteTemporaryFiles();
	}

}
