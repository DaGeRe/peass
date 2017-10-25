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
package de.peran.dependency.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

import de.peran.dependency.PackageFinder;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.testtransformation.JUnitTestTransformer;

/**
 * Organizes the running of tests in a maven project by enhancing the pom,
 * changing the test classes and calling the maven test goal
 * 
 * @author reichelt
 *
 */
public class MavenKiekerTestExecutor extends TestExecutor {

	private static final Logger LOG = LogManager.getLogger(MavenKiekerTestExecutor.class);

	public static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
	public static final String SUREFIRE_ARTIFACTID = "maven-surefire-plugin";

	/**
	 * This is added to surefire, assuming that kieker has been downloaded
	 * already, so that the aspectj-weaving can take place.
	 */
	protected static final String KIEKER_ARG_LINE = "-javaagent:" + System.getProperty("user.home") + "/.m2/repository/net/kieker-monitoring/kieker/1.12/kieker-1.12-aspectj.jar";

	private final File resultsFolder;
	protected File lastTmpFile;
	protected Charset lastEncoding = StandardCharsets.UTF_8;

	public MavenKiekerTestExecutor(final File projectFolder, final File moduleFolder, final File resultsFolder) {
		super(projectFolder, moduleFolder);
		this.resultsFolder = resultsFolder;
	}

	protected void compileVersion(final File logFile) {
		final ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "test-compile");

		pb.directory(projectFolder);
		if (logFile != null) {
			pb.redirectOutput(Redirect.appendTo(logFile));
			pb.redirectError(Redirect.appendTo(logFile));
		} 

		try {
			Process process = pb.start();
			int result = process.waitFor();
			if (result != 0) {
				throw new RuntimeException("Compilation failed, see" + logFile.getAbsolutePath());
			}

			generateAOPXML();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Compilation failed, see" + logFile.getAbsolutePath());
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException("Compilation failed, see" + logFile.getAbsolutePath());
		}
	}

	protected void generateAOPXML() throws IOException {
		String lowestPackage = PackageFinder.getLowestPackageOverall(moduleFolder);
		File metainf = new File(moduleFolder, "target/test-classes/META-INF");
		metainf.mkdir(); 
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(metainf, "aop.xml")))) {
			// <!DOCTYPE aspectj PUBLIC "-//AspectJ//DTD//EN"
			// "http://www.aspectj.org/dtd/aspectj_1_5_0.dtd">
			writer.write("<!DOCTYPE aspectj PUBLIC \"-//AspectJ//DTD//EN\" \"http://www.aspectj.org/dtd/aspectj_1_5_0.dtd\">\n");
			writer.write("<aspectj>\n");
			writer.write("	<weaver options=\"-verbose\">");
			writer.write("<include within=\"" + lowestPackage + "..*\" />");
			writer.write("	</weaver>\n");
			writer.write("	<aspects>");
			writer.write("		<aspect ");
			writer.write("name=\"kieker.monitoring.probe.aspectj.operationExecution.OperationExecutionAspectFull\" />");
			writer.write("	</aspects>\n");
			writer.write("</aspectj>");
			writer.flush();
		}
	}

	protected Process buildProcess(final File logFile, final String... commandLineAddition) throws IOException {
		final String[] originals = new String[] { "mvn", 
				"surefire:test", "-fn", 
				"-Dcheckstyle.skip=true", 
				"-Dmaven.compiler.source=1.7", 
				"-Dmaven.compiler.target=1.7",
				"-Dmaven.javadoc.skip=true",
				"-Denforcer.skip=true"};
		final String[] vars = new String[commandLineAddition.length + originals.length];
		for (int i = 0; i < originals.length; i++) {
			vars[i] = originals[i];
		}
		for (int i = 0; i < commandLineAddition.length; i++) {
			vars[originals.length + i] = commandLineAddition[i];
		}

		LOG.debug("Command: {}", vars);

		final ProcessBuilder pb = new ProcessBuilder(vars);
		pb.environment().put("KOPEME_HOME", resultsFolder.getAbsolutePath());

		pb.directory(moduleFolder);
		if (logFile != null) {
			pb.redirectOutput(Redirect.appendTo(logFile));
			pb.redirectError(Redirect.appendTo(logFile));
		}

		final Process process = pb.start();
		return process;
	}

	/**
	 * Runs all tests and saves the results to the given result folder
	 * 
	 * @param specialResultFolder
	 *            Folder for saving the results
	 * @param tests
	 *            Name of the test that should be run
	 */
	public void executeAllTests(final File logFile) {
		preparePom();
		prepareTests();

		try {
			compileVersion(logFile);
			final Process process = buildProcess(logFile);
			LOG.info("Starting Process");
			process.waitFor();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Runs the given tests and saves the results to the given result folder
	 * 
	 * @param specialResultFolder
	 *            Folder for saving the results
	 * @param tests
	 *            Name of the test that should be run
	 */
	public void executeTests(final TestSet tests, File logFolder) {
		preparePom();
		prepareTests();

		compileVersion(new File(logFolder, "log_compilation.txt"));
		for (final Map.Entry<String, List<String>> clazzEntry : tests.entrySet()) {
			final File logFile = new File(logFolder, "log_" + clazzEntry.getKey() + ".txt");
			if (clazzEntry.getValue().size() > 0) {
				for (final String method : clazzEntry.getValue()) {
					if (method.length() > 0) {
						runTest(logFile, clazzEntry.getKey() + "#" + method);
					} else {
						runTest(logFile, clazzEntry.getKey());
					}

				}
			} else {
				runTest(logFile, clazzEntry.getKey());
			}
		}

	}

	/**
	 * Runs the given test and saves the results to the result folder.
	 * 
	 * @param specialResultFolder
	 *            Folder for saving the results
	 * @param testname
	 *            Name of the test that should be run
	 */
	private void runTest(final File logFile, final String testname) {
		try {
			LOG.debug("Executing: {}", testname);
			final Process process = buildProcess(logFile, "-Dtest=" + testname);
			process.waitFor();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prepares the tests by changing their source, so they are executed with
	 * KoPeMe.
	 */
	public void prepareTests() {
		final JUnitTestTransformer testGenerator = new JUnitTestTransformer(moduleFolder, false, true);
		testGenerator.setEncoding(lastEncoding);
		testGenerator.setIterations(1);
		testGenerator.setWarmupExecutions(0);
		testGenerator.transformTests();
	}

	public static Charset getEncoding(Model model) {
		Charset value = StandardCharsets.UTF_8;
		Properties properties = model.getProperties();
		if (properties != null) {
			String encoding = (String) properties.get("project.build.sourceEncoding");
			if (encoding != null) {
				if (encoding.equals("ISO-8859-1")) {
					value = StandardCharsets.ISO_8859_1;
				}
			}
		}
		return value;
	}

	public static Charset rewriteSurefire(final File pomFile, final String additionalArgLine) {
		final MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			final Model model = reader.read(new FileInputStream(pomFile));
			if (model.getBuild() == null) {
				model.setBuild(new Build());
			}
			MavenKiekerTestExecutor.extendDependencies(model);

			final Plugin surefire = MavenKiekerTestExecutor.findPlugin(model, MavenKiekerTestExecutor.SUREFIRE_ARTIFACTID, MavenKiekerTestExecutor.ORG_APACHE_MAVEN_PLUGINS);

			MavenKiekerTestExecutor.extendSurefire(additionalArgLine, surefire, true);

			for (final Dependency dependency : model.getDependencies()) {
				if ("junit".equals(dependency.getArtifactId())) {
					dependency.setVersion("4.12");
				}
			}

			final MavenXpp3Writer writer = new MavenXpp3Writer();
			writer.write(new FileWriter(pomFile), model);

			Charset encoding = MavenKiekerTestExecutor.getEncoding(model);
			return encoding;
		} catch (IOException | XmlPullParserException e1) {
			e1.printStackTrace();
		}
		return StandardCharsets.UTF_8;
	}

	public void preparePom() {
		final MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			File pomFile = new File(moduleFolder, "pom.xml");
			final Model model = reader.read(new FileInputStream(pomFile));
			if (model.getBuild() == null) {
				model.setBuild(new Build());
			}
			final Plugin surefire = findPlugin(model, SUREFIRE_ARTIFACTID, ORG_APACHE_MAVEN_PLUGINS);

			final Path tempFiles = Files.createTempDirectory("kiekerTemp");
			lastTmpFile = tempFiles.toFile();
			final String argline = KIEKER_ARG_LINE + " -Djava.io.tmpdir=" + tempFiles.toString();
			extendSurefire(argline, surefire, true);
			extendDependencies(model);

			setJDK(model);

			final MavenXpp3Writer writer = new MavenXpp3Writer();
			writer.write(new FileWriter(pomFile), model);

			lastEncoding = getEncoding(model);
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}
	}

	public Charset getEncoding() {
		return lastEncoding;
	}

	public static void extendDependencies(final Model model) {
		if (model.getDependencies() == null) {
			model.setDependencies(new LinkedList<Dependency>());
		}

		for (final Dependency dependency : model.getDependencies()) {
			if (dependency.getArtifactId().equals("junit") && dependency.getGroupId().equals("junit")) {
				dependency.setVersion("4.12");
			}
		}

		final List<Dependency> dependencies = model.getDependencies();

		final Dependency kopeme_dependency = new Dependency();
		kopeme_dependency.setGroupId("de.dagere.kopeme");
		kopeme_dependency.setArtifactId("kopeme-junit3");
		kopeme_dependency.setVersion("0.8-SNAPSHOT");
		kopeme_dependency.setScope("test");

		dependencies.add(kopeme_dependency);

		final Dependency kopeme_dependency2 = new Dependency();
		kopeme_dependency2.setGroupId("de.dagere.kopeme");
		kopeme_dependency2.setArtifactId("kopeme-junit");
		kopeme_dependency2.setVersion("0.8-SNAPSHOT");
		kopeme_dependency2.setScope("test");

		dependencies.add(kopeme_dependency2);
	}

	public static void extendSurefire(final String additionalArgLine, final Plugin plugin, boolean updateVersion) {
		if (plugin.getConfiguration() == null) {
			plugin.setConfiguration(new Xpp3Dom("configuration"));
		}

		if (updateVersion) {
			System.out.println("Surefire" + plugin.getClass() + " " + plugin.getConfiguration().getClass());
			plugin.setVersion("2.21.0-SNAPSHOT");
		}

		final Xpp3Dom conf = (Xpp3Dom) plugin.getConfiguration();
		TestExecutor.addNode(conf, "forkmode", "always");

		Xpp3Dom argLine = conf.getChild("argLine");
		if (argLine != null) {
			final String changedArgLine = argLine.getValue().contains("-Xmx") ? argLine.getValue().replaceAll("-Xmx[0-9]{0,3}[mM]", "-Xmx1g") : argLine.getValue();
			argLine.setValue(changedArgLine + " " + additionalArgLine);
		} else {
			argLine = new Xpp3Dom("argLine");
			argLine.setValue(additionalArgLine);
			conf.addChild(argLine);
		}
	}

	public File getLastTmpFile() {
		return lastTmpFile;
	}
}
