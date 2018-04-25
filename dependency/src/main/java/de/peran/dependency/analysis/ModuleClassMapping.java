package de.peran.dependency.analysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peran.dependency.ClazzFinder;
import de.peran.dependency.execution.MavenPomUtil;

public class ModuleClassMapping {

	private static final Logger LOG = LogManager.getLogger(ModuleClassMapping.class);
	
	private ModuleClassMapping() {

	}

	private static final Map<String, String> mapping = new HashMap<>();

	public static String getModuleOfClass(String clazz) {
		return mapping.get(clazz);
	}

	public static void loadClasses(File folder) {
		try {
			for (final File module : MavenPomUtil.getModules(new File(folder, "pom.xml"))) {
				LOG.debug("Module: {}", module.getAbsolutePath());
				final List<String> classes = ClazzFinder.getClasses(module);
				String moduleName;
				if (module.equals(folder)) {
					moduleName = "";
				} else {
					final int pathIndex = folder.getAbsolutePath().length()+1;
					moduleName = module.getAbsolutePath().substring(pathIndex);
				}
				for (final String clazz : classes) {
					mapping.put(clazz, moduleName);
				}
			}
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}
	}
}
