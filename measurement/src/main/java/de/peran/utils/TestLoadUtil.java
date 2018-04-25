package de.peran.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.peran.reduceddependency.ChangedTraceTests;

public class TestLoadUtil {
	public static ChangedTraceTests loadChangedTests(final CommandLine line) throws IOException, JsonParseException, JsonMappingException {
		final ChangedTraceTests changedTests;
		if (line.hasOption(OptionConstants.EXECUTIONFILE.getName())) {
			final ObjectMapper mapper = new ObjectMapper();
			ChangedTraceTests testsTemp;
			final File executionFile = new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName()));
			if (!executionFile.exists()){
				throw new RuntimeException("Executionfile needs to exist");
			}
			try {
				testsTemp = mapper.readValue(executionFile, ChangedTraceTests.class);
			} catch (final JsonMappingException e) {
				e.printStackTrace();
				final ObjectMapper objectMapper = new ObjectMapper();
				final SimpleModule module = new SimpleModule();
				module.addDeserializer(ChangedTraceTests.class, new ChangedTraceTests.OldVersionDeserializer());
				objectMapper.registerModule(module);
				testsTemp = objectMapper.readValue(executionFile, ChangedTraceTests.class);
			}
			changedTests = testsTemp;
		} else {
			changedTests = null;
		}
		return changedTests;
	}
}
