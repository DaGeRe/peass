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
package de.peran.dependency.changes;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holds data of the difference between two versions, i.e. which classes are changed and whether the pom has changed or not.
 * @author reichelt
 *
 */
public class VersionDiff {

	private static final Logger LOG = LogManager.getLogger(VersionDiff.class);

	private boolean pomChanged;
	private final List<String> changedClasses;

	public VersionDiff() {
		changedClasses = new LinkedList<>();
		pomChanged = false;
	}

	/**
	 * @return the pomChanged
	 */
	public boolean isPomChanged() {
		return pomChanged;
	}

	/**
	 * @param pomChanged the pomChanged to set
	 */
	public void setPomChanged(final boolean pomChanged) {
		this.pomChanged = pomChanged;
	}

	public List<String> getChangedClasses() {
		return changedClasses;
	}

	/**
	 * @param changedClasses the changedClasses to set
	 */
	public void addChangedClass(final String changedClass) {
		changedClasses.add(changedClass);
	}

	public void addChange(final String currentFileName) {
		if (currentFileName.endsWith("pom.xml")) {
			setPomChanged(true);
		} else {
			if (currentFileName.endsWith(".java")) {
				final int indexOf = currentFileName.indexOf("src");
				if (indexOf == -1) {
					LOG.error("Index von src nicht gefunden: " + currentFileName);
				} else {
					final String classPath = currentFileName.substring(indexOf);
					addChangedClass(classPath);
				}
			}
		}
	}

	@Override
	public String toString() {
		String ret = "Pom: " + pomChanged + " Klassen: ";
		for (final String cl : changedClasses) {
			ret += cl + "\n";
		}
		return ret;
	}
}