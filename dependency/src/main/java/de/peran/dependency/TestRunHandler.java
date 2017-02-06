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
package de.peran.dependency;

import java.io.File;
import java.io.IOException;

import de.dagere.kopeme.PomProjectNameReader;
import de.dagere.kopeme.PomProjectNameReader.ProjectInfo;

/**
 * Handles the running of tests
 * @author reichelt
 *
 */
public class TestRunHandler {
	protected final File projectFolder;
	protected final File resultsFolder, logFolder;
	protected final String groupId;
	protected final String artifactId;
	protected final File xmlFileFolder;
	private final MavenKiekerTestExecutor executor;

	public TestRunHandler(final File projectFolder) {
		super();
		this.projectFolder = projectFolder;
		resultsFolder = new File(projectFolder, "peran_results_kieker");
		logFolder = new File(projectFolder, "peran_logs");
		logFolder.mkdir();
		final ProjectInfo projectInfo = new PomProjectNameReader().getProjectInfo(new File(projectFolder, "pom.xml"));
		this.groupId = projectInfo.getGroupId();
		this.artifactId = projectInfo.getArtifactId();
		xmlFileFolder = new File(resultsFolder, groupId + File.separator + artifactId);

		executor = new MavenKiekerTestExecutor(projectFolder, resultsFolder, logFolder);
	}

	/**
	 * Executes the tests with all methods of the given test classes.
	 * 
	 * @param testsToUpdate
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void executeKoPeMeKiekerRun(final TestSet testsToUpdate) throws IOException, InterruptedException {
		executor.executeTests(testsToUpdate);
	}

}
