package de.peass;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.ExecutionData;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.statistics.DependencyStatistics;
import de.peass.utils.Constants;
import de.peass.utils.OptionConstants;

/**
 * Reads multiple dependency files and prints its statistics.
 * 
 * @author reichelt
 *
 */
public class DependencyStatisticSummarizer {

	public static void main(final String[] args) throws ParseException, JsonParseException, JsonMappingException, IOException, JAXBException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER);

		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		// final File dependenciesFile = new File(args[0]);
		final File folder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));

		System.out.println("Project;Versions;Normal-Tests;SIC;TIC; Tests once changed; Tests multiple times changed");
		for (final File xmlFile : FileUtils.listFiles(folder, new WildcardFileFilter("deps_*.xml"), TrueFileFilter.INSTANCE)) {
			final String projektName = xmlFile.getName().replace("deps_", "").replace(".xml", "");
			final File executeFile = new File(xmlFile.getParentFile(), "views_" + projektName + "/execute" + projektName + ".json");
			
			if (xmlFile.exists() && executeFile.exists()){
				final ExecutionData changedTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);

				final DependencyStatistics statistics = DependencyStatisticAnalyzer.getChangeStatistics(xmlFile, changedTests);

				System.out.println(projektName + ";" + statistics.getSize() + ";" + statistics.getOverallRunTests() + ";" + statistics.getPruningRunTests() + ";" + statistics.getChangedTraceTests() + ";"
						+ statistics.getOnceChangedTests().size() + ";" + statistics.getMultipleChangedTest().size());
			}
		}

		System.out.println("====");
		System.out.println("Project & Versions & Tests & SIC & TIC\\");
		for (final File xmlFile : FileUtils.listFiles(folder, new WildcardFileFilter("deps_*.xml"), TrueFileFilter.INSTANCE)) {
			final String projektName = xmlFile.getName().replace("deps_", "").replace(".xml", "");
			final File executeFile = new File(xmlFile.getParentFile(), "views_" + projektName + "/execute" + projektName + ".json");

			if (xmlFile.exists() && executeFile.exists()) {
				final ExecutionData changedTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);

				final DependencyStatistics statistics = DependencyStatisticAnalyzer.getChangeStatistics(xmlFile, changedTests);

				final double percent = 10000d * statistics.getChangedTraceTests() / statistics.getOverallRunTests();
				System.out.println(percent);
				System.out.println(projektName + " & " + statistics.getSize() + " & " + statistics.getOverallRunTests() + " & " + statistics.getPruningRunTests()+ " & " + statistics.getChangedTraceTests()  + " & " + Math.round(percent)/100d+ " %\\");
				
			}
		}
	}
}
