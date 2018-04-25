package de.peran.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.peran.DependencyReadingStarter;
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.reduceddependency.ChangedTraceTests;

public class GetNonSelectedTests {

	private static final Logger LOG = LogManager.getLogger(GetNonSelectedTests.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();
	static {
		MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		final File allTestsFile = new File(args[0]);
		final File selectedTestsFile = new File(args[1]);

		final ChangedTraceTests all = MAPPER.readValue(allTestsFile, ChangedTraceTests.class);
		final ChangedTraceTests selected = MAPPER.readValue(selectedTestsFile, ChangedTraceTests.class);

		final ChangedTraceTests nonSelected = new ChangedTraceTests();
		nonSelected.setUrl(all.getUrl());
		for (final Map.Entry<String, TestSet> allVersion : all.getVersions().entrySet()) {
			LOG.debug("Version: {}", allVersion.getKey());
			final TestSet allTestSet = allVersion.getValue();
			if (selected.getVersions().containsKey(allVersion.getKey())) {
				final TestSet selectedVersion = selected.getVersions().get(allVersion.getKey());
				for (final Map.Entry<ChangedEntity, List<String>> selectedTests : selectedVersion.getTestcases().entrySet()) {
					final String testclazz = selectedTests.getKey().getJavaClazzName();
					final List<String> remainingTests = allTestSet.getTestcases().get(testclazz);
					System.out.println("Searching: " + testclazz + " " + remainingTests);
					if (remainingTests != null) { //TODO Workaround: Test-suite and extended tests are also selected, but shouldn't
						if (!remainingTests.containsAll(selectedTests.getValue())) {
							LOG.error("Testclazz " + testclazz);
							LOG.error("Selected contain more than all: " + remainingTests + " " + selectedTests.getValue());
						}

						remainingTests.removeAll(selectedTests.getValue());
						if (remainingTests.size() == 0) {
							allTestSet.getTestcases().remove(testclazz);
						}
					}
				}
			}
			LOG.debug("Size: " + allTestSet.size());
			nonSelected.getVersions().put(allVersion.getKey(), allTestSet);
		}
		MAPPER.writeValue(new File(DependencyReadingStarter.getResultFolder(), "out.json"), nonSelected);
	}
}
