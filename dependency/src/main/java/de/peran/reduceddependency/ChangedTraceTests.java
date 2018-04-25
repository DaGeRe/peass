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

import de.peran.dependency.analysis.data.ChangedEntity;
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
			final ChangedTraceTests tests = new ChangedTraceTests();
			final JsonNode root = p.getCodec().readTree(p);
			final JsonNode versions = root.get("versions");
			for (final Iterator<Entry<String, JsonNode>> iterator = versions.fields(); iterator.hasNext();) {
				final Entry<String, JsonNode> value = iterator.next();
				final String version = value.getKey();
				value.getValue().forEach(testInChild -> {
					final TestSet testcase = new TestSet();
					final String clazz = testInChild.get("testclazz").asText();
					final JsonNode methods = testInChild.get("testmethod");
					methods.forEach(method -> {
						final ChangedEntity entity = new ChangedEntity(clazz, "");
						testcase.addTest(entity, method.asText());
					});
					tests.addCall(version, testcase);
				});
			}
			return tests;
		}

	}

	private String url;

	private Map<String, TestSet> versions = new LinkedHashMap<>();

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setVersions(Map<String, TestSet> versions) {
		this.versions = versions;
	}

	public Map<String, TestSet> getVersions() {
		return versions;
	}

	@JsonIgnore
	public void addCall(final String version, final TestSet tests) {
		final TestSet executes = versions.get(version);
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
		executes.addTest(testcase);
	}

	@JsonIgnore
	public boolean versionContainsTest(final String version, final TestCase currentIterationTest) {
		final TestSet clazzExecutions = versions.get(version);
		if (clazzExecutions != null) {
			for (final Map.Entry<ChangedEntity, List<String>> clazz : clazzExecutions.entrySet()) {
				final ChangedEntity testclazz = clazz.getKey();
				final List<String> methods = clazz.getValue();
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