package de.peran;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.peran.dependency.PeASSFolderUtil;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependencyprocessors.DependencyTester;
import de.peran.utils.OptionConstants;

/**
 * Remeasures one testcase for the given versions
 * 
 * @author reichelt
 *
 */
public class RemeasureOneTest {
	public static void main(final String[] args) throws ParseException, IOException, InterruptedException, JAXBException {
		final Options options = OptionConstants.createOptions(OptionConstants.REPETITIONS, OptionConstants.VMS, OptionConstants.DURATION, OptionConstants.DURATION,
				OptionConstants.TEST, OptionConstants.ENDVERSION, OptionConstants.FOLDER);
		final CommandLineParser parser = new DefaultParser();

		CommandLine line = parser.parse(options, args);

		String test = line.getOptionValue(OptionConstants.TEST.getName());
		File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
		PeASSFolderUtil.setProjectFolder(projectFolder);
		final int vms = Integer.parseInt(line.getOptionValue(OptionConstants.VMS.getName(), "15"));
		final int repetitions = Integer.parseInt(line.getOptionValue(OptionConstants.REPETITIONS.getName(), "100"));
		final int duration = Integer.parseInt(line.getOptionValue(OptionConstants.DURATION.getName(), "60000"));

		String endversion = line.getOptionValue(OptionConstants.ENDVERSION.getName(), null);

		DependencyTester tester = new DependencyTester(projectFolder, projectFolder, duration, vms, true, repetitions, false);

		TestCase testcase = new TestCase(test);

		File logFolder = new File(PeASSFolderUtil.getLogFolder(), endversion + "_remeasure");
		logFolder.mkdirs();
		for (int i = 0; i < vms; i++) {
			TestSet testset = new TestSet();
			testset.addTest(testcase.getClazz(), testcase.getMethod());
			tester.evaluateOnce(testset, endversion, i, logFolder);
			tester.evaluateOnce(testset, endversion + "~1", i, logFolder);
		}
	}
}
