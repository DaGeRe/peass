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


import de.peran.dependency.ClazzFinder;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.testtransformation.JUnitTestTransformer;
import de.peran.utils.StreamGobbler;

/**
 * Organizes the running of tests in a maven project by enhancing the pom, changing the test classes and calling the maven test goal
 * 
 * @author reichelt
 *
 */
public class MavenKiekerTestExecutor extends TestExecutor {

	private static final Logger LOG = LogManager.getLogger(MavenKiekerTestExecutor.class);

	public static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
	public static final String SUREFIRE_ARTIFACTID = "maven-surefire-plugin";

	/**
	 * This is added to surefire, assuming that kieker has been downloaded already, so that the aspectj-weaving can take place.
	 */
	protected static final String KIEKER_ARG_LINE = "-javaagent:" + System.getProperty("user.home") + "/.m2/repository/net/kieker-monitoring/kieker/1.12/kieker-1.12-aspectj.jar";

	// private final File resultsFolder;
	protected Charset lastEncoding = StandardCharsets.UTF_8;
	protected final JUnitTestTransformer testGenerator;

	public MavenKiekerTestExecutor(final File projectFolder, final File moduleFolder, final File resultsFolder, boolean useKieker) {
		super(projectFolder, moduleFolder, resultsFolder);
		testGenerator = new JUnitTestTransformer(moduleFolder);
		testGenerator.setUseKieker(useKieker);
		testGenerator.setLogFullData(false);
		testGenerator.setEncoding(lastEncoding);
		testGenerator.setIterations(1);
		testGenerator.setWarmupExecutions(0);
		// this.resultsFolder = resultsFolder;
	}

	public MavenKiekerTestExecutor(final File projectFolder, final File moduleFolder, final File resultsFolder, JUnitTestTransformer testtransformer) {
		super(projectFolder, moduleFolder, resultsFolder);
		this.testGenerator = testtransformer;
		// this.resultsFolder = resultsFolder;
	}

	protected boolean compileVersion(final File logFile) {
		return compileVersion(logFile, "mvn", "clean", "test-compile");
	}

	protected boolean compileVersion(final File logFile, final String... compilationArgs) {
		final ProcessBuilder pb = new ProcessBuilder(compilationArgs);
		pb.directory(projectFolder);
		if (logFile != null) {
			pb.redirectOutput(Redirect.appendTo(logFile));
			pb.redirectError(Redirect.appendTo(logFile));
		}

		try {
			final Process process = pb.start();
			final int result = process.waitFor();
			if (result != 0) {
				LOG.error("Compilation failed, see " + logFile.getAbsolutePath());
				return false;
			}

			generateAOPXML();
		} catch (final IOException e) {
			e.printStackTrace();
			LOG.error("Compilation failed, see " + logFile.getAbsolutePath());
		} catch (final InterruptedException e) {
			e.printStackTrace();
			LOG.error("Compilation failed, see " + logFile.getAbsolutePath());
		}
		return true;
	}

	protected void generateAOPXML() throws IOException {
		final List<String> classes = ClazzFinder.getClasses(moduleFolder);
		final File metainf = new File(moduleFolder, "target/test-classes/META-INF");
		metainf.mkdir();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(metainf, "aop.xml")))) {
			// <!DOCTYPE aspectj PUBLIC "-//AspectJ//DTD//EN"
			// "http://www.aspectj.org/dtd/aspectj_1_5_0.dtd">
			writer.write("<!DOCTYPE aspectj PUBLIC \"-//AspectJ//DTD//EN\" \"http://www.aspectj.org/dtd/aspectj_1_5_0.dtd\">\n");
			writer.write("<aspectj>\n");
			writer.write("	<weaver options=\"-verbose\">\n");

			// writer.write(" <include within=\"" + clazz + "..*\" />\n");
			for (final String clazz : classes) {
				writer.write("   <include within=\"" + clazz + "\" />\n");
			}
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
				"surefire:test",
				"-fn",
				"-o",
				"-Dcheckstyle.skip=true",
				"-Dmaven.compiler.source=1.7",
				"-Dmaven.compiler.target=1.7",
				"-Dmaven.javadoc.skip=true",
				"-Denforcer.skip=true" };
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
	 * @param specialResultFolder Folder for saving the results
	 * @param tests Name of the test that should be run
	 */
	public void executeAllTests(final File logFile) {
		try {
			boolean compiled = prepareRunning(logFile);
			if (compiled) {
				final Process process = buildProcess(logFile);
				LOG.info("Starting Process");
				process.waitFor();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean prepareRunning(final File logFile){
		preparePom();
		LOG.debug("Starting Test Transformation");
		prepareTests();
		
		LOG.debug("Starting Compilation");
		boolean compiled = compileVersion(logFile);
		return compiled;
	}

	/**
	 * Runs the given tests and saves the results to the given result folder
	 * 
	 * @param specialResultFolder Folder for saving the results
	 * @param tests Name of the test that should be run
	 */
	public void executeTests(final TestSet tests, final File logFolder) {
		boolean compiled = prepareRunning(new File(logFolder, "log_compilation.txt"));
		if (compiled) {
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
	}

	/**
	 * Runs the given test and saves the results to the result folder.
	 * 
	 * @param specialResultFolder Folder for saving the results
	 * @param testname Name of the test that should be run
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
	 * Prepares the tests by changing their source, so they are executed with KoPeMe.
	 */
	public void prepareTests() {
		testGenerator.transformTests();
	}

	protected boolean testRunning() {
		boolean isRunning;
		final ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "test-compile", "-DskipTests=true", "-Dmaven.test.skip.exec");
		pb.directory(projectFolder);
		try {
			LOG.debug(pb.command());
			final Process process = pb.start();
			final String result = StreamGobbler.getFullProcess(process, true, 5000);
			System.out.println(result);
			process.waitFor();
			final int returncode = process.exitValue();
			if (returncode != 0) {
				isRunning = false;
			} else {
				isRunning = true;
			}
		} catch (final IOException | InterruptedException e) {
			e.printStackTrace();
			isRunning = false;
		}
		return isRunning;
	}

	public boolean isVersionRunning() {
		final File potentialPom = new File(projectFolder, "pom.xml");
		final File testFolder = new File(projectFolder, "src/test");
		LOG.debug(potentialPom);
		boolean isRunning = false;
		if (potentialPom.exists()) {
			if (testFolder.exists()) {
				isRunning = testVersion(potentialPom);
				LOG.debug("pom.xml existing");
				isRunning = testRunning();
				if (isRunning) {
					jdk_version = 8;
				} else {
					final String boot_class_path = System.getenv("BOOT_LIBS");
					if (boot_class_path != null) {
						try {
							final MavenXpp3Reader reader = new MavenXpp3Reader();
							final Model model = reader.read(new FileInputStream(potentialPom));
							if (model.getBuild() == null) {
								model.setBuild(new Build());
							}
							final Plugin compiler = MavenPomUtil.findPlugin(model, MavenKiekerTestExecutor.COMPILER_ARTIFACTID,
									MavenKiekerTestExecutor.ORG_APACHE_MAVEN_PLUGINS);

							MavenPomUtil.extendCompiler(compiler, boot_class_path);
							final MavenXpp3Writer writer = new MavenXpp3Writer();
							writer.write(new FileWriter(potentialPom), model);

							isRunning = testRunning();
							if (isRunning) {
								jdk_version = 6;
							}
						} catch (IOException | XmlPullParserException e) {
							e.printStackTrace();
						}
					}
				}
			} else {

			}
		}

		return isRunning;
	}

	public static void rewriteSurefire(final File pomFile, final String additionalArgLine) {
		try {
			final MavenXpp3Reader reader = new MavenXpp3Reader();
			final Model model = reader.read(new FileInputStream(pomFile));
			if (model.getBuild() == null) {
				model.setBuild(new Build());
			}

			final Plugin surefire = MavenPomUtil.findPlugin(model, MavenKiekerTestExecutor.SUREFIRE_ARTIFACTID, MavenKiekerTestExecutor.ORG_APACHE_MAVEN_PLUGINS);

			MavenPomUtil.extendSurefire(additionalArgLine, surefire, true);
			MavenPomUtil.extendDependencies(model);

			for (final Dependency dependency : model.getDependencies()) {
				if ("junit".equals(dependency.getArtifactId())) {
					dependency.setVersion("4.12");
				}
			}

			final MavenXpp3Writer writer = new MavenXpp3Writer();
			writer.write(new FileWriter(pomFile), model);
		} catch (IOException | XmlPullParserException e1) {
			e1.printStackTrace();
		}
	}

	public void preparePom() {
		preparePom(true);
	}
	
	public void preparePom(boolean update) {
		final File pomFile = new File(moduleFolder, "pom.xml");
		final MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			final Model model = reader.read(new FileInputStream(pomFile));
			if (model.getBuild() == null) {
				model.setBuild(new Build());
			}
			final Plugin surefire = MavenPomUtil.findPlugin(model, SUREFIRE_ARTIFACTID, ORG_APACHE_MAVEN_PLUGINS);

			final Path tempFiles = Files.createTempDirectory("kiekerTemp");
			lastTmpFile = tempFiles.toFile();
			final String argline;
			if (testGenerator.isUseKieker()) {
				argline = KIEKER_ARG_LINE + " -Djava.io.tmpdir=" + tempFiles.toString();
			} else {
				argline = "";
			}

			MavenPomUtil.extendSurefire(argline, surefire, update);
			MavenPomUtil.extendDependencies(model);

			setJDK(model);

			final MavenXpp3Writer writer = new MavenXpp3Writer();
			writer.write(new FileWriter(pomFile), model);

			lastEncoding = MavenPomUtil.getEncoding(model);
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}
	}

	protected boolean testVersion(final File potentialPom) {
		try {
			final MavenXpp3Reader reader2 = new MavenXpp3Reader();
			final Model model2 = reader2.read(new FileInputStream(potentialPom));
			final Properties properties = model2.getProperties();
			if (properties != null) {
				final String source = properties.getProperty("maven.compiler.source");
				final String target = properties.getProperty("maven.compiler.target");
				if (target != null && (target.equals("1.3") || target.equals("1.4"))) {
					return false;
				}
				if (source != null && (source.equals("1.3") || source.equals("1.4"))) {
					return false;
				}
			}
			Plugin compilerPlugin = MavenPomUtil.findPlugin(model2, "maven-compiler-plugin", "org.apache.maven.plugins");
			if (compilerPlugin != null) {
				Xpp3Dom config = (Xpp3Dom) compilerPlugin.getConfiguration();
				if (config != null) {
					Xpp3Dom sourceChild = config.getChild("source");
					Xpp3Dom targetChild = config.getChild("target");
					if (sourceChild != null && targetChild != null) {
						final String source = sourceChild.getValue();
						final String target = targetChild.getValue();
						if (target != null && (target.equals("1.3") || target.equals("1.4") || target.equals("1.5"))) {
							return false;
						}
						if (source != null && (source.equals("1.3") || source.equals("1.4") || target.equals("1.5"))) {
							return false;
						}
					}
				}
			}
			// if (model2.getBuild().getPl)
		} catch (XmlPullParserException | IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public Charset getEncoding() {
		return lastEncoding;
	}

}
