package de.peass.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.reader.DependencyReaderBase;

public class TestLoadUtil {
   
	public static ExecutionData loadChangedTests(final CommandLine line) throws IOException, JsonParseException, JsonMappingException {
		final ExecutionData changedTests;
		if (line.hasOption(OptionConstants.EXECUTIONFILE.getName())) {
			
			ExecutionData testsTemp;
			final File executionFile = new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName()));
			if (!executionFile.exists()){
				throw new RuntimeException("Executionfile needs to exist");
			}
			try {
				testsTemp = DependencyReaderBase.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
			} catch (final JsonMappingException e) {
				e.printStackTrace();
				testsTemp = null;
//				final ObjectMapper objectMapper = new ObjectMapper();
//				final SimpleModule module = new SimpleModule();
//				module.addDeserializer(ChangedTraceTests.class, new ChangedTraceTests.OldVersionDeserializer());
//				objectMapper.registerModule(module);
//				testsTemp = objectMapper.readValue(executionFile, ChangedTraceTests.class);
			}
			changedTests = testsTemp;
		} else {
			changedTests = null;
		}
		return changedTests;
	}
}
