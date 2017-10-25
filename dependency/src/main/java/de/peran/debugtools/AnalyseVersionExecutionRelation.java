package de.peran.debugtools;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.javaparser.utils.Log;

import de.peran.DependencyReadingStarter;
import de.peran.DependencyStatisticAnalyzer;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.utils.OptionConstants;
import de.peran.vcs.GitCommit;
import de.peran.vcs.GitUtils;

public class AnalyseVersionExecutionRelation {
	private static final Logger LOG = LogManager.getLogger(AnalyseVersionExecutionRelation.class);

	public static void main(String[] args) throws ParseException, JsonParseException, IOException, JAXBException {
		final Options options = OptionConstants.createOptions(OptionConstants.EXECUTIONFILE, OptionConstants.FOLDER,
				OptionConstants.DEPENDENCYFILE);
		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		ChangedTraceTests changedTests;

		final ObjectMapper mapper = new ObjectMapper();
		ChangedTraceTests testsTemp;
		try {
			testsTemp = mapper.readValue(new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName())),
					ChangedTraceTests.class);
		} catch (JsonMappingException e) {
			ObjectMapper objectMapper = new ObjectMapper();
			SimpleModule module = new SimpleModule();
			module.addDeserializer(ChangedTraceTests.class, new ChangedTraceTests.Deserializer());
			objectMapper.registerModule(module);
			testsTemp = objectMapper.readValue(new File(line.getOptionValue(OptionConstants.EXECUTIONFILE.getName())),
					ChangedTraceTests.class);
		}
		changedTests = testsTemp;

		final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
		final File dependenciesFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
		final Versiondependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependenciesFile);

		List<GitCommit> commits = GitUtils.getCommits(projectFolder);

		Iterator<String> ticIt = changedTests.getVersions().keySet().iterator();
		String currentTic = ticIt.next();

		Iterator<Version> versionIt = dependencies.getVersions().getVersion().iterator();
		Version currenVersion = versionIt.next();
		for (GitCommit commit : commits) {
			if (currenVersion.getVersion().equals(commit.getTag())) {
				LOG.debug("Found: {}", currenVersion.getVersion());
				if (currentTic.equals(commit.getTag())){
					LOG.debug("Equal");
					currentTic = ticIt.next();
				}
				if (versionIt.hasNext()) {
					currenVersion = versionIt.next();
				}
			} else {
				if (currentTic.equals(commit.getTag())){
					LOG.debug("Found, but not in dependencies: {}", currentTic);
					currentTic = ticIt.next();
				}
				// LOG.debug("Not found: {}", commit.getTag());
			}
		}

		// int overall = 0;
		// for (Map.Entry<String, TestSet> entry :
		// changedTests.getVersions().entrySet()){
		//
		// int count = 0;
		// for (Entry<String, java.util.List<String>> testcase :
		// entry.getValue().entrySet()){
		// count += testcase.getValue().size();
		// }
		// System.out.println(entry.getKey()+ ": " + count);
		// overall+=count;
		// }
		// System.out.println("All: " + overall);
	}
}
