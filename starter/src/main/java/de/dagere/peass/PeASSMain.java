package de.dagere.peass;

import java.util.concurrent.Callable;

import de.dagere.peass.measurement.cleaning.CleaningStarter;
import de.dagere.peass.measurement.utils.CreateMeasurementExecutionScript;
import de.dagere.peass.reexecutions.FindMissingExecutions;
import de.dagere.peass.visualization.VisualizeRCA;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "peass", mixinStandardHelpOptions = true, subcommands = { DependencyExecutionReader.class, 
      DependencyTestStarter.class,
      GetChanges.class, 
      CleaningStarter.class, 
      IsChange.class, 
      RootCauseAnalysis.class, 
      CreateMeasurementExecutionScript.class, 
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
