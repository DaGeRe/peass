package de.peran;

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
import com.fasterxml.jackson.databind.ObjectMapper;

import de.peran.DependencyStatisticAnalyzer.Statistics;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.utils.OptionConstants;

/**
 * Reads multiple dependency files and prints its statistics.
 * @author reichelt
 *
 */
public class DependencyStatisticSummarizer {
	private static final ObjectMapper mapper = new ObjectMapper();

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
				final ChangedTraceTests changedTests = mapper.readValue(executeFile, ChangedTraceTests.class);

				final Statistics statistics = DependencyStatisticAnalyzer.getChangeStatistics(xmlFile, changedTests);

				System.out.println(projektName + ";" + statistics.size + ";" + statistics.overallRunTests + ";" + statistics.pruningRunTests + ";" + statistics.changedTraceTests + ";"
						+ statistics.onceChangedTests.size() + ";" + statistics.multipleChangedTest.size());
			}
		}

		System.out.println("====");
		System.out.println("Project & Versions & Tests & SIC & TIC\\");
		for (final File xmlFile : FileUtils.listFiles(folder, new WildcardFileFilter("deps_*.xml"), TrueFileFilter.INSTANCE)) {
			final String projektName = xmlFile.getName().replace("deps_", "").replace(".xml", "");
			final File executeFile = new File(xmlFile.getParentFile(), "views_" + projektName + "/execute" + projektName + ".json");

			if (xmlFile.exists() && executeFile.exists()) {
				final ChangedTraceTests changedTests = mapper.readValue(executeFile, ChangedTraceTests.class);

				final Statistics statistics = DependencyStatisticAnalyzer.getChangeStatistics(xmlFile, changedTests);

				System.out.println(projektName + " & " + statistics.size + " & " + statistics.overallRunTests + " & " + statistics.pruningRunTests + " & " + statistics.changedTraceTests + "\\");
			}
		}
	}
}
