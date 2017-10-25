package de.peran.debugtools;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.peran.dependency.analysis.data.TestSet;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.utils.OptionConstants;

public class GetExecutionCount {
	public static void main(String[] args) throws ParseException, JsonParseException, IOException {
		final Options options = OptionConstants.createOptions(OptionConstants.EXECUTIONFILE);
		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);
		
		ChangedTraceTests changedTests;
		
		final ObjectMapper mapper = new ObjectMapper();
		ChangedTraceTests testsTemp;
		try {
			testsTemp = mapper.readValue(new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName())), ChangedTraceTests.class);
		} catch (JsonMappingException e) {
			ObjectMapper objectMapper = new ObjectMapper();
			SimpleModule module = new SimpleModule();
			module.addDeserializer(ChangedTraceTests.class, new ChangedTraceTests.Deserializer());
			objectMapper.registerModule(module);
			testsTemp = objectMapper.readValue(new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName())), ChangedTraceTests.class);
		}
		changedTests = testsTemp;
		
		int overall = 0;
		for (Map.Entry<String, TestSet> entry : changedTests.getVersions().entrySet()){
			
			int count = 0;
			for (Entry<String, java.util.List<String>> testcase : entry.getValue().entrySet()){
				count += testcase.getValue().size();
			}
			System.out.println(entry.getKey()+ ": " + count);
			overall+=count;
		}
		System.out.println("All: " + overall);
	}
}
