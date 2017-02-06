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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peran.testtransformation.JUnitTestTransformer;

/**
 * Organizes the running of tests in a maven project by enhancing the pom, changing the test classes and calling the maven test goal
 * @author reichelt
 *
 */
public class MavenKiekerTestExecutor {

	private static final Logger LOG = LogManager.getLogger(MavenKiekerTestExecutor.class);

	private static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
	private static final String SUREFIRE_ARTIFACTID = "maven-surefire-plugin";

	/**
	 * This is added to surefire, assuming that kieker has been downloaded already, so that the aspectj-weaving can take place.
	 */
	protected static final String KIEKER_ARG_LINE = "-javaagent:" + System.getProperty("user.home") + "/.m2/repository/net/kieker-monitoring/kieker/1.12/kieker-1.12-aspectj.jar";

	private final File projectFolder;
	private final File logFolder;
	private final File resultsFolder;

	public MavenKiekerTestExecutor(final File projectFolder, final File resultsFolder, final File logFolder) {
		this.projectFolder = projectFolder;
		this.logFolder = logFolder;
		this.resultsFolder = resultsFolder;
	}

	private Process buildProcess(final File logFile, final String... commandLineAddition) throws IOException {
		final String[] originals = new String[] { "mvn", "clean", "test", "-fn", "-Dcheckstyle.skip=true",
				"-Dmaven.compiler.source=1.7",
				"-Dmaven.compiler.target=1.7",
				"-Dmaven.javadoc.skip=true" };
		final String[] vars = new String[commandLineAddition.length + originals.length];
		for (int i = 0; i < originals.length; i++) {
			vars[i] = originals[i];
		}
		for (int i = 0; i < commandLineAddition.length; i++) {
			vars[originals.length + i] = commandLineAddition[i];
		}

		final ProcessBuilder pb = new ProcessBuilder(vars);
		pb.environment().put("KOPEME_HOME", resultsFolder.getAbsolutePath());
		pb.directory(projectFolder);
		if (logFile != null) {
			pb.redirectOutput(logFile);
			pb.redirectError(logFile);
		}

		final Process process = pb.start();
		return process;
	}

	/**
	 * Runs all tests and saves the results to the given result folder
	 * @param specialResultFolder Folder for saving the results
	 * @param tests Name of the test that should be run
	 */
	public void executeTests() {
		preparePom();
		prepareTests();

		try {
			final Process process = buildProcess(new File(logFolder, "log.txt"));
			process.waitFor();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Runs the given tests and saves the results to the given result folder
	 * @param specialResultFolder Folder for saving the results
	 * @param tests Name of the test that should be run
	 */
	public void executeTests(final TestSet tests) {
		preparePom();
		prepareTests();

		for (final Map.Entry<String, List<String>> clazzEntry : tests.entrySet()) {
			if (clazzEntry.getValue().size() > 0) {
				for (final String method : clazzEntry.getValue()){
					runTest( clazzEntry.getKey()+"#"+method);
				}
			} else {
				runTest( clazzEntry.getKey());
			}
		}

	}

	/**
	 * Runs the given test and saves the results to the result folder.
	 * @param specialResultFolder Folder for saving the results
	 * @param testname	Name of the test that should be run
	 */
	private void runTest(final String testname) {
		preparePom();
		prepareTests();

		try {
			final Process process = buildProcess(new File(logFolder, "log.txt"), "-Dtest=" + testname);
			process.waitFor();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prepares the tests by changing their source, so they are executed with KoPeMe.
	 */
	private void prepareTests() {
		final JUnitTestTransformer testGenerator = new JUnitTestTransformer(projectFolder, false, true);
		testGenerator.setIterations(1);
		testGenerator.setWarmupExecutions(0);
		testGenerator.transformTests();
	}

	private void preparePom() {
		final MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			final File pomFile = new File(projectFolder, "pom.xml");
			final Model model = reader.read(new FileInputStream(pomFile));
			if (model.getBuild() == null) {
				model.setBuild(new Build());
			}
			Plugin surefire = null;
			for (final Plugin plugin : model.getBuild().getPlugins()) {
				if (plugin.getArtifactId().equals(SUREFIRE_ARTIFACTID) && plugin.getGroupId().equals(ORG_APACHE_MAVEN_PLUGINS)) {
					surefire = plugin;
					break;
				}
			}
			if (surefire == null) {
				surefire = new Plugin();
				surefire.setArtifactId(SUREFIRE_ARTIFACTID);
				surefire.setGroupId(ORG_APACHE_MAVEN_PLUGINS);
				model.getBuild().getPlugins().add(surefire);
			}
			extendSurefire(KIEKER_ARG_LINE, surefire);
			extendDependencies(model);

			final MavenXpp3Writer writer = new MavenXpp3Writer();
			writer.write(new FileWriter(pomFile), model);
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}
	}

	private void extendDependencies(final Model model) {
		if (model.getDependencies() == null) {
			model.setDependencies(new LinkedList<Dependency>());
		}
		final List<Dependency> dependencies = model.getDependencies();

		final Dependency kopeme_dependency = new Dependency();
		kopeme_dependency.setGroupId("de.dagere.kopeme");
		kopeme_dependency.setArtifactId("kopeme-junit3");
		kopeme_dependency.setVersion("0.8-SNAPSHOT");
		kopeme_dependency.setScope("test");

		dependencies.add(kopeme_dependency);
	}

	private static void extendSurefire(final String additionalArgLine, final Plugin plugin) {
		if (plugin.getConfiguration() == null) {
			plugin.setConfiguration(new Xpp3Dom("configuration"));
		}
		System.out.println("Surefire" + plugin.getClass() + " " + plugin.getConfiguration().getClass());
		plugin.setVersion("2.17");

		final Xpp3Dom conf = (Xpp3Dom) plugin.getConfiguration();
		Xpp3Dom forkMode = conf.getChild("forkMode");
		if (forkMode != null) {
			forkMode.setValue("always");
		} else if (forkMode == null) {
			forkMode = new Xpp3Dom("forkMode");
			forkMode.setValue("always");
			conf.addChild(forkMode);
		}

		Xpp3Dom argLine = conf.getChild("argLine");
		if (argLine != null) {
			argLine.setValue(argLine.getValue() + " " + additionalArgLine); // in case argLine does not end in space..
		} else {
			argLine = new Xpp3Dom("argLine");
			argLine.setValue(additionalArgLine);
			conf.addChild(argLine);
		}
	}

	public static void insertDependency(final File eingabePOM, final String kopemedependency) {
		final File temp = new File(eingabePOM.getAbsolutePath() + ".1");
		try (final BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
			try (final BufferedReader br = new BufferedReader(new FileReader(eingabePOM))) {
				String line;
				while ((line = br.readLine()) != null) {

					bw.write(line + "\n");

					if (line.contains("<dependencies>"))
						bw.write(kopemedependency);
				}
			}
			bw.flush();
			eingabePOM.delete();
			temp.renameTo(eingabePOM);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
