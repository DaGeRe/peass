package de.dagere.peass;

import java.util.concurrent.Callable;

import de.dagere.peass.utils.DivideVersions;
import de.peass.ContinuousExecutionStarter;
import de.peass.GetChanges;
import de.peass.ReadProperties;
import de.peass.clean.TestCleaner;
import de.peass.reexecutions.FindMissingExecutions;
import de.peass.visualization.VisualizeRCA;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "peass", mixinStandardHelpOptions = true, subcommands = { DependencyExecutionReader.class, 
      DependencyTestStarter.class,
      GetChanges.class, 
      ReadProperties.class, 
      TestCleaner.class, 
      IsChange.class, 
      RootCauseAnalysis.class, 
      DivideVersions.class, 
      VisualizeRCA.class, 
      ContinuousExecutionStarter.class,
      FindMissingExecutions.class}, synopsisSubcommandLabel = "COMMAND")
public class PeASSMain implements Callable<Void> {
   public static void main(final String[] args) {
      final CommandLine line = new CommandLine(new PeASSMain());
      if (args.length != 0) {
         System.exit(line.execute(args));
      } else {
         line.usage(System.out);
      }

   }

   @Override
   public Void call() throws Exception {
      return null;
   }
}
