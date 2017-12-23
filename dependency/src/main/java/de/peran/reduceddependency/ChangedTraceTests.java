package de.peran.reduceddependency;

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

	public static class OldVersionDeserializer extends StdDeserializer<ChangedTraceTests> {

		private static final long serialVersionUID = 1L;

		public OldVersionDeserializer() {
			this(null);
		}

		public OldVersionDeserializer(Class<?> vc) {
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
	
	public Map<String, TestSet> getVersions() {
		return versions;
	}

	@JsonIgnore
	public void addCall(final String version, final TestSet tests) {
		TestSet executes = versions.get(version);
		if (executes == null) {
			versions.put(version, tests);
		} else {
			executes.addTestSet(tests);
		}
	}

	@JsonIgnore
	public void addCall(final String version, final TestCase testcase) {
		TestSet executes = versions.get(version);
		if (executes == null) {
			executes = new TestSet();
			versions.put(version, executes);
		}
		executes.addTest(testcase.getClazz(), testcase.getMethod());
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