/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peass.utils;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Defines options for starting the various processes of the project.
 * @author reichelt
 *
 */
public enum OptionConstants {
   // Generic
	STARTVERSION("startversion", "Version, where the process should be started", false),
	ENDVERSION("endversion", "Version, where the process should be finished", false),
	VERSION("version", "Only version execute", false),
	FOLDER("folder", "Folder of the project that should be analyzed", true),
	DEPENDENCYFILE("dependencyfile", "Dependencyfile specifying the tests which should be executed", false),
	EXECUTIONFILE("executionfile", "JSON-file telling which tests to execute in each version", false),
	VIEWFOLDER("viewfolder", "Viewfolder containing th views of the json", true),
	USE_SLURM("useslurm", "Whether to use slurm or simple bash execution files", false),
	OUT("out", "Folder for output", false),
	FULLRESULTFOLDER("fullresultfolder", "Folder to save the full results of the tests", false), 
	CHANGEFILE("changefile", "changefile for processing", false),
	KNOWLEDGEFILE("knowledgefile", "knowledgefile for processing", false),
	// Measurement
	TIMEOUT("timeout", "Timeout, after that one test is finished in minutes.", false),
	DURATION("duration", "Test duration, if a timebased testcase should be used", false),
   USEKIEKER("usekieker", "Whether to instrument the tests with kieker (slows down execution, but delivers kieker traces)", false),
   ITERATIONS("iterations", "Number of measurement iterations", false),
   WARMUP("warmup", "Number of warmup iterations", false),
   VMS("vms", "Count of virtual machines that should be started (after each other, in order to get reliable test results)", false),
   REPETITIONS("repetitions", "Count every testcase should be repeated", false),
   TEST("test", "the test to execute", false),
	
	// Analysis
   DATA("data", "Data for processing", false),
	CONFIDENCE("confidence", "Confidence level for analysis with t-test, default 0.01", false);
	

	private final String name, description;
	private final boolean required;

	OptionConstants(final String name, final String description, final boolean required) {
		this.name = name;
		this.description = description;
		this.required = required;
	}

	public static Options createOptions(final OptionConstants... optionNames) {
		final Options options = new Options();

		for (final OptionConstants constant : optionNames) {
			final Option option = Option.builder(constant.name)
					.required(constant.required)
					.hasArg()
					.desc(constant.description)
					.build();
			options.addOption(option);
		}

		return options;
	}

	public String getName() {
		return name;
	}
}
