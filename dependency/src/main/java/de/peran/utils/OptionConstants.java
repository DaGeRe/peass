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
package de.peran.utils;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Defines options for starting the various processes of the project.
 * @author reichelt
 *
 */
public enum OptionConstants {

	STARTVERSION("startversion", "Version, where the process should be started", false),
	ENDVERSION("endversion", "Version, where the process should be finished", false),
	FOLDER("folder", "Folder of the project that should be analyzed", true),
	ITERATIONS("iterationen", "Iterationen, wie oft jeder Testfall ausgeführt werden soll", false),
	WARMUP("warmup", "Iterationen, wie oft jeder Testfall als Aufwärm-Ausführung ausgeführt werden soll", false),
	DURATION("duration", "Test duration, if a timebased testcase should be used", false),
	REPETITIONS("repetitions", "Count every testcase should be repeated", false),
	DEPENDENCYFILE("dependencyfile", "Dependency-Datei, aus der die auzuführenden Tests auszulesen sind", false),
	OUT("out", "Folder for output", false),
	INITIAL("initial", "Whether the initial version should be tested", false), 
	VMS("vms", "Count of virtual machines that should be started (after each other, in order to get reliable test results)", false),
	EXECUTIONFILE("executionfile", "JSON-file telling which tests to execute in each version", false),
	FULLRESULTFOLDER("fullresultfolder", "Folder to save the full results of the tests", false), 
	TEST("test", "the test to execute", true),
	MODULE("module", "the module to research", false),
	CHANGEFILE("changefile", "changefile for processing", false);

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
