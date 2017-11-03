package de.peran.reduceddependency;

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


import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestSet;

/**
 * Saves all tests where traces have changed and therefore a performance change could have taken place.
 * 
 * Used for JSON Serialisation.
 * 
 * @author reichelt
 *
 */
public class ChangedTraceTests {

	public static class Deserializer extends StdDeserializer<ChangedTraceTests> {

		private static final long serialVersionUID = 1L;

		public Deserializer() {
			this(null);
		}

		public Deserializer(Class<?> vc) {
			super(vc);
		}

		@Override
		public ChangedTraceTests deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
			ChangedTraceTests tests = new ChangedTraceTests();
			JsonNode root = p.getCodec().readTree(p);
			JsonNode versions = root.get("versions");
			for (Iterator<Entry<String, JsonNode>> iterator = versions.fields(); iterator.hasNext();) {
				Entry<String, JsonNode> value = iterator.next();
				String version = value.getKey();
				value.getValue().forEach(testInChild -> {
					TestSet testcase = new TestSet();
					String clazz = testInChild.get("testclazz").asText();
					JsonNode methods = testInChild.get("testmethod");
					methods.forEach(method -> {
						testcase.addTest(clazz, method.asText());
					});
					tests.addCall(version, testcase);
				});
			}
			return tests;
		}

	}

	private Map<String, TestSet> versions = new LinkedHashMap<>();

	public void setVersions(Map<String, TestSet> versions) {
		this.versions = versions;
	}

	public void addCall(final String version, final TestSet tests) {
		TestSet executes = versions.get(version);
		if (executes == null) {
			versions.put(version, tests);
		} else {
			executes.addTestSet(tests);
		}
	}

	public void addCall(final String version, final TestCase testcase) {
		TestSet executes = versions.get(version);
		if (executes == null) {
			executes = new TestSet();
			versions.put(version, executes);
		}
		executes.addTest(testcase.getClazz(), testcase.getMethod());
	}

	public Map<String, TestSet> getVersions() {
		return versions;
	}

	@JsonIgnore
	public boolean versionContainsTest(final String version, final TestCase currentIterationTest) {
		final TestSet clazzExecutions = versions.get(version);
		if (clazzExecutions != null) {
			for (final Map.Entry<String, List<String>> clazz : clazzExecutions.entrySet()) {
				String testclazz = clazz.getKey();
				List<String> methods = clazz.getValue();
				if (testclazz.equals(currentIterationTest.getClazz())) {
					if (methods.contains(currentIterationTest.getMethod())) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
