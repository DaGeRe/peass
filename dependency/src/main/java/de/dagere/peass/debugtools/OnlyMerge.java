package de.dagere.peass.debugtools;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.TestSelectionConfigMixin;
import de.dagere.peass.dependency.parallel.PartialDependenciesMerger;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.vcs.CommitUtil;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;

public class OnlyMerge implements Callable<Void>{
   
   @Mixin
   private TestSelectionConfigMixin config;
   
   @Mixin
   private ExecutionConfigMixin executionConfigMixin;
   
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
      final List<String> commits = CommitUtil.getGitCommits(executionConfigMixin.getStartcommit(), executionConfigMixin.getEndcommit(), projectFolder);
      
      final File[] files = config.getResultBaseFolder().listFiles((FilenameFilter) new WildcardFileFilter("deps*.json"));
      CommitComparatorInstance instance = new CommitComparatorInstance(commits);
      PartialDependenciesMerger.mergeVersions(new File(config.getResultBaseFolder(), "merged.json"), files, instance);
      return null;
   }
}
