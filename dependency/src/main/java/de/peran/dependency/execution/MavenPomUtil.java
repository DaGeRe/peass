package de.peran.dependency.execution;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class MavenPomUtil {
	
	private static final Logger LOG = LogManager.getLogger(MavenPomUtil.class);
	
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
	
	public static Charset getEncoding(final Model model) {
		Charset value = StandardCharsets.UTF_8;
		final Properties properties = model.getProperties();
		if (properties != null) {
			final String encoding = (String) properties.get("project.build.sourceEncoding");
			if (encoding != null) {
				if (encoding.equals("ISO-8859-1")) {
					value = StandardCharsets.ISO_8859_1;
				}
			}
		}
		return value;
	}
	
	public static Plugin findPlugin(final Model model, final String artifactId, final String groupId) {
		Plugin surefire = null;
		if (model.getBuild() == null) {
			model.setBuild(new Build());
		}
		if (model.getBuild().getPlugins() == null) {
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

	public static void extendSurefire(final String additionalArgLine, final Plugin plugin, final boolean updateVersion) {
		if (plugin.getConfiguration() == null) {
			plugin.setConfiguration(new Xpp3Dom("configuration"));
		}

		if (updateVersion) {
			System.out.println("Surefire" + plugin.getClass() + " " + plugin.getConfiguration().getClass());
			plugin.setVersion("2.21.0-SNAPSHOT");
		}

		final Xpp3Dom conf = (Xpp3Dom) plugin.getConfiguration();
		MavenPomUtil.addNode(conf, "forkmode", "always");

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
		bootclasspath.setValue(boot_class_path + "/resources.jar${path.separator}" + boot_class_path
				+ "/rt.jar${path.separator}" + boot_class_path + "/sunrsasign.jar:" + boot_class_path
				+ "/jsse.jar${path.separator}" + boot_class_path + "/jce.jar${path.separator}" + boot_class_path
				+ "/charsets.jar${path.separator}" + boot_class_path + "/jfr.jar");

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

}
