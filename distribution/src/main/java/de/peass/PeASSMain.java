package de.peass;

import java.util.concurrent.Callable;

import de.peass.clean.TestCleaner;
import de.peass.utils.DivideVersions;
import de.peass.visualization.GenerateRCAHTML;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "peass", mixinStandardHelpOptions = true, subcommands = { DependencyExecutionReader.class, 
      GetChanges.class, 
      ReadProperties.class, 
      TestCleaner.class, 
      IsChange.class, 
      RootCauseAnalysis.class, 
      DivideVersions.class, 
      GenerateRCAHTML.class }, synopsisSubcommandLabel = "COMMAND")
public class PeASSMain implements Callable<Void> {
   public static void main(final String[] args) {
      final CommandLine line = new CommandLine(new PeASSMain());
      if (args.length != 0) {
         line.execute(args);
      } else {
         line.usage(System.out);
      }

   }

   @Override
   public Void call() throws Exception {
      return null;
   }
}
