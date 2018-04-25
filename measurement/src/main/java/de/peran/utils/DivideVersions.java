package de.peran.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.generated.Versiondependencies;
import de.peran.reduceddependency.ChangedTraceTests;
import de.peran.statistics.DependencyStatisticAnalyzer;

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

		final ChangedTraceTests changedTests = TestLoadUtil.loadChangedTests(line);

		System.out.println("timestamp=$(date +%s)");
		for (int i = 0; i < dependencies.getVersions().getVersion().size(); i++) {
			final String endversion = dependencies.getVersions().getVersion().get(i).getVersion();
			// System.out.println("-startversion " + startversion + " -endversion " + endversion);
			if (changedTests == null) {
				System.out.println(
						"sbatch --nice=1000000 --time=10-0 "
								+ "--output=/nfs/user/do820mize/processlogs/process_" + i + "_$timestamp.out "
								+ "--workdir=/nfs/user/do820mize "
								+ "--export=PROJECT=" + url + ",HOME=/newnfs/user/do820mize,START="
								+ endversion + ",END=" + endversion + ",INDEX=" + i + " executeTests.sh");
			} else if (changedTests != null && changedTests.getVersions().containsKey(endversion)) {
				for (final Map.Entry<ChangedEntity, List<String>> testcase : changedTests.getVersions().get(endversion).getTestcases().entrySet()) {
					for (final String method : testcase.getValue()) {
						System.out.println(
								"sbatch --nice=1000000 --time=10-0 "
										+ "--output=/nfs/user/do820mize/processlogs/process_" + i + "_" + method + "_$timestamp.out "
										+ "--workdir=/nfs/user/do820mize "
										+ "--export=PROJECT=" + url + ",HOME=/newnfs/user/do820mize,"
										+ "START=" + endversion + ","
										+ "END=" + endversion + ","
										+ "INDEX=" + i + ","
										+ "TEST=" + testcase.getKey().getJavaClazzName() + "#" + method + " executeTests.sh");
					}
				}
			}
		}
	}
}
