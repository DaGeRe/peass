package de.dagere.peass;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.CommitUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(description = "Get the traces of the project", name = "getTraces")
public class GetTraceStarter implements Callable<Void> {
   
   @Option(names = { "-project", "--project" }, description = "Project folder path")
   File projectFolder;
   
   @Mixin
   ExecutionConfigMixin executionConfigMixin;
   
   public static void main(String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new GetTraceStarter());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }
   
   @Override
   public Void call() throws Exception {

      final List<String> commits = CommitUtil.getGitCommits(executionConfigMixin.getStartcommit(), executionConfigMixin.getEndcommit(), projectFolder,
            executionConfigMixin.isLinearizeHistory());
      VersionComparator.setVersions(commits);

      CommitComparatorInstance comparator = new CommitComparatorInstance(commits);
      
      PeassFolders folders = new PeassFolders(projectFolder);
      final KiekerResultManager tracereader = new KiekerResultManager(folders, new ExecutionConfig(5), new KiekerConfig(true), new EnvironmentVariables());
      final TestSet testset = new TestSet();
      
      //TODO Get tests
      testset.addTest(new TestMethodCall("viewtest.TestMe", "test", ""));
      tracereader.getExecutor().loadClasses();
      tracereader.executeKoPeMeKiekerRun(testset, "1", folders.getDependencyLogFolder());
      return null;
   }
}
