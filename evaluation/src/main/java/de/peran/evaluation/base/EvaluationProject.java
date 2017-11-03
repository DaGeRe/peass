package de.peran.evaluation.base;

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
