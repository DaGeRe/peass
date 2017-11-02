package de.peran.evaluation.infinitest;

import java.io.File;
import java.util.List;

import org.infinitest.ClasspathProvider;

/**
 * Mimics StandaloneClasspath from Infinitest.
 * 
 * @author reichelt
 *
 */
public class StandaloneClasspath implements ClasspathProvider {
	private final List<File> classDirs;
	private final String classpath;
	private final List<File> classDirsInClasspath;

	public StandaloneClasspath(final List<File> classOutputDirs, final String classpath) {
		this(classOutputDirs, classOutputDirs);
	}

	public StandaloneClasspath(final List<File> classOutputDirs, final List<File> classDirsInClasspath) {
		classDirs = classOutputDirs;
		this.classDirsInClasspath = classDirsInClasspath;
		if (System.getProperty("surefire.test.class.path") != null) {
			classpath = System.getProperty("surefire.test.class.path");
		} else {
			classpath = System.getProperty("java.class.path");
		}
	}

	@Override
	public List<File> getClassOutputDirs() {
		return classDirs;
	}

	@Override
	public String getCompleteClasspath() {
		return classpath;
	}

	@Override
	public String toString() {
		return "Classpath :[" + classpath + "]  Class Directories: [" + classDirs + "]";
	}

	@Override
	public List<File> classDirectoriesInClasspath() {
		return classDirsInClasspath;
	}

}
