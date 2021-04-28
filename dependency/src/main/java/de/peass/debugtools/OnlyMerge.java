package de.peass.debugtools;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import de.dagere.peass.CommitUtil;
import de.peass.config.DependencyReaderConfigMixin;
import de.peass.dependency.parallel.PartialDependenciesMerger;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.vcs.GitCommit;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;

public class OnlyMerge implements Callable<Void>{
   
   @Mixin
   private DependencyReaderConfigMixin config;
   
   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new OnlyMerge());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }  
   
   @Override
   public Void call() throws Exception {
      final File projectFolder = config.getProjectFolder();
      final List<GitCommit> commits = CommitUtil.getGitCommits(config.getStartversion(), config.getEndversion(), projectFolder);
      VersionComparator.setVersions(commits);
      
      final File[] files = config.getResultBaseFolder().listFiles((FilenameFilter) new WildcardFileFilter("deps*.json"));
      
      PartialDependenciesMerger.mergeVersions(new File(config.getResultBaseFolder(), "merged.json"), files);
      return null;
   }
}
