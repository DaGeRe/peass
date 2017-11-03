package de.peran.utils;

/*-
 * #%L
 * peran-measurement
 * %%
 * Copyright (C) 2015 - 2017 DaGeRe
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peran.DependencyStatisticAnalyzer;
import de.peran.DependencyTestPairStarter;
import de.peran.generated.Versiondependencies;
import de.peran.reduceddependency.ChangedTraceTests;

/**
 * Divides the versions of a dependencyfile (and optionally an executionfile) in order to start slurm test executions.
 * 
 * @author reichelt
 *
 */
public class DivideVersions {
	public static void main(final String[] args) throws JAXBException, ParseException, JsonParseException, JsonMappingException, IOException {
		final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE, OptionConstants.EXECUTIONFILE);
		final CommandLineParser parser = new DefaultParser();

		final CommandLine line = parser.parse(options, args);

		final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
		final Versiondependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
		final String url = dependencies.getUrl().replaceAll("\n", "").replaceAll(" ", "");

		final ChangedTraceTests changedTests = DependencyTestPairStarter.loadChangedTests(line);

		System.out.println("timestamp=$(date +%s)");
		for (int i = 0; i < dependencies.getVersions().getVersion().size(); i++) {
			final String endversion = dependencies.getVersions().getVersion().get(i).getVersion();
			// System.out.println("-startversion " + startversion + " -endversion " + endversion);
			if (changedTests == null || (changedTests != null && changedTests.getVersions().containsKey(endversion))) {
				System.out.println(
						"sbatch --nice=1000000 --time=10-0 "
								+ "--output=/newnfs/user/do820mize/processlogs/process_" + i + "_$timestamp.out "
								+ "--workdir=/newnfs/user/do820mize "
								+ "--export=PROJECT=" + url + ",HOME=/newnfs/user/do820mize,START="
								+ endversion + ",END=" + endversion + ",INDEX=" + i + " executeTests.sh");
			}
		}
	}
}
