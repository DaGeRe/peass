package de.peran.evaluation.infinitest;

/*-
 * #%L
 * peran-evaluation
 * %%
 * Copyright (C) 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


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
