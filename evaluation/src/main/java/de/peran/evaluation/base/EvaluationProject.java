package de.peran.evaluation.base;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Saves the url and the EvaluationVersions for every version or a project.
 * 
 * @author reichelt
 *
 */
public class EvaluationProject {
	private String url;
	private Map<String, EvaluationVersion> versions = new LinkedHashMap<>();

	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public Map<String, EvaluationVersion> getVersions() {
		return versions;
	}

	public void setVersions(final Map<String, EvaluationVersion> versions) {
		this.versions = versions;
	}

	public int getOverallTestCount() {
		int tests = 0;
		for (final Map.Entry<String, EvaluationVersion> test : versions.entrySet()) {
			tests += test.getValue().getTestCount();
		}
		return tests;
	}

}