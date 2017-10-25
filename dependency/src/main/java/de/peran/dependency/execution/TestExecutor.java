package de.peran.dependency.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peran.utils.StreamGobbler;

/**
 * Base functionality for executing tests
 * 
 * @author reichelt
 *
 */
public class TestExecutor {

	private static final Logger LOG = LogManager.getLogger(TestExecutor.class);

	public static final String COMPILER_ARTIFACTID = "maven-compiler-plugin";

	protected final File projectFolder, moduleFolder;
	protected int jdk_version = 8;

	public TestExecutor(final File projectFolder, File moduleFolder) {
		this.projectFolder = projectFolder;
		this.moduleFolder = moduleFolder;
	}

	public void setJDKVersion(final int jdk_version) {
		this.jdk_version = jdk_version;
	}

	public int getJDKVersion() {
		return jdk_version;
	}

	protected void setJDK(final Model model) {
		if (jdk_version != 8) {
			final String boot_class_path = System.getenv("BOOT_LIBS");
			if (boot_class_path != null) {
				final Plugin compiler = findPlugin(model, COMPILER_ARTIFACTID, MavenKiekerTestExecutor.ORG_APACHE_MAVEN_PLUGINS);
				extendCompiler(compiler, boot_class_path);
			}
		}
	}

	public static void extendCompiler(final Plugin plugin, final String boot_class_path) {
		if (boot_class_path == null || !new File(boot_class_path).exists()) {
			throw new RuntimeException("Boot-Classpath " + boot_class_path + " is not defined.");
		}
		if (plugin.getConfiguration() == null) {
			plugin.setConfiguration(new Xpp3Dom("configuration"));
		}
		System.out.println("Compiler" + plugin.getClass() + " " + plugin.getConfiguration().getClass());
		plugin.setVersion("3.6.1");

		LOG.info("BOOT_LIBS: {}", boot_class_path);

		final Xpp3Dom conf = (Xpp3Dom) plugin.getConfiguration();

		final Xpp3Dom compilerArguments = findChild(conf, "compilerArguments");
		final Xpp3Dom bootclasspath = findChild(compilerArguments, "bootclasspath");
		bootclasspath.setValue(boot_class_path + "/resources.jar${path.separator}" + boot_class_path + "/rt.jar${path.separator}" + boot_class_path + "/sunrsasign.jar:"
				+ boot_class_path + "/jsse.jar${path.separator}" + boot_class_path + "/jce.jar${path.separator}" + boot_class_path + "/charsets.jar${path.separator}"
				+ boot_class_path + "/jfr.jar");

	}

	public static void setIncrementalBuild(final Plugin plugin, final boolean build) {
		if (plugin.getConfiguration() == null) {
			plugin.setConfiguration(new Xpp3Dom("configuration"));
		}
		System.out.println("Compiler" + plugin.getClass() + " " + plugin.getConfiguration().getClass());
		plugin.setVersion("3.6.1");

		final Xpp3Dom conf = (Xpp3Dom) plugin.getConfiguration();

		final Xpp3Dom compilerArguments = findChild(conf, "useIncrementalCompilation");
		compilerArguments.setValue("" + build);

	}

	private static Xpp3Dom findChild(final Xpp3Dom conf, final String name) {
		Xpp3Dom compilerArguments = conf.getChild(name);
		if (compilerArguments == null) {
			compilerArguments = new Xpp3Dom(name);
			conf.addChild(compilerArguments);
		}
		return compilerArguments;
	}

	protected static Xpp3Dom addNode(final Xpp3Dom conf, final String nodeName, final String value) {
		Xpp3Dom forkMode = conf.getChild(nodeName);
		if (forkMode != null) {
			forkMode.setValue(value); // Wir brauchen once/always, damit die
										// Kieker-Optionen gesetzt werden
		} else if (forkMode == null) {
			forkMode = new Xpp3Dom(nodeName);
			forkMode.setValue(value);
			conf.addChild(forkMode);
		}
		return forkMode;
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
							final Plugin compiler = MavenKiekerTestExecutor.findPlugin(model, MavenKiekerTestExecutor.COMPILER_ARTIFACTID,
									MavenKiekerTestExecutor.ORG_APACHE_MAVEN_PLUGINS);

							MavenKiekerTestExecutor.extendCompiler(compiler, boot_class_path);
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
			Plugin compilerPlugin = findPlugin(model2, "maven-compiler-plugin", "org.apache.maven.plugins");
			if (compilerPlugin != null) {
				Xpp3Dom config = (Xpp3Dom) compilerPlugin.getConfiguration();
				if (config != null){
					final String source = config.getChild("source").getValue();
					final String target = config.getChild("target").getValue();
					if (target != null && (target.equals("1.3") || target.equals("1.4") || target.equals("1.5"))) {
						return false;
					}
					if (source != null && (source.equals("1.3") || source.equals("1.4")|| target.equals("1.5"))) {
						return false;
					}
				}
			}
			// if (model2.getBuild().getPl)
		} catch (XmlPullParserException | IOException e) {
			e.printStackTrace();
		}
		return true;
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

	public static Plugin findPlugin(final Model model, final String artifactId, final String groupId) {
		Plugin surefire = null;
		if (model.getBuild() == null){
			model.setBuild(new Build());
		}
		if (model.getBuild().getPlugins() == null){
			model.getBuild().setPlugins(new LinkedList<Plugin>());
		}
		for (final Plugin plugin : model.getBuild().getPlugins()) {
			if (plugin.getArtifactId().equals(artifactId) && plugin.getGroupId().equals(groupId)) {
				surefire = plugin;
				break;
			}
		}
		if (surefire == null) {
			surefire = new Plugin();
			surefire.setArtifactId(artifactId);
			surefire.setGroupId(groupId);
			model.getBuild().getPlugins().add(surefire);
		}
		return surefire;
	}

}
