package de.peran.evaluation.infinitest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.infinitest.changedetect.FileChangeDetector;
import org.infinitest.parser.ClassFileIndex;
import org.infinitest.parser.JavaClass;

import de.peran.dependency.execution.MavenKiekerTestExecutor;
import de.peran.dependency.execution.MavenPomUtil;
import de.peran.dependency.execution.TestExecutor;
import de.peran.evaluation.base.EvaluationVersion;
import de.peran.evaluation.base.Evaluator;

/**
 * Runs all the tests of all versions with infinitest in order to determine the count of tests infinitest would run
 * @author reichelt
 *
 */
public class InfinitestEvaluator extends Evaluator {

	private static final Logger LOG = LogManager.getLogger(InfinitestEvaluator.class);

	public InfinitestEvaluator(final File projectFolder) {
		super(projectFolder, "infinitest");
	}

	@Override
	public void evaluate() {
		final List<File> classPath = Arrays.asList(new File(projectFolder, "target/classes"), new File(projectFolder, "target/test-classes"));
		final File pomFile = new File(projectFolder, "pom.xml");
		final StandaloneClasspath classpath = new StandaloneClasspath(classPath, classPath);
		final FileChangeDetector changeDetector = new FileChangeDetector();
		final File resultFile = new File(resultFolder, "evaluation_" + projectFolder.getName() + "_infinitest.json");

		changeDetector.setClasspathProvider(classpath);

		final ClassFileIndex index = new ClassFileIndex(classpath);
		final MavenXpp3Reader reader = new MavenXpp3Reader();
		int i = 0;
		while (iterator.hasNextCommit()) {
			iterator.goToNextCommit();

			if (pomFile.exists()) {
				try {
					final Model model = reader.read(new FileInputStream(pomFile));
					if (model.getBuild() == null) {
						model.setBuild(new Build());
					}
					final Plugin compiler = MavenPomUtil.findPlugin(model, MavenKiekerTestExecutor.COMPILER_ARTIFACTID, MavenKiekerTestExecutor.ORG_APACHE_MAVEN_PLUGINS);
					MavenPomUtil.setIncrementalBuild(compiler, false);

					final MavenXpp3Writer writer = new MavenXpp3Writer();
					writer.write(new FileWriter(pomFile), model);

					final ProcessBuilder pb = new ProcessBuilder(new String[] { "mvn", "compile", "test-compile" });
					pb.directory(projectFolder);

					pb.start().waitFor();

					final Set<JavaClass> changedClasses = getChangedClasses(changeDetector, index);

					System.out.println("All changes: " + changedClasses);

					String testname = "";
					final EvaluationVersion currentVersion = new EvaluationVersion();
					for (final Iterator<JavaClass> clazzIterator = changedClasses.iterator(); clazzIterator.hasNext();) {
						final JavaClass clazz = clazzIterator.next();
						if (!clazz.isATest()) {
							clazzIterator.remove();
						} else {
							currentVersion.getTestcaseExecutions().put(clazz.getName(), 0);
							testname += clazz.getName() + ",";
						}
					}

					if (currentVersion.getTestcaseExecutions().size() > 0) {
						testname = testname.substring(0, testname.length() - 1);
						final File currentFile = new File(debugFolder, "myResult" + i + "_" + iterator.getTag() + ".txt");

						executor.executeTests(currentFile, testname);

						final EvaluationVersion adjustedVersion = getTestsFromFile(currentFile);
						evaluation.getVersions().put(iterator.getTag(), adjustedVersion);
						OBJECTMAPPER.writeValue(resultFile, evaluation);
					}

					System.out.println("Tests 2: " + changedClasses);
				} catch (final Exception e) {
					e.printStackTrace();
				}

			}
			i++;
		}

	}

	private Set<JavaClass> getChangedClasses(final FileChangeDetector changeDetector, final ClassFileIndex index) throws IOException {
		final Set<File> changedFiles = changeDetector.findChangedFiles();
		System.out.println("Changes: " + changedFiles);
		final Set<JavaClass> changedClasses = index.findClasses(changedFiles);
		final Set<JavaClass> changedParents = index.findChangedParents(changedClasses);

		// combine two sets
		changedClasses.addAll(changedParents);
		return changedClasses;
	}
	
	public static void main(final String[] args) throws IOException, XmlPullParserException, InterruptedException {
		final InfinitestEvaluator evaluator = new InfinitestEvaluator(new File(args[0]));
		evaluator.evaluate();
	}

}
