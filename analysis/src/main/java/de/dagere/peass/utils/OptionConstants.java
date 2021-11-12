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
package de.dagere.peass.utils;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Defines options for starting the various processes of the project.
 * @author reichelt
 *
 */
public enum OptionConstants {
   // Generic
	DEPENDENCYFILE("dependencyfile", "Dependencyfile specifying the tests which should be executed", false),

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
