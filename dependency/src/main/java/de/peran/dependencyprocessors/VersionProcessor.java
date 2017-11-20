package de.peran.dependencyprocessors;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.peran.DependencyStatisticAnalyzer;
import de.peran.dependency.PeASSFolderUtil;
import de.peran.generated.Versiondependencies;
import de.peran.generated.Versiondependencies.Initialversion;
import de.peran.generated.Versiondependencies.Versions.Version;
import de.peran.utils.OptionConstants;
import de.peran.vcs.GitUtils;
import de.peran.vcs.VersionControlSystem;

/**
 * Basic class for all classes that operate somehow on an folder and it's dependencyfile.
 * 
 * @author reichelt
 *
 */
public abstract class VersionProcessor {

	protected File projectFolder;
	protected VersionControlSystem vcs;
	protected final Versiondependencies dependencies;
	protected final CommandLine line;
	protected final String startversion;
	protected final String endversion;

	public VersionProcessor(final String[] args) throws ParseException, JAXBException {
		this(args, true);
	}

	public VersionProcessor(final String[] args, final boolean isProjectFolder) throws ParseException, JAXBException {
		final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.DEPENDENCYFILE, OptionConstants.WARMUP, OptionConstants.ITERATIONS, OptionConstants.VMS,
				OptionConstants.STARTVERSION, OptionConstants.ENDVERSION,
				OptionConstants.EXECUTIONFILE, OptionConstants.REPETITIONS, OptionConstants.DURATION, 
				OptionConstants.CHANGEFILE,	OptionConstants.TEST);
		final CommandLineParser parser = new DefaultParser();

		line = parser.parse(options, args);

		final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
		dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);

		projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
		if (!projectFolder.exists()) {
			GitUtils.downloadProject(dependencies.getUrl(), projectFolder);
		}
		PeASSFolderUtil.setProjectFolder(projectFolder);

		startversion = line.getOptionValue(OptionConstants.STARTVERSION.getName(), null);
		endversion = line.getOptionValue(OptionConstants.ENDVERSION.getName(), null);

		VersionComparator.setDependencies(dependencies);
		if (isProjectFolder) {
			vcs = VersionControlSystem.getVersionControlSystem(projectFolder);
		} else {
			vcs = null;
		}

		VersionComparator.setDependencies(dependencies);
	}

	public void processCommandline() throws ParseException, JAXBException {
		processInitialVersion(dependencies.getInitialversion());

		for (final Version version : dependencies.getVersions().getVersion()) {
			processVersion(version);
		}
	}

	protected void processInitialVersion(final Initialversion version) {

	}

	protected abstract void processVersion(Version version);

	protected CommandLine getLine() {
		return line;
	}
}
