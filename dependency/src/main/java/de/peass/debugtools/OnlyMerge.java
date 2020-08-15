package de.peass.debugtools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.DependencyReadingStarter;
import de.peass.dependency.parallel.Merger;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.OptionConstants;
import de.peass.vcs.GitCommit;

public class OnlyMerge {
   public static void main(final String[] args) throws JsonGenerationException, JsonMappingException, IOException, ParseException {
      final Options options = OptionConstants.createOptions(OptionConstants.FOLDER, OptionConstants.STARTVERSION, OptionConstants.ENDVERSION, OptionConstants.OUT,
            OptionConstants.DEPENDENCYFILE, OptionConstants.TIMEOUT);
      
      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);
      
      final File projectFolder = new File(line.getOptionValue(OptionConstants.FOLDER.getName()));
      final List<GitCommit> commits = DependencyReadingStarter.getGitCommits(line, projectFolder);
      VersionComparator.setVersions(commits);
      
      final File merge = new File("/home/reichelt/daten3/diss/chunk2/commons-pool");
      final File[] files = merge.listFiles((FilenameFilter) new WildcardFileFilter("*.json"));
      
      Merger.mergeVersions(new File(merge, "merged.json"), files);
   }
}
