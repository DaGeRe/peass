package de.dagere.peass;

import java.util.concurrent.Callable;

import de.dagere.peass.measurement.cleaning.CleanStarter;
import de.dagere.peass.measurement.utils.CreateScriptStarter;
import de.dagere.peass.reexecutions.FindMissingExecutionStarter;
import de.dagere.peass.visualization.VisualizeRCAStarter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "peass", mixinStandardHelpOptions = true, subcommands = { SelectStarter.class, 
      MeasureStarter.class,
      GetChangesStarter.class, 
      CleanStarter.class, 
      IsChangeStarter.class, 
      SearchCauseStarter.class, 
      CreateScriptStarter.class, 
      VisualizeRCAStarter.class, 
      ContinuousExecutionStarter.class,
      FindMissingExecutionStarter.class}, synopsisSubcommandLabel = "COMMAND")
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
