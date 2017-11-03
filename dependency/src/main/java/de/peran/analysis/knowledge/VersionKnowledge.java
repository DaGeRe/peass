package de.peran.analysis.knowledge;

/*-
 * #%L
 * peran-dependency
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


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.peran.dependencyprocessors.VersionComparator;

public class VersionKnowledge {
	
	// Metadata for analysis
	private int versions;
	private int changes;
	private int testcases;
	
	public int getVersions() {
		return versions;
	}

	public void setVersions(int versions) {
		this.versions = versions;
	}

	public int getChanges() {
		return changes;
	}

	public void setChanges(int changes) {
		this.changes = changes;
	}

	public int getTestcases() {
		return testcases;
	}

	public void setTestcases(int testcases) {
		this.testcases = testcases;
	}

	private Map<String, Changes> versionChanges = VersionComparator.hasDependencies() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

	public Map<String, Changes> getVersionChanges() {
		return versionChanges;
	}

	public void setVersionChanges(Map<String, Changes> versionChanges) {
		this.versionChanges = versionChanges;
	}

	@JsonIgnore
	public Changes getVersion(String key) {
		Changes result = versionChanges.get(key);
		if (result == null){
			result = new Changes();
			versionChanges.put(key, result);
		}
		return result;
	}
}
