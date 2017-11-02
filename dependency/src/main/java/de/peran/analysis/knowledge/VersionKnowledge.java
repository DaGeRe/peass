package de.peran.analysis.knowledge;

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
